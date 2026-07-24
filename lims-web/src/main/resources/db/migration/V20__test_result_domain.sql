-- Issue #79: Structured test results and specification judgment domain.
CREATE TABLE IF NOT EXISTS test_result (
    id VARCHAR(64) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    analysis_task_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    item_id VARCHAR(64),
    test_method VARCHAR(200),
    equipment_id VARCHAR(64),
    raw_value VARCHAR(200),
    entered_value NUMERIC(20,6),
    unit VARCHAR(50),
    spec_lower NUMERIC(20,6),
    spec_upper NUMERIC(20,6),
    judgment VARCHAR(20) DEFAULT 'PENDING',
    uncertainty NUMERIC(20,6),
    repeat_count INTEGER DEFAULT 0,
    result_attachment_url VARCHAR(500),
    remark TEXT,
    entered_by VARCHAR(64) NOT NULL,
    entered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reviewed_by VARCHAR(64),
    reviewed_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ENTERED',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_test_result_task ON test_result(analysis_task_id);
CREATE INDEX IF NOT EXISTS idx_test_result_request ON test_result(request_id);
CREATE INDEX IF NOT EXISTS idx_test_result_status ON test_result(status);

ALTER TABLE test_result ADD CONSTRAINT IF NOT EXISTS chk_test_result_judgment
    CHECK (judgment IN ('PASS', 'FAIL', 'CONDITIONAL', 'PENDING'));
ALTER TABLE test_result ADD CONSTRAINT IF NOT EXISTS chk_test_result_status
    CHECK (status IN ('ENTERED', 'REVIEWED', 'REJECTED', 'INVALIDATED'));
