-- V1__create_auth_tables.sql
-- Gateway authentication schema.
-- Flyway runs this automatically on startup if the table does not exist.

CREATE TABLE IF NOT EXISTS gateway_users
(
    id            UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    phone_number  VARCHAR(20)  NOT NULL,
    is_active     BOOLEAN      NOT NULL    DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL    DEFAULT NOW(),
    last_login_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_gateway_users_email ON gateway_users (email);
CREATE INDEX IF NOT EXISTS idx_gateway_users_role ON gateway_users (role);

CREATE TABLE IF NOT EXISTS refresh_tokens
(
    id          UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES gateway_users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL    DEFAULT NOW(),
    revoked     BOOLEAN     NOT NULL    DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- Seed a default courier and customer for local development testing.
-- Password for both is: LocalDev123!
-- BCrypt hash generated with strength 12.
INSERT INTO gateway_users (email, password_hash, role, first_name, last_name, phone_number)
VALUES ('courier@deli.local',
        '$2b$12$cEsMjz0GZEtXQYDzWmfi3O/qYA9LFNRhJfxhnLcVMEyMZ0g2Kd4YC',
        'COURIER',
        'Marco',
        'Bianchi',
        '+4915123456789')
ON CONFLICT (email) DO NOTHING;

INSERT INTO gateway_users (email, password_hash, role, first_name, last_name, phone_number)
VALUES ('customer@deli.local',
        '$2b$12$cEsMjz0GZEtXQYDzWmfi3O/qYA9LFNRhJfxhnLcVMEyMZ0g2Kd4YC',
        'CUSTOMER',
        'Anna',
        'Müller',
        '+4915987654321')
ON CONFLICT (email) DO NOTHING;
