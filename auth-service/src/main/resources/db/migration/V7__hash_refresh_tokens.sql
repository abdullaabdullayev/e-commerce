-- Refresh tokens are now stored as SHA-256 digests. Older rows held raw token values
-- (and no endpoint ever accepted them), so remove them instead of keeping plaintext secrets.
DELETE FROM refresh_tokens;
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_account ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expiry ON refresh_tokens(expiry_date);
