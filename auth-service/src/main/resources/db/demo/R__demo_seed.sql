-- Demo data: loaded only with the "demo" Spring profile (db/demo location). Idempotent.
-- Password for all demo accounts: Demo123!
-- Fixed IDs keep cross-service demo references (user, course and order seeds) consistent.
INSERT INTO accounts (id, email, username, password_hash, active) VALUES
 (1, 'admin@demo.local',   'demo_admin',   '$2a$10$aR.qm1ymJ50.6HsHZ/drHOEG1XeUQdXgv/Qi/jrSwnWyAPXo7h0tO', true),
 (2, 'teacher@demo.local', 'demo_teacher', '$2a$10$aR.qm1ymJ50.6HsHZ/drHOEG1XeUQdXgv/Qi/jrSwnWyAPXo7h0tO', true),
 (3, 'student@demo.local', 'demo_student', '$2a$10$aR.qm1ymJ50.6HsHZ/drHOEG1XeUQdXgv/Qi/jrSwnWyAPXo7h0tO', true),
 (4, 'parent@demo.local',  'demo_parent',  '$2a$10$aR.qm1ymJ50.6HsHZ/drHOEG1XeUQdXgv/Qi/jrSwnWyAPXo7h0tO', true)
ON CONFLICT DO NOTHING;

-- Grant roles only to rows that really are the demo accounts, never to a real account
-- that happens to occupy one of the fixed IDs.
INSERT INTO account_roles (account_id, role)
SELECT a.id, r.role
FROM accounts a
JOIN (VALUES ('admin@demo.local', 'ADMIN'), ('teacher@demo.local', 'TEACHER'),
             ('student@demo.local', 'STUDENT'), ('parent@demo.local', 'PARENT')) AS r(email, role)
  ON a.email = r.email
ON CONFLICT DO NOTHING;

-- Explicit IDs do not advance the identity sequence.
SELECT setval(pg_get_serial_sequence('accounts', 'id'), (SELECT COALESCE(max(id), 1) FROM accounts));
