-- =============================================================================
--  V4 · Refresh-token families (reuse detection)
--
--  Every login starts a new "family"; every rotation of a refresh token keeps
--  the same family_id. If a token that was already ROTATED is presented again,
--  it has most likely been stolen (either the attacker or the real user is
--  replaying an old token), so AuthService revokes every still-active token in
--  that family. Effect: that one session is logged out everywhere it was copied
--  to; the user's other sessions (other devices = other families) survive.
--
--  Pattern: OAuth 2.0 Security BCP (RFC 9700) §4.14.2, "refresh token rotation".
-- =============================================================================

ALTER TABLE refresh_token ADD COLUMN family_id UUID;

-- Existing rows: give each its own family (we can't reconstruct old chains).
UPDATE refresh_token SET family_id = gen_random_uuid() WHERE family_id IS NULL;

ALTER TABLE refresh_token ALTER COLUMN family_id SET NOT NULL;

-- Only active tokens are ever revoked by family, so a partial index is enough.
CREATE INDEX ix_refresh_token_family_active
    ON refresh_token (family_id)
    WHERE revoked_at IS NULL;
