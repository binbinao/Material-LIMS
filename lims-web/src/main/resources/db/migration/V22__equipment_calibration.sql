-- Issue #81: Equipment calibration and qualification control.
CREATE TABLE IF NOT EXISTS equipment_calibration (
    id VARCHAR(64) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    equipment_id VARCHAR(64) NOT NULL,
    calibration_type VARCHAR(50) NOT NULL DEFAULT 'EXTERNAL',
    calibrated_at DATE NOT NULL,
    next_calibration_date DATE,
    certificate_no VARCHAR(200),
    certificate_url VARCHAR(500),
    calibrated_by VARCHAR(200),
    result VARCHAR(20) DEFAULT 'PASS',
    range_min NUMERIC(20,6),
    range_max NUMERIC(20,6),
    accuracy VARCHAR(100),
    remark TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_calib_equipment ON equipment_calibration(equipment_id);
CREATE INDEX IF NOT EXISTS idx_calib_next_date ON equipment_calibration(next_calibration_date);

ALTER TABLE equipment_calibration ADD CONSTRAINT IF NOT EXISTS chk_calib_result
    CHECK (result IN ('PASS', 'FAIL', 'CONDITIONAL'));
ALTER TABLE equipment_calibration ADD CONSTRAINT IF NOT EXISTS chk_calib_type
    CHECK (calibration_type IN ('INTERNAL', 'EXTERNAL'));
