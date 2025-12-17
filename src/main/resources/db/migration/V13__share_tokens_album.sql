-- Refactor share tokens from gallery-level to album-level.

ALTER TABLE share_tokens
    ADD COLUMN IF NOT EXISTS album_id BIGINT;

-- Backfill album_id from the referenced gallery (legacy schema).
UPDATE share_tokens st
SET album_id = g.album_id
FROM galleries g
WHERE st.album_id IS NULL
  AND st.gallery_id IS NOT NULL
  AND g.id = st.gallery_id;

ALTER TABLE share_tokens
    ALTER COLUMN album_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_share_tokens_album'
    ) THEN
        ALTER TABLE share_tokens
            ADD CONSTRAINT fk_share_tokens_album
                FOREIGN KEY (album_id)
                REFERENCES albums(id)
                ON DELETE CASCADE;
    END IF;
END $$;

DROP INDEX IF EXISTS idx_share_tokens_gallery;
CREATE INDEX IF NOT EXISTS idx_share_tokens_album
    ON share_tokens(album_id);

CREATE INDEX IF NOT EXISTS idx_share_tokens_tenant_album
    ON share_tokens(tenant_id, album_id);

ALTER TABLE share_tokens
    DROP COLUMN IF EXISTS gallery_id;

