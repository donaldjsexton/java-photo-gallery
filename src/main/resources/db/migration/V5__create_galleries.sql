-- V5__create_galleries.sql
-- Hierarchical galleries

CREATE TABLE IF NOT EXISTS galleries (
    id              BIGSERIAL PRIMARY KEY,
    parent_id       BIGINT REFERENCES galleries(id) ON DELETE SET NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    visibility      VARCHAR(50) NOT NULL DEFAULT 'private',
    password_hash   VARCHAR(255),
    cover_photo_id  BIGINT REFERENCES photos(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Basic indexes
CREATE INDEX IF NOT EXISTS idx_galleries_parent
    ON galleries(parent_id);

CREATE INDEX IF NOT EXISTS idx_galleries_visibility
    ON galleries(visibility);
