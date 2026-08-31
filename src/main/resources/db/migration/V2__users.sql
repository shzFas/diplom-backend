-- Одна сущность пользователя вместо разделённых User и Student.
--
-- В версии 2023 года это были две коллекции с почти одинаковыми полями,
-- из-за чего login, register, getMe и changePassword были написаны дважды,
-- и дважды же был захардкожен секрет JWT. Роль различает их лучше, чем
-- отдельная таблица: у всех трёх ролей одинаковый цикл аутентификации.

CREATE TABLE users (
    id            bigserial PRIMARY KEY,
    full_name     text        NOT NULL CHECK (length(btrim(full_name)) > 0),
    email         citext      NOT NULL UNIQUE,
    password_hash text        NOT NULL,
    role          user_role   NOT NULL,
    avatar_url    text,

    -- Пишется только сервером после подтверждения владения чатом.
    -- Клиент не может назначить себе чужой chat_id: в версии 2023 года
    -- это был открытый эндпоинт, принимавший username из URL.
    telegram_chat_id bigint UNIQUE,

    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    deactivated_at timestamptz
);

COMMENT ON COLUMN users.deactivated_at IS
    'Мягкое удаление: уволенный учитель остаётся автором выставленных оценок.';

-- Список активных пользователей роли — самый частый запрос админки.
CREATE INDEX users_role_active_idx ON users (role) WHERE deactivated_at IS NULL;


-- Refresh-токены хранятся, чтобы их можно было отозвать: при logout,
-- при смене пароля и при компрометации. Access-токен живёт 15 минут
-- и не хранится вовсе.
CREATE TABLE refresh_tokens (
    id         bigserial PRIMARY KEY,
    user_id    bigint      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash text        NOT NULL UNIQUE,
    issued_at  timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    CHECK (expires_at > issued_at)
);

COMMENT ON COLUMN refresh_tokens.token_hash IS
    'SHA-256 от токена. Утечка таблицы не даёт войти под пользователем.';

CREATE INDEX refresh_tokens_user_active_idx
    ON refresh_tokens (user_id) WHERE revoked_at IS NULL;
