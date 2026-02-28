INSERT INTO venues (city_id, name)
SELECT c.id, 'Bean Street Cafe'
FROM cities c
WHERE c.name = 'Pune'
  AND NOT EXISTS (
    SELECT 1 FROM venues v WHERE v.city_id = c.id AND v.name = 'Bean Street Cafe'
  );

INSERT INTO venues (city_id, name)
SELECT c.id, 'Roast & Ritual'
FROM cities c
WHERE c.name = 'Pune'
  AND NOT EXISTS (
    SELECT 1 FROM venues v WHERE v.city_id = c.id AND v.name = 'Roast & Ritual'
  );

INSERT INTO venues (city_id, name)
SELECT c.id, 'Lakeside Coffee House'
FROM cities c
WHERE c.name = 'Pune'
  AND NOT EXISTS (
    SELECT 1 FROM venues v WHERE v.city_id = c.id AND v.name = 'Lakeside Coffee House'
  );

INSERT INTO events (type, title, poster_url)
SELECT 'CAFE', 'Coffee Catch-up', NULL
WHERE NOT EXISTS (SELECT 1 FROM events e WHERE e.type = 'CAFE' AND e.title = 'Coffee Catch-up');

INSERT INTO events (type, title, poster_url)
SELECT 'CAFE', 'Brunch Meetup', NULL
WHERE NOT EXISTS (SELECT 1 FROM events e WHERE e.type = 'CAFE' AND e.title = 'Brunch Meetup');

INSERT INTO events (type, title, poster_url)
SELECT 'CAFE', 'Evening Chai Plan', NULL
WHERE NOT EXISTS (SELECT 1 FROM events e WHERE e.type = 'CAFE' AND e.title = 'Evening Chai Plan');

INSERT INTO showtimes (event_id, venue_id, starts_at, format)
SELECT e.id, v.id, UTC_DATE() + INTERVAL 18 HOUR, 'GENERAL'
FROM events e
JOIN venues v ON v.name = 'Bean Street Cafe'
JOIN cities c ON c.id = v.city_id AND c.name = 'Pune'
WHERE e.type = 'CAFE' AND e.title = 'Coffee Catch-up'
  AND NOT EXISTS (
    SELECT 1 FROM showtimes s
    WHERE s.event_id = e.id AND s.venue_id = v.id AND s.starts_at = UTC_DATE() + INTERVAL 18 HOUR
  );

INSERT INTO showtimes (event_id, venue_id, starts_at, format)
SELECT e.id, v.id, UTC_DATE() + INTERVAL 1 DAY + INTERVAL 11 HOUR, 'GENERAL'
FROM events e
JOIN venues v ON v.name = 'Roast & Ritual'
JOIN cities c ON c.id = v.city_id AND c.name = 'Pune'
WHERE e.type = 'CAFE' AND e.title = 'Brunch Meetup'
  AND NOT EXISTS (
    SELECT 1 FROM showtimes s
    WHERE s.event_id = e.id AND s.venue_id = v.id AND s.starts_at = UTC_DATE() + INTERVAL 1 DAY + INTERVAL 11 HOUR
  );

INSERT INTO showtimes (event_id, venue_id, starts_at, format)
SELECT e.id, v.id, UTC_DATE() + INTERVAL 1 DAY + INTERVAL 18 HOUR, 'GENERAL'
FROM events e
JOIN venues v ON v.name = 'Lakeside Coffee House'
JOIN cities c ON c.id = v.city_id AND c.name = 'Pune'
WHERE e.type = 'CAFE' AND e.title = 'Evening Chai Plan'
  AND NOT EXISTS (
    SELECT 1 FROM showtimes s
    WHERE s.event_id = e.id AND s.venue_id = v.id AND s.starts_at = UTC_DATE() + INTERVAL 1 DAY + INTERVAL 18 HOUR
  );
