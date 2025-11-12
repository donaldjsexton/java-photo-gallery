CREATE TABLE IF NOT EXISTS photos (
  id BIGSERIAL PRIMARY KEY,
  original_name      VARCHAR(255) NOT NULL,
  file_name          VARCHAR(255) NOT NULL UNIQUE,
  content_type       VARCHAR(100),
  size               BIGINT NOT NULL,
  file_hash          VARCHAR(128),
  upload_date        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  searchable_text    TEXT,
  camera_info        TEXT,
  date_taken_parsed  TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_photos_upload_date ON photos(upload_date DESC);
CREATE INDEX IF NOT EXISTS idx_photos_searchable_text ON photos ((lower(searchable_text)));
CREATE INDEX IF NOT EXISTS idx_photos_date_taken ON photos(date_taken_parsed);
