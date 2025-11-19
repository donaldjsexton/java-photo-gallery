-- V6__create_gallery_photos.sql
-- Mapping photos into galleries with optional manual ordering

CREATE TABLE IF NOT EXISTS gallery_photos (
    id          BIGSERIAL PRIMARY KEY,
    gallery_id  BIGINT NOT NULL REFERENCES galleries(id) ON DELETE CASCADE,
    photo_id    BIGINT NOT NULL REFERENCES photos(id) ON DELETE CASCADE,
    sort_order  INT,
    added_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Each photo only once per gallery
ALTER TABLE gallery_photos
    ADD CONSTRAINT uq_gallery_photo UNIQUE (gallery_id, photo_id);

CREATE INDEX IF NOT EXISTS idx_gallery_photos_gallery
    ON gallery_photos(gallery_id);

CREATE INDEX IF NOT EXISTS idx_gallery_photos_photo
    ON gallery_photos(photo_id);
