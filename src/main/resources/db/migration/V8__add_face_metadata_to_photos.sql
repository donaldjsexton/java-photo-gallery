-- V8__add_face_metadata_to_photos.sql
-- Optional face metadata hook for future OpenCV usage

ALTER TABLE photos
    ADD COLUMN IF NOT EXISTS face_count INT,
    ADD COLUMN IF NOT EXISTS face_metadata JSONB;
