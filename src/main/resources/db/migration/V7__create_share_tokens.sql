-- V7__create_share_tokens.sql
-- Share tokens for public / password-protected links

CREATE TABLE IF NOT EXISTS share_tokens (
    id            UUID PRIMARY KEY,
    gallery_id    BIGINT NOT NULL REFERENCES galleries(id) ON DELETE CASCADE,
    expires_at    TIMESTAMPTZ,
    password_hash VARCHAR(255),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_share_tokens_gallery
    ON share_tokens(gallery_id);

CREATE INDEX IF NOT EXISTS idx_share_tokens_expires_at
    ON share_tokens(expires_at);
