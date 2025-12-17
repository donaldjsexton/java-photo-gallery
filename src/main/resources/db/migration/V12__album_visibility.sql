-- Album visibility (private/public) at the album level.

ALTER TABLE albums
    ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE';

CREATE INDEX IF NOT EXISTS idx_albums_tenant_visibility
    ON albums(tenant_id, visibility);

