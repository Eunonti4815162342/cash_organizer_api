CREATE TABLE IF NOT EXISTS cash_organizer.password_reset_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_prt_token ON cash_organizer.password_reset_tokens (token);
CREATE INDEX IF NOT EXISTS idx_prt_email ON cash_organizer.password_reset_tokens (email);
