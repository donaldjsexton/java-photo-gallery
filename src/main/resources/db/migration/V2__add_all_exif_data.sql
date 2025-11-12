ALTER TABLE photos
  ADD COLUMN IF NOT EXISTS original_name      VARCHAR(255),
  ADD COLUMN IF NOT EXISTS file_name          VARCHAR(255),
  ADD COLUMN IF NOT EXISTS content_type       VARCHAR(100),
  ADD COLUMN IF NOT EXISTS size               BIGINT,
  ADD COLUMN IF NOT EXISTS file_hash          VARCHAR(128),
  ADD COLUMN IF NOT EXISTS upload_date        TIMESTAMPTZ,

  -- EXIF fields (strings in entity → TEXT)
  ADD COLUMN IF NOT EXISTS camera             TEXT,
  ADD COLUMN IF NOT EXISTS date_taken         TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS gps_latitude       TEXT,
  ADD COLUMN IF NOT EXISTS gps_longitude      TEXT,
  ADD COLUMN IF NOT EXISTS orientation        TEXT,
  ADD COLUMN IF NOT EXISTS focal_length       TEXT,
  ADD COLUMN IF NOT EXISTS aperture           TEXT,
  ADD COLUMN IF NOT EXISTS shutter_speed      TEXT,
  ADD COLUMN IF NOT EXISTS iso                TEXT,
  ADD COLUMN IF NOT EXISTS image_height       TEXT,
  ADD COLUMN IF NOT EXISTS image_width        TEXT,

  ADD COLUMN IF NOT EXISTS all_exif_data      TEXT,

  -- @Temporal(DATE) → DATE
  ADD COLUMN IF NOT EXISTS date_taken_parsed  DATE,

  ADD COLUMN IF NOT EXISTS searchable_text    VARCHAR(500),
  ADD COLUMN IF NOT EXISTS location_text      VARCHAR(100),
  ADD COLUMN IF NOT EXISTS camera_info        VARCHAR(100);

-- Reasonable defaults where null today would hurt queries:
UPDATE photos SET upload_date = NOW() WHERE upload_date IS NULL;
