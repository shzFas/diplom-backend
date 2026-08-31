-- Учебный календарь.
--
-- В версии 2023 года четверть была строковым полем ktpPeriod, продублированным
-- в каждом уроке и в каждой оценке. Из-за этого нельзя было ни проверить, что
-- дата урока попадает в свою четверть, ни закрыть четверть, ни отчитаться по
-- учебному году. Здесь четверть — сущность с датами и признаком закрытия.

CREATE TABLE academic_years (
    id        smallserial PRIMARY KEY,
    name      text NOT NULL UNIQUE,          -- '2025–2026'
    starts_on date NOT NULL,
    ends_on   date NOT NULL,
    CHECK (ends_on > starts_on)
);

CREATE TABLE terms (
    id               smallserial PRIMARY KEY,
    academic_year_id smallint NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    ordinal          smallint NOT NULL CHECK (ordinal BETWEEN 1 AND 4),
    starts_on        date NOT NULL,
    ends_on          date NOT NULL,

    -- Заморозка итоговых оценок. Пока NULL — итоговая считается на лету;
    -- после закрытия она зафиксирована в term_grades вместе с версией формулы,
    -- и последующая правка весов не меняет прошлое молча.
    closed_at        timestamptz,

    CHECK (ends_on > starts_on),
    UNIQUE (academic_year_id, ordinal),

    -- Четверти внутри года не пересекаются. Проверкой в коде это гарантировать
    -- нельзя — два параллельных запроса пройдут её оба.
    EXCLUDE USING gist (
        academic_year_id WITH =,
        (daterange(starts_on, ends_on, '[]')) WITH &&
    )
);

COMMENT ON COLUMN terms.closed_at IS
    'Закрытая четверть не принимает новые оценки и не пересчитывает итоговые.';

CREATE INDEX terms_year_idx ON terms (academic_year_id, ordinal);
