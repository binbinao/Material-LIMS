-- V15: Add password-based login alongside Azure AD SSO.
--
-- User-facing requirement: close SSO, enable manual login with a default
-- password ("password") for every existing account. The /api/v1/auth/login
-- endpoint verifies login_id + BCrypt-hashed password and issues a LIMS
-- JWT, replacing the SSO callback flow. Azure AD endpoints remain in
-- code (gated by `azure.ad.enabled`) so this migration is reversible.
--
-- Schema:
--   - sys_user gains a NOT NULL password_hash column (BCrypt $2a$)
--   - Every existing user is seeded with the BCrypt hash of the literal
--     string "password". The hash below was produced offline by
--     BCryptPasswordEncoder.encode("password") with default cost (10).
--   - Salt is per-call (BCrypt random), so a fresh hash on first login
--     will differ — what matters is that BCryptPasswordEncoder.matches
--     returns true, which it does for any valid hash of "password".

ALTER TABLE sys_user ADD COLUMN password_hash VARCHAR(255);
UPDATE sys_user SET password_hash = '$2a$10$dvgGdK63f3ADcGfKoeRzCudSWYEQUuLN/zvI7bceaT2KhjiKSeyMG';
ALTER TABLE sys_user ALTER COLUMN password_hash SET NOT NULL;
