INSERT INTO venues (city_id, name)
SELECT c.id, 'Sunrise Multiplex' FROM cities c
WHERE c.name = 'Pune'
  AND NOT EXISTS (SELECT 1 FROM venues v WHERE v.city_id = c.id AND v.name = 'Sunrise Multiplex');

INSERT INTO venues (city_id, name)
SELECT c.id, 'Galaxy Screens' FROM cities c
WHERE c.name = 'Pune'
  AND NOT EXISTS (SELECT 1 FROM venues v WHERE v.city_id = c.id AND v.name = 'Galaxy Screens');

INSERT INTO venues (city_id, name)
SELECT c.id, 'Riverside Cinema' FROM cities c
WHERE c.name = 'Pune'
  AND NOT EXISTS (SELECT 1 FROM venues v WHERE v.city_id = c.id AND v.name = 'Riverside Cinema');

INSERT INTO venues (city_id, name)
SELECT c.id, 'Metro Theatre' FROM cities c
WHERE c.name = 'Pune'
  AND NOT EXISTS (SELECT 1 FROM venues v WHERE v.city_id = c.id AND v.name = 'Metro Theatre');

INSERT INTO venues (city_id, name)
SELECT c.id, 'Skyline IMAX' FROM cities c
WHERE c.name = 'Pune'
  AND NOT EXISTS (SELECT 1 FROM venues v WHERE v.city_id = c.id AND v.name = 'Skyline IMAX');
