-- =============================================================================
-- V3 · Seed the "system" bootstrap user
--
--  WHY:  AssetAssignment.assigned_by_user_id and returned_by_user_id are
--        NOT NULL foreign keys to user_account. Until real authentication
--        lands (Milestone 3), the service defaults these to id=1 so we
--        keep the audit trail meaningful without loosening the schema.
--
--  SAFE: enabled=FALSE and password_hash is deliberately NOT a valid
--        BCrypt string, so nobody can log in as this user in M3+.
--
--  Idempotent: uses ON CONFLICT DO NOTHING and resets the sequence past
--  the reserved id so real inserts continue at id=2+.
-- =============================================================================

INSERT INTO person (id, first_name, last_name, email, phone, is_active)
VALUES (1, 'System', 'Bootstrap', 'system@itams.local', NULL, TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_account (id, person_id, username, password_hash, enabled)
VALUES (1, 1, 'system', '!not-a-real-hash-do-not-login', FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_role (user_account_id, role_id)
SELECT 1, id FROM role WHERE code = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Reset sequences past the reserved id so future auto-inserts continue at id=2+
SELECT setval('person_id_seq',       GREATEST((SELECT MAX(id) FROM person),       1));
SELECT setval('user_account_id_seq', GREATEST((SELECT MAX(id) FROM user_account), 1));
