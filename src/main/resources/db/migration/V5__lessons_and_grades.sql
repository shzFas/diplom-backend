-- Уроки (бывший Ktp), оценки (бывший Mark) и посещаемость.

CREATE TABLE lessons (
    id            bigserial PRIMARY KEY,
    assignment_id bigint   NOT NULL REFERENCES teaching_assignments(id) ON DELETE CASCADE,
    term_id       smallint NOT NULL REFERENCES terms(id) ON DELETE RESTRICT,
    title         text     NOT NULL CHECK (length(btrim(title)) > 0),
    lesson_date   date     NOT NULL,
    kind          assessment_kind NOT NULL,
    max_score     smallint NOT NULL CHECK (max_score > 0),
    created_at    timestamptz NOT NULL DEFAULT now(),

    -- Защита от дубля урока. В версии 2023 года эта проверка загружала всю
    -- коллекцию в память Node и фильтровала её через .filter(), из-за чего
    -- два параллельных запроса оба не находили дубль и оба записывали.
    UNIQUE (assignment_id, lesson_date, title)
);

-- Правило «один СОЧ в четверти» — ограничение базы, а не проверка в коде.
-- Гонкой его обойти нельзя.
CREATE UNIQUE INDEX lessons_one_soch_per_term_idx
    ON lessons (assignment_id, term_id)
    WHERE kind = 'SOCH'::assessment_kind;

CREATE INDEX lessons_assignment_term_idx ON lessons (assignment_id, term_id);

-- Дата урока должна попадать в свою четверть. Межтабличное условие
-- нельзя выразить через CHECK, поэтому это триггер.
CREATE OR REPLACE FUNCTION lesson_date_within_term() RETURNS trigger AS $$
DECLARE
    t_start date;
    t_end   date;
BEGIN
    SELECT starts_on, ends_on INTO t_start, t_end
    FROM terms WHERE id = NEW.term_id;

    IF NEW.lesson_date < t_start OR NEW.lesson_date > t_end THEN
        RAISE EXCEPTION
            'lesson_date % is outside term % (% .. %)',
            NEW.lesson_date, NEW.term_id, t_start, t_end
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER lessons_date_within_term_trg
    BEFORE INSERT OR UPDATE OF lesson_date, term_id ON lessons
    FOR EACH ROW EXECUTE FUNCTION lesson_date_within_term();


-- Оценка. В версии 2023 года Mark дублировал семь полей из Ktp
-- (markPredmet, markClassStudent, markTeacher, markMaxValue, markSochSor,
-- markPeriod, markDate) без источника истины, и правка урока их не обновляла.
-- Здесь всё это выводится через join по lesson_id.
CREATE TABLE grades (
    id         bigserial PRIMARY KEY,
    lesson_id  bigint   NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    student_id bigint   NOT NULL REFERENCES users(id)   ON DELETE RESTRICT,
    score      smallint NOT NULL CHECK (score >= 0),
    graded_by  bigint   NOT NULL REFERENCES users(id)   ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    -- Одна оценка на ученика за урок. В прежней схеме дубли ничем не запрещались.
    UNIQUE (lesson_id, student_id)
);

CREATE INDEX grades_student_idx ON grades (student_id);

-- Верхняя граница балла живёт на уроке, поэтому проверка межтабличная.
CREATE OR REPLACE FUNCTION grade_within_lesson_max() RETURNS trigger AS $$
DECLARE
    lesson_max smallint;
BEGIN
    SELECT max_score INTO lesson_max FROM lessons WHERE id = NEW.lesson_id;

    IF NEW.score > lesson_max THEN
        RAISE EXCEPTION
            'score % exceeds max_score % of lesson %',
            NEW.score, lesson_max, NEW.lesson_id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER grades_within_lesson_max_trg
    BEFORE INSERT OR UPDATE OF score, lesson_id ON grades
    FOR EACH ROW EXECUTE FUNCTION grade_within_lesson_max();


-- Посещаемость отдельно от оценок. Ученик может отсутствовать и не иметь
-- оценки, отсутствовать и получить балл за отработку, либо присутствовать
-- без оценки — булев markFalse не выражал ни одного из этих случаев.
CREATE TABLE attendance (
    lesson_id  bigint NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    student_id bigint NOT NULL REFERENCES users(id)   ON DELETE RESTRICT,
    status     attendance_status NOT NULL,
    noted_by   bigint NOT NULL REFERENCES users(id)   ON DELETE RESTRICT,
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (lesson_id, student_id)
);

CREATE INDEX attendance_student_idx ON attendance (student_id);
