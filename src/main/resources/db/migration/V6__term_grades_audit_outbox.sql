-- Замороженные итоговые, аудит правок и очередь уведомлений.

-- Итоговая за четверть не хранится, пока четверть открыта: она считается
-- чистой функцией доменного слоя. При закрытии четверти результат фиксируется
-- здесь вместе с версией формулы — иначе позднейшая правка весов СОР/СОЧ
-- молча изменила бы уже выставленные годовые оценки.
CREATE TABLE term_grades (
    assignment_id   bigint   NOT NULL REFERENCES teaching_assignments(id) ON DELETE CASCADE,
    student_id      bigint   NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    term_id         smallint NOT NULL REFERENCES terms(id) ON DELETE RESTRICT,
    percentage      numeric(5,2) NOT NULL CHECK (percentage BETWEEN 0 AND 100),
    final_score     smallint NOT NULL CHECK (final_score BETWEEN 1 AND 5),
    formula_version text     NOT NULL,
    frozen_at       timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (assignment_id, student_id, term_id)
);

COMMENT ON COLUMN term_grades.formula_version IS
    'Версия правил расчёта, действовавших на момент закрытия четверти.
     Веса СОР и СОЧ и пороги перевода в пятибалльную шкалу задаются
     нормативно и менялись, поэтому они конфигурация, а не константа в коде.';


-- Правка оценки — юридически значимое событие. В версии 2023 года
-- deleteOne удалял запись бесследно.
CREATE TABLE grade_audit (
    id         bigserial PRIMARY KEY,
    grade_id   bigint       NOT NULL,   -- намеренно без FK: запись переживает удаление оценки
    lesson_id  bigint       NOT NULL,
    student_id bigint       NOT NULL,
    actor_id   bigint       NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    action     grade_action NOT NULL,
    old_score  smallint,
    new_score  smallint,
    reason     text,
    at         timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX grade_audit_grade_idx   ON grade_audit (grade_id, at DESC);
CREATE INDEX grade_audit_student_idx ON grade_audit (student_id, at DESC);


-- Уведомления отправляются воркером, а не внутри HTTP-запроса.
-- В версии 2023 года это был axios.post без await прямо в контроллере:
-- ошибка отправки терялась молча, а время ответа Telegram попадало
-- в время ответа API.
CREATE TABLE outbox (
    id           bigserial PRIMARY KEY,
    kind         text          NOT NULL,
    payload      jsonb         NOT NULL,
    status       outbox_status NOT NULL DEFAULT 'PENDING',
    attempts     smallint      NOT NULL DEFAULT 0,
    last_error   text,
    available_at timestamptz   NOT NULL DEFAULT now(),
    created_at   timestamptz   NOT NULL DEFAULT now()
);

-- Выборка воркера: что готово к отправке прямо сейчас.
CREATE INDEX outbox_pending_idx
    ON outbox (available_at)
    WHERE status = 'PENDING'::outbox_status;
