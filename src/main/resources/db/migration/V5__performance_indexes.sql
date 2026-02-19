CREATE INDEX idx_city_name_lookup ON cities (name);
CREATE INDEX idx_venue_city_lookup ON venues (city_id);
CREATE INDEX idx_event_type_lookup ON events (type);
CREATE INDEX idx_showtime_event_lookup ON showtimes (event_id);
CREATE INDEX idx_showtime_venue_lookup ON showtimes (venue_id);
CREATE INDEX idx_showtime_event_venue_lookup ON showtimes (event_id, venue_id);
