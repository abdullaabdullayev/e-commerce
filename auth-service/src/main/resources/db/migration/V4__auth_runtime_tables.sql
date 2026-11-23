-- Earlier installations created these two tables through Hibernate schema update.
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMPTZ NOT NULL,
    user_id BIGINT NOT NULL REFERENCES accounts(id)
);
CREATE TABLE IF NOT EXISTS auth_outbox (
    id UUID PRIMARY KEY,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_auth_outbox_pending ON auth_outbox(published_at, next_attempt_at, created_at);
