-- V3: add report reject audit fields (issue #64 / P7)
--
-- Before: approveReport() wrote approved_by + approved_at, but
-- rejectReport() only flipped status — no audit trail of who rejected.
-- After: rejected_by + rejected_at mirror the approve pattern.

ALTER TABLE report
    ADD COLUMN IF NOT EXISTS rejected_by VARCHAR(36),
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP;

COMMENT ON COLUMN report.rejected_by IS 'User id who rejected the report (mirrors approved_by)';
COMMENT ON COLUMN report.rejected_at  IS 'When the rejection happened (mirrors approved_at)';
