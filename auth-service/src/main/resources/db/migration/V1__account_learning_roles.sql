-- Adopt existing Hibernate-created accounts without replacing their IDs or passwords.
CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true
);
ALTER TABLE accounts ALTER COLUMN email DROP NOT NULL;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS username VARCHAR(40);
CREATE UNIQUE INDEX IF NOT EXISTS accounts_username_unique ON accounts(username);
CREATE TABLE account_roles (
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    role VARCHAR(30) NOT NULL CHECK (role IN ('CUSTOMER','ADMIN','TEACHER','STUDENT')),
    PRIMARY KEY (account_id, role)
);
INSERT INTO account_roles(account_id,role) SELECT id,'CUSTOMER' FROM accounts;
