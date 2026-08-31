-- Проверка, что доменные правила действительно живут в базе, а не только
-- в комментариях к миграциям. Запускается на пустой схеме после flyway migrate:
--   docker compose exec -T db psql -U bilimedu -d bilimedu -f - < schema_constraints.sql
-- Всё выполняется в транзакции и откатывается: база остаётся чистой.

\set ON_ERROR_STOP on
BEGIN;

CREATE FUNCTION assert_fails(stmt text, label text) RETURNS void AS $$
BEGIN
    BEGIN
        EXECUTE stmt;
    EXCEPTION WHEN others THEN
        RAISE NOTICE 'PASS  % -- отклонено: %', label, left(SQLERRM, 55);
        RETURN;
    END;
    RAISE EXCEPTION 'FAIL  % -- ограничение НЕ сработало', label;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION assert_ok(stmt text, label text) RETURNS void AS $$
BEGIN
    EXECUTE stmt;
    RAISE NOTICE 'PASS  %', label;
END;
$$ LANGUAGE plpgsql;

-- ---------- посев ----------

INSERT INTO academic_years (id, name, starts_on, ends_on)
VALUES (1, '2025-2026', '2025-09-01', '2026-05-25');

INSERT INTO terms (id, academic_year_id, ordinal, starts_on, ends_on) VALUES
    (1, 1, 1, '2025-09-01', '2025-10-25'),
    (2, 1, 2, '2025-11-03', '2025-12-27');

INSERT INTO users (id, full_name, email, password_hash, role) VALUES
    (1, 'Учитель Алгебры', 'teacher@school.kz',  'x', 'TEACHER'),
    (2, 'Ученик Первый',   'student1@school.kz', 'x', 'STUDENT'),
    (3, 'Ученик Второй',   'student2@school.kz', 'x', 'STUDENT');

INSERT INTO classes (id, academic_year_id, name) VALUES
    (1, 1, '8А'), (2, 1, '8Б');

INSERT INTO subjects (id, name) VALUES (1, 'Алгебра');

INSERT INTO teaching_assignments (id, class_id, subject_id, teacher_id) VALUES
    (1, 1, 1, 1),   -- 8А, Алгебра
    (2, 2, 1, 1);   -- 8Б, Алгебра

INSERT INTO enrollments (id, student_id, class_id, from_date, to_date) VALUES
    (1, 2, 1, '2025-09-01', NULL),
    (2, 3, 1, '2025-09-01', NULL);

INSERT INTO lessons (id, assignment_id, term_id, title, lesson_date, kind, max_score) VALUES
    (1, 1, 1, 'Квадратные уравнения', '2025-09-10', 'LESSON', 10),
    (2, 1, 1, 'СОР № 1',              '2025-09-24', 'SOR',    15),
    (3, 1, 1, 'СОЧ за I четверть',    '2025-10-20', 'SOCH',   20);

INSERT INTO grades (lesson_id, student_id, score, graded_by) VALUES (2, 2, 12, 1);

-- Посев использует явные id, поэтому последовательности остались на 1.
-- Без этого сброса первый же INSERT без id упрётся в свой собственный PK.
DO $$
DECLARE r record;
BEGIN
    FOR r IN
        SELECT table_name AS t
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND column_name  = 'id'
          AND column_default LIKE 'nextval%'
    LOOP
        EXECUTE format(
            'SELECT setval(pg_get_serial_sequence(''public.%I'', ''id''),
                           GREATEST((SELECT COALESCE(max(id), 0) FROM public.%I), 1))',
            r.t, r.t);
    END LOOP;
END $$;

-- ---------- регрессия на баг версии 2023 года ----------

SELECT assert_ok($$
    INSERT INTO users (full_name, email, password_hash, role)
    VALUES ('Ученик Третий', 'student3@school.kz', 'x', 'STUDENT')
$$, 'T1  второй и третий ученик без Telegram регистрируются (sparse-баг Mongo)');

-- ---------- учебный календарь ----------

SELECT assert_fails($$
    INSERT INTO terms (academic_year_id, ordinal, starts_on, ends_on)
    VALUES (1, 3, '2025-10-20', '2025-11-10')
$$, 'T2  пересекающиеся четверти внутри года');

SELECT assert_fails($$
    INSERT INTO lessons (assignment_id, term_id, title, lesson_date, kind, max_score)
    VALUES (1, 1, 'Урок не в своей четверти', '2025-12-01', 'LESSON', 10)
$$, 'T3  дата урока вне границ своей четверти');

-- ---------- правила оценивания ----------

SELECT assert_fails($$
    INSERT INTO lessons (assignment_id, term_id, title, lesson_date, kind, max_score)
    VALUES (1, 1, 'Второй СОЧ', '2025-10-22', 'SOCH', 20)
$$, 'T4  второй СОЧ в одной четверти по одному предмету');

SELECT assert_ok($$
    INSERT INTO lessons (assignment_id, term_id, title, lesson_date, kind, max_score)
    VALUES (1, 1, 'СОР № 2', '2025-10-08', 'SOR', 15)
$$, 'T5  второй СОР в четверти разрешён');

SELECT assert_ok($$
    INSERT INTO lessons (assignment_id, term_id, title, lesson_date, kind, max_score)
    VALUES (2, 1, 'СОЧ за I четверть', '2025-10-21', 'SOCH', 20)
$$, 'T6  СОЧ в той же четверти, но в другом классе');

SELECT assert_fails($$
    INSERT INTO grades (lesson_id, student_id, score, graded_by) VALUES (2, 2, 9, 1)
$$, 'T7  вторая оценка тому же ученику за тот же урок');

SELECT assert_fails($$
    INSERT INTO grades (lesson_id, student_id, score, graded_by) VALUES (2, 3, 99, 1)
$$, 'T8  балл выше max_score урока');

SELECT assert_fails($$
    INSERT INTO grades (lesson_id, student_id, score, graded_by) VALUES (2, 3, -1, 1)
$$, 'T9  отрицательный балл');

-- ---------- временна́я модель ----------

SELECT assert_fails($$
    INSERT INTO enrollments (student_id, class_id, from_date, to_date)
    VALUES (2, 2, '2025-10-01', NULL)
$$, 'T10 одновременное зачисление в два класса');

SELECT assert_ok($$
    UPDATE enrollments SET to_date = '2025-10-31' WHERE id = 1;
    INSERT INTO enrollments (student_id, class_id, from_date, to_date)
    VALUES (2, 2, '2025-11-01', NULL)
$$, 'T11 перевод в другой класс: закрыть старое, открыть новое');

-- Главная проверка: перевод не переписал историю.
-- Оценка, выставленная в сентябре, по-прежнему принадлежит 8А.
DO $$
DECLARE
    class_of_grade text;
    current_class  text;
BEGIN
    SELECT c.name INTO class_of_grade
    FROM grades g
    JOIN lessons l              ON l.id = g.lesson_id
    JOIN teaching_assignments a ON a.id = l.assignment_id
    JOIN classes c              ON c.id = a.class_id
    WHERE g.student_id = 2;

    SELECT c.name INTO current_class
    FROM enrollments e JOIN classes c ON c.id = e.class_id
    WHERE e.student_id = 2 AND e.to_date IS NULL;

    IF class_of_grade <> '8А' OR current_class <> '8Б' THEN
        RAISE EXCEPTION 'FAIL  T12 -- история испорчена: оценка в %, ученик сейчас в %',
            class_of_grade, current_class;
    END IF;
    RAISE NOTICE 'PASS  T12 сентябрьская оценка осталась за 8А, ученик числится в 8Б';
END $$;

ROLLBACK;
