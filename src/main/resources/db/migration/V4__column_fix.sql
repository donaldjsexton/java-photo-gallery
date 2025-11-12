-- Convert TIMESTAMPTZ -> DATE by truncating the time component
ALTER TABLE photos
  ALTER COLUMN date_taken_parsed TYPE DATE
  USING date_taken_parsed::date;
