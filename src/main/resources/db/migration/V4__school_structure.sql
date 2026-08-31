-- Классы, предметы, назначения учителей и зачисления учеников.
--
-- Три ошибки версии 2023 года, которые чинит этот файл:
--   1. Predmet.classes был нетипизированным массивом внутри документа предмета.
--   2. User.permission был нетипизированным массивом назначений учителя.
--      Ни то, ни другое нельзя было проверить запросом.
--   3. Student.classId был колонкой, и перевод ученика перезаписывал историю:
--      прошлогодние оценки начинали выглядеть как оценки нового класса.

-- Класс-группа существует внутри учебного года: «8А» 2025/26 и «9А» 2026/27 —
-- разные строки. Переход на следующий год не мутирует прошлое.
CREATE TABLE classes (
    id               bigserial PRIMARY KEY,
    academic_year_id smallint NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    name             text NOT NULL CHECK (length(btrim(name)) > 0),
    UNIQUE (academic_year_id, name)
);

CREATE TABLE subjects (
    id   bigserial PRIMARY KEY,
    name text NOT NULL UNIQUE CHECK (length(btrim(name)) > 0)
);


-- Заменяет и Predmet.classes[], и User.permission[].
-- «Кто какой предмет ведёт в каком классе» — центральная сущность модели:
-- на неё вешаются уроки, через неё считаются итоговые, ею же проверяются права.
-- В OneRoster эта сущность называется class (section).
CREATE TABLE teaching_assignments (
    id         bigserial PRIMARY KEY,
    class_id   bigint NOT NULL REFERENCES classes(id)  ON DELETE CASCADE,
    subject_id bigint NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    teacher_id bigint NOT NULL REFERENCES users(id)    ON DELETE RESTRICT,
    UNIQUE (class_id, subject_id)
);

COMMENT ON TABLE teaching_assignments IS
    'Один учитель на пару «класс + предмет». Смена учителя в середине года —
     это UPDATE teacher_id; авторство уже выставленных оценок сохраняется
     в grades.graded_by и не теряется.';

CREATE INDEX teaching_assignments_teacher_idx ON teaching_assignments (teacher_id);
CREATE INDEX teaching_assignments_class_idx   ON teaching_assignments (class_id);


-- Зачисление — связь с датами, а не атрибут ученика.
CREATE TABLE enrollments (
    id         bigserial PRIMARY KEY,
    student_id bigint NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    class_id   bigint NOT NULL REFERENCES classes(id) ON DELETE RESTRICT,
    from_date  date NOT NULL,
    to_date    date,                       -- NULL = учится сейчас; иначе последний день включительно

    CHECK (to_date IS NULL OR to_date >= from_date),

    -- Ученик не может одновременно числиться в двух классах.
    -- Перевод оформляется как закрытие текущей записи и открытие новой.
    EXCLUDE USING gist (
        student_id WITH =,
        (daterange(from_date, COALESCE(to_date, 'infinity'::date), '[]')) WITH &&
    )
);

COMMENT ON COLUMN enrollments.to_date IS
    'Последний день обучения в классе, включительно. NULL — зачисление активно.';

-- Текущий состав класса — самый частый запрос журнала.
CREATE INDEX enrollments_current_class_idx
    ON enrollments (class_id) WHERE to_date IS NULL;

CREATE INDEX enrollments_student_idx ON enrollments (student_id);
