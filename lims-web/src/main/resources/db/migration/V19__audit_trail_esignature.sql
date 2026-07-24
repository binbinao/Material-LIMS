-- Issue #82: Immutable audit trail and e-signature foundation.
-- Add before/after value capture, tamper-evident hash, and e-signature fields.

ALTER TABLE sys_operation_log ADD COLUMN IF NOT EXISTS before_value TEXT;
ALTER TABLE sys_operation_log ADD COLUMN IF NOT EXISTS after_value TEXT;
ALTER TABLE sys_operation_log ADD COLUMN IF NOT EXISTS entry_hash VARCHAR(64);
ALTER TABLE sys_operation_log ADD COLUMN IF NOT EXISTS signature_user_id VARCHAR(64);
ALTER TABLE sys_operation_log ADD COLUMN IF NOT EXISTS signature_meaning VARCHAR(200);
ALTER TABLE sys_operation_log ADD COLUMN IF NOT EXISTS signed_at TIMESTAMP;

-- Add e-signature fields to report for compliance (21 CFR Part 11)
ALTER TABLE report ADD COLUMN IF NOT EXISTS signature_user_id VARCHAR(64);
ALTER TABLE report ADD COLUMN IF NOT EXISTS signature_meaning VARCHAR(200);
ALTER TABLE report ADD COLUMN IF NOT EXISTS signed_at TIMESTAMP;

-- Prevent tampering: audit log is insert-only
REVOKE UPDATE, DELETE ON sys_operation_log FROM PUBLIC;
