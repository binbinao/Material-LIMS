-- Issue #80: Sample barcode and full lifecycle management.
-- The sample table already exists from V1 but lacks lifecycle fields.

ALTER TABLE sample ADD COLUMN IF NOT EXISTS barcode VARCHAR(100) UNIQUE;
ALTER TABLE sample ADD COLUMN IF NOT EXISTS batch_no VARCHAR(100);
ALTER TABLE sample ADD COLUMN IF NOT EXISTS container VARCHAR(100);
ALTER TABLE sample ADD COLUMN IF NOT EXISTS quantity NUMERIC(20,4);
ALTER TABLE sample ADD COLUMN IF NOT EXISTS quantity_unit VARCHAR(50);
ALTER TABLE sample ADD COLUMN IF NOT EXISTS storage_location VARCHAR(200);
ALTER TABLE sample ADD COLUMN IF NOT EXISTS custodian_id VARCHAR(64);
ALTER TABLE sample ADD COLUMN IF NOT EXISTS received_condition VARCHAR(200);
ALTER TABLE sample ADD COLUMN IF NOT EXISTS sample_status VARCHAR(30) DEFAULT 'RECEIVED';
ALTER TABLE sample ADD COLUMN IF NOT EXISTS received_at TIMESTAMP;
ALTER TABLE sample ADD COLUMN IF NOT EXISTS disposed_at TIMESTAMP;
ALTER TABLE sample ADD COLUMN IF NOT EXISTS disposal_method VARCHAR(50);
ALTER TABLE sample ADD COLUMN IF NOT EXISTS parent_sample_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_sample_barcode ON sample(barcode);
CREATE INDEX IF NOT EXISTS idx_sample_request ON sample(request_id);
CREATE INDEX IF NOT EXISTS idx_sample_status ON sample(sample_status);

ALTER TABLE sample ADD CONSTRAINT IF NOT EXISTS chk_sample_status
    CHECK (sample_status IN ('RECEIVED', 'IN_TESTING', 'SPLIT', 'STORED', 'DISPOSED', 'REJECTED'));
