-- Расширения и перечисления домена.
--
-- citext     — регистронезависимый email без ручного lower() в каждом запросе.
-- btree_gist — нужен для EXCLUDE-ограничений, где скалярное поле (=)
--              комбинируется с диапазоном дат (&&): см. terms и enrollments.

CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TYPE user_role AS ENUM ('ADMIN', 'TEACHER', 'STUDENT');

-- Тип оценочной единицы. LESSON — обычный урок без суммативной оценки,
-- SOR — суммативное оценивание за раздел, SOCH — за четверть.
CREATE TYPE assessment_kind AS ENUM ('LESSON', 'SOR', 'SOCH');

-- Посещаемость — отдельный домен от оценок: булев флаг не отличил бы
-- уважительную причину от прогула, а отчётность строится именно на этом.
CREATE TYPE attendance_status AS ENUM ('PRESENT', 'EXCUSED', 'UNEXCUSED', 'LATE');

CREATE TYPE outbox_status AS ENUM ('PENDING', 'SENT', 'FAILED');

CREATE TYPE grade_action AS ENUM ('CREATED', 'UPDATED', 'DELETED');
