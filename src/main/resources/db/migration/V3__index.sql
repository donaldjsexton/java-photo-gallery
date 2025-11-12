-- Lookups / sorts
CREATE INDEX IF NOT EXISTS idx_photos_upload_date        ON photos (upload_date DESC);
CREATE INDEX IF NOT EXISTS idx_photos_date_taken         ON photos (date_taken DESC);
CREATE INDEX IF NOT EXISTS idx_photos_date_taken_parsed  ON photos (date_taken_parsed DESC);

-- Case-insensitive filters used in JPQL LOWER(...) LIKE ...
CREATE INDEX IF NOT EXISTS idx_photos_camera_lower       ON photos (LOWER(camera));
CREATE INDEX IF NOT EXISTS idx_photos_camera_info_lower  ON photos (LOWER(camera_info));
CREATE INDEX IF NOT EXISTS idx_photos_searchable_lower   ON photos (LOWER(searchable_text));

-- Dedupe/safety
CREATE UNIQUE INDEX IF NOT EXISTS uid_photos_file_hash   ON photos (file_hash);
CREATE UNIQUE INDEX IF NOT EXISTS uid_photos_file_name   ON photos (file_name);
