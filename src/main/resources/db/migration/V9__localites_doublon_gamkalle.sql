-- « Gamkalley » est une autre graphie de « Gamkalé » (V8) : on garde la seconde.
UPDATE salons            SET locality_id = (SELECT id FROM localities WHERE name = 'Gamkalé' AND type = 'DISTRICT')
 WHERE locality_id = (SELECT id FROM localities WHERE name = 'Gamkalley' AND type = 'DISTRICT');
UPDATE application_users SET locality_id = (SELECT id FROM localities WHERE name = 'Gamkalé' AND type = 'DISTRICT')
 WHERE locality_id = (SELECT id FROM localities WHERE name = 'Gamkalley' AND type = 'DISTRICT');
DELETE FROM localities WHERE name = 'Gamkalley' AND type = 'DISTRICT';
