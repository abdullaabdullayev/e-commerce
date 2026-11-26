-- Earlier demo seeds inserted accounts with explicit IDs without advancing the sequence,
-- so new registrations collided on the primary key. Realign it with the stored rows.
SELECT setval(pg_get_serial_sequence('accounts', 'id'),
              COALESCE((SELECT max(id) FROM accounts), 1),
              (SELECT count(*) > 0 FROM accounts));
