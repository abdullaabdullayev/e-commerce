-- Earlier demo seeds inserted profiles with explicit IDs without advancing the sequence,
-- so the first registered user's profile failed on the primary key. Realign it.
SELECT setval(pg_get_serial_sequence('users', 'id'),
              COALESCE((SELECT max(id) FROM users), 1),
              (SELECT count(*) > 0 FROM users));
