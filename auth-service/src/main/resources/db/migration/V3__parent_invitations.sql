CREATE TABLE parent_invitations (
    token_hash VARCHAR(64) PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ
);
