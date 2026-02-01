ALTER TABLE cities
  ADD COLUMN postal_code VARCHAR(10) NULL,
  ADD COLUMN time_zone VARCHAR(64) NULL;

CREATE INDEX idx_city_postal_code ON cities (postal_code);

ALTER TABLE venues
  ADD COLUMN address VARCHAR(255) NULL,
  ADD COLUMN postal_code VARCHAR(10) NULL,
  ADD COLUMN source VARCHAR(20) NULL,
  ADD COLUMN source_id VARCHAR(120) NULL,
  ADD COLUMN source_url VARCHAR(500) NULL;

CREATE INDEX idx_venue_postal_code ON venues (postal_code);
CREATE INDEX idx_venue_source ON venues (source, source_id);

ALTER TABLE events
  ADD COLUMN source VARCHAR(20) NULL,
  ADD COLUMN source_id VARCHAR(120) NULL,
  ADD COLUMN source_url VARCHAR(500) NULL;

CREATE INDEX idx_event_source ON events (source, source_id);

ALTER TABLE showtimes
  ADD COLUMN source VARCHAR(20) NULL,
  ADD COLUMN source_id VARCHAR(120) NULL,
  ADD COLUMN source_url VARCHAR(500) NULL;

CREATE INDEX idx_showtime_source ON showtimes (source, source_id);
