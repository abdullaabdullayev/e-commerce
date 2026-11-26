-- Demo data: loaded only with the "demo" Spring profile (db/demo location). Idempotent.
INSERT INTO users (id, account_id, email, first_name, last_name, phone_number, guardian_name, guardian_contact) VALUES
 (1, 1, 'admin@demo.local',   'Aysel', 'Mammadova', '+994501110011', NULL, NULL),
 (2, 2, 'teacher@demo.local', 'Murad', 'Aliyev',    '+994502220022', NULL, NULL),
 (3, 3, 'student@demo.local', 'Nigar', 'Hasanova',  '+994503330033', 'Leyla Hasanova', '+994504440044'),
 (4, 4, 'parent@demo.local',  'Leyla', 'Hasanova',  '+994504440044', NULL, NULL)
ON CONFLICT DO NOTHING;

-- Link only the demo parent and demo student profiles, never real profiles on those IDs.
INSERT INTO parent_student_links (parent_account_id, student_account_id)
SELECT p.account_id, s.account_id
FROM users p, users s
WHERE p.email = 'parent@demo.local' AND s.email = 'student@demo.local'
ON CONFLICT DO NOTHING;

-- Explicit IDs do not advance the identity sequence.
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT COALESCE(max(id), 1) FROM users));
