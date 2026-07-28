-- Issue #83: Account lockout and session version for JWT invalidation.
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS session_version INTEGER NOT NULL DEFAULT 0;
