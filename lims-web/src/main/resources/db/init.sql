-- Material LIMS Database Schema (ordered by dependencies)
-- PostgreSQL 15+

-- Independent tables first
CREATE TABLE brand (
    id VARCHAR(36) PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE, description TEXT, sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(), deleted_at TIMESTAMPTZ, version INTEGER DEFAULT 0
);
CREATE TABLE request_type (
    id VARCHAR(36) PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE, task_duration_days INTEGER NOT NULL, part_info_required BOOLEAN DEFAULT TRUE, description TEXT, sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(), deleted_at TIMESTAMPTZ, version INTEGER DEFAULT 0
);
CREATE TABLE holiday (
    id VARCHAR(36) PRIMARY KEY, date DATE NOT NULL, name VARCHAR(200) NOT NULL, type VARCHAR(20) NOT NULL CHECK (type IN ('NATIONAL', 'COMPANY')), year INTEGER NOT NULL,
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(), deleted_at TIMESTAMPTZ, version INTEGER DEFAULT 0, UNIQUE(date, type)
);
CREATE TABLE request_note (
    id VARCHAR(36) PRIMARY KEY, content TEXT NOT NULL, is_active BOOLEAN DEFAULT TRUE, sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(), deleted_at TIMESTAMPTZ, version INTEGER DEFAULT 0
);
CREATE TABLE department (
    id VARCHAR(36) PRIMARY KEY, name VARCHAR(200) NOT NULL, parent_id VARCHAR(36) REFERENCES department(id), external_id VARCHAR(100), level INTEGER DEFAULT 1, sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(), deleted_at TIMESTAMPTZ, version INTEGER DEFAULT 0
);
CREATE TABLE knowledge_doc (
    id VARCHAR(36) PRIMARY KEY, title VARCHAR(500) NOT NULL, category VARCHAR(20) NOT NULL CHECK (category IN ('MANUAL', 'VIDEO')), file_url VARCHAR(1000) NOT NULL, file_size BIGINT, description TEXT,
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(), deleted_at TIMESTAMPTZ, version INTEGER DEFAULT 0
);
CREATE TABLE test_group (
    id VARCHAR(36) PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE, description TEXT, sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(), deleted_at TIMESTAMPTZ, version INTEGER DEFAULT 0
);
CREATE TABLE test_site (
    id VARCHAR(36) PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE, location VARCHAR(500), description TEXT, sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(), deleted_at TIMESTAMPTZ, version INTEGER DEFAULT 0
);
CREATE TABLE analysis_type (
    id VARCHAR(36) PRIMARY KEY, group_id VARCHAR(36) NOT NULL REFERENCES test_group(id), name VARCHAR(200) NOT NULL, description TEXT, sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(), deleted_at TIMESTAMPTZ, version INTEGER DEFAULT 0
);
CREATE TABLE specification (
    id VARCHAR(36) PRIMARY KEY, group_id VARCHAR(36) REFERENCES test_group(id), name VARCHAR(200) NOT NULL, unit VARCHAR(50), description TEXT, sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(), deleted_at TIMESTAMPTZ, version INTEGER DEFAULT 0
);
CREATE TABLE equipment (
    id VARCHAR(36) PRIMARY KEY, name VARCHAR(200) NOT NULL, model VARCHAR(200), serial_number VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'UNDER_REPAIR', 'DECOMMISSIONED')),
    location VARCHAR(500), purchase_date DATE, warranty_expiry DATE, description TEXT,
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(), deleted_at TIMESTAMPTZ, version INTEGER DEFAULT 0
);
CREATE TABLE analysis_item (
    id VARCHAR(36) PRIMARY KEY, group_id VARCHAR(36) NOT NULL REFERENCES test_group(id), site_id VARCHAR(36) REFERENCES test_site(id), type_id VARCHAR(36) NOT NULL REFERENCES analysis_type(id),
    name VARCHAR(200) NOT NULL, equipment_id VARCHAR(36) REFERENCES equipment(id), test_standards VARCHAR(500), specification_id VARCHAR(36) REFERENCES specification(id),
    cost DECIMAL(12,2), unit_price DECIMAL(12,2), unit VARCHAR(50), description TEXT, attachment_url VARCHAR(1000), is_active BOOLEAN DEFAULT TRUE, sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(), deleted_at TIMESTAMPTZ, version INTEGER DEFAULT 0
);
CREATE TABLE sys_user (
    id VARCHAR(36) PRIMARY KEY, email VARCHAR(200) NOT NULL UNIQUE, display_name VARCHAR(200) NOT NULL, login_id VARCHAR(200),
    dept_id VARCHAR(36) REFERENCES department(id), roles VARCHAR(100) DEFAULT 'REQUESTER', external_id VARCHAR(200), is_active BOOLEAN DEFAULT TRUE, last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP DEFAULT NOW()
);
CREATE TABLE request (
    id VARCHAR(36) PRIMARY KEY, request_no VARCHAR(50) NOT NULL UNIQUE, brand_id VARCHAR(36) NOT NULL REFERENCES brand(id), dept_id VARCHAR(36) REFERENCES department(id),
    type_id VARCHAR(36) NOT NULL REFERENCES request_type(id), requester_id VARCHAR(36) NOT NULL REFERENCES sys_user(id), proxy_requester_id VARCHAR(36) REFERENCES sys_user(id),
    real_requester_name VARCHAR(200), part_number VARCHAR(200), part_name VARCHAR(500), eco VARCHAR(200), supplier_code VARCHAR(200), supplier_name VARCHAR(500),
    request_reason TEXT NOT NULL, priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SUBMITTED', 'ASSIGNED', 'SAMPLING', 'REPORTING', 'APPROVING', 'COMPLETED', 'REJECTED')),
    due_date DATE, sample_delivery_note TEXT, total_cost DECIMAL(14,2), process_instance_id VARCHAR(100), submitted_at TIMESTAMP, assigned_at TIMESTAMP,
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(), deleted_at TIMESTAMPTZ, version INTEGER DEFAULT 0
);
CREATE TABLE analysis_task (
    id VARCHAR(36) PRIMARY KEY, request_id VARCHAR(36) NOT NULL REFERENCES request(id), item_id VARCHAR(36) NOT NULL REFERENCES analysis_item(id),
    assignee_id VARCHAR(36) REFERENCES sys_user(id), status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DELAYED', 'COMPLETED')),
    delay_reason TEXT, started_at TIMESTAMP, completed_at TIMESTAMP, sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(), deleted_at TIMESTAMPTZ, version INTEGER DEFAULT 0
);
CREATE TABLE sample (
    id VARCHAR(36) PRIMARY KEY, request_id VARCHAR(36) NOT NULL REFERENCES request(id), received_by VARCHAR(36) REFERENCES sys_user(id), received_at TIMESTAMP,
    preparation_status VARCHAR(20) DEFAULT 'PENDING' CHECK (preparation_status IN ('PENDING', 'PREPARING', 'READY')), preparation_detail TEXT, completed_at TIMESTAMP,
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(), deleted_at TIMESTAMPTZ, version INTEGER DEFAULT 0
);
CREATE TABLE report (
    id VARCHAR(36) PRIMARY KEY, request_id VARCHAR(36) NOT NULL REFERENCES request(id), task_id VARCHAR(36) REFERENCES analysis_task(id),
    author_id VARCHAR(36) NOT NULL REFERENCES sys_user(id), version_number VARCHAR(20) NOT NULL DEFAULT 'V1.0', revision_note TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'IN_REVIEW', 'APPROVED', 'REVISING')),
    file_url VARCHAR(1000), pdf_url VARCHAR(1000), sharepoint_file_id VARCHAR(200), sharepoint_edit_url VARCHAR(1000),
    approved_by VARCHAR(36) REFERENCES sys_user(id), approved_at TIMESTAMP, submitted_at TIMESTAMP,
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW(), deleted_at TIMESTAMPTZ, version INTEGER DEFAULT 0
);
CREATE TABLE report_revision (
    id VARCHAR(36) PRIMARY KEY, report_id VARCHAR(36) NOT NULL REFERENCES report(id), version_number VARCHAR(20) NOT NULL,
    revision_note TEXT, file_url VARCHAR(1000), pdf_url VARCHAR(1000), archived_by VARCHAR(36), archived_at TIMESTAMP DEFAULT NOW(), created_at TIMESTAMP DEFAULT NOW()
);
CREATE TABLE equipment_repair (
    id VARCHAR(36) PRIMARY KEY, equipment_id VARCHAR(36) NOT NULL REFERENCES equipment(id), report_date DATE NOT NULL,
    fault_description TEXT NOT NULL, repair_action TEXT, repair_cost DECIMAL(12,2), repaired_by VARCHAR(200), completion_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'REPORTING' CHECK (status IN ('REPORTING', 'REPAIRING', 'COMPLETED')),
    created_by VARCHAR(36), updated_by VARCHAR(36), created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP DEFAULT NOW(), deleted_at TIMESTAMP, version INTEGER DEFAULT 0
);
CREATE TABLE sys_operation_log (
    id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36) REFERENCES sys_user(id), module VARCHAR(50) NOT NULL, action VARCHAR(20) NOT NULL,
    entity_id VARCHAR(100), detail TEXT, ip VARCHAR(50), created_at TIMESTAMP DEFAULT NOW()
);
CREATE TABLE sys_i18n_message (
    id VARCHAR(36) PRIMARY KEY, message_key VARCHAR(200) NOT NULL, locale VARCHAR(10) NOT NULL, message_value TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP DEFAULT NOW(), UNIQUE(message_key, locale)
);

-- Indexes
CREATE INDEX idx_request_requester ON request(requester_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_request_status ON request(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_request_brand ON request(brand_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_request_created_at ON request(created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_task_request ON analysis_task(request_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_task_assignee ON analysis_task(assignee_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_report_request ON report(request_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_report_status ON report(status) WHERE deleted_at IS NULL;

-- Seed data
INSERT INTO sys_user (id, email, display_name, login_id, roles, is_active) VALUES
('user-admin-001', 'admin@lims.local', 'Admin User', 'admin', 'ADMIN', true),
('user-manager-001', 'manager@lims.local', 'Manager User', 'manager', 'MANAGER', true),
('user-engineer-001', 'engineer@lims.local', 'Engineer User', 'engineer', 'ENGINEER', true),
('user-tech-001', 'tech@lims.local', 'Technician User', 'tech', 'TECHNICIAN', true),
('user-requester-001', 'requester@lims.local', 'Requester User', 'requester', 'REQUESTER', true),
('dev-user-0001', 'dev@lims.local', 'Dev User', 'dev', 'ADMIN,MANAGER,ENGINEER,REQUESTER,TECHNICIAN', true);
INSERT INTO brand (id, name, description, sort_order) VALUES
('brand-001', 'Brand A', 'Test Brand A', 1), ('brand-002', 'Brand B', 'Test Brand B', 2),
('brand-003', 'BASF', 'BASF Chemical Corporation', 3), ('brand-004', 'Dow', 'Dow Chemical Company', 4),
('brand-005', 'Evonik', 'Evonik Industries', 5);
INSERT INTO department (id, name, parent_id, level, sort_order) VALUES
('dept-001', 'Quality Assurance', NULL, 1, 1),
('dept-002', 'R&D Center', NULL, 1, 2),
('dept-003', 'Production', NULL, 1, 3),
('dept-004', 'Chemical Lab', 'dept-001', 2, 1),
('dept-005', 'Physical Lab', 'dept-001', 2, 2),
('dept-006', 'Materials Research', 'dept-002', 2, 1);
INSERT INTO request_type (id, name, task_duration_days, description) VALUES
('type-001', 'Material Analysis', 10, 'Standard material analysis'),
('type-002', 'Component Testing', 15, 'Component-level testing'),
('type-003', 'Failure Analysis', 7, 'Root cause failure analysis'),
('type-004', 'Raw Material Qualification', 20, 'New raw material qualification testing');
INSERT INTO request_note (id, content, is_active, sort_order) VALUES
('note-001', 'Sample must be stored at 25C before testing', true, 1),
('note-002', 'Requires MSDS document attached', true, 2),
('note-003', 'Expedited processing requested by QA manager', true, 3),
('note-004', 'Previous batch had non-conformance report NCR-2026-012', true, 4),
('note-005', 'Follow ASTM E1479 standard method', true, 5);
INSERT INTO test_group (id, name, description) VALUES
('group-001', 'Chemical Analysis', 'Chemical testing group'),
('group-002', 'Physical Testing', 'Mechanical and physical property testing'),
('group-003', 'Environmental Testing', 'Environmental stress and durability testing');
INSERT INTO analysis_type (id, group_id, name) VALUES
('atype-001', 'group-001', 'ICP-OES'),
('atype-002', 'group-001', 'GC-MS'),
('atype-003', 'group-002', 'Tensile Test'),
('atype-004', 'group-002', 'Hardness Test'),
('atype-005', 'group-003', 'Salt Spray Test');
INSERT INTO test_site (id, name, location) VALUES
('site-001', 'Lab A', 'Building 1 Floor 2'),
('site-002', 'Lab B', 'Building 2 Floor 1'),
('site-003', 'Env Chamber', 'Building 3 Room 105');
INSERT INTO specification (id, group_id, name, unit, description) VALUES
('spec-001', 'group-001', 'Heavy Metal Limits', 'ppm', 'Maximum allowable heavy metal content per ASTM F963'),
('spec-002', 'group-001', 'VOC Content', 'g/L', 'Volatile organic compound limits per EPA Method 24'),
('spec-003', 'group-002', 'Tensile Strength', 'MPa', 'Minimum tensile strength requirement'),
('spec-004', 'group-002', 'Shore D Hardness', 'Shore D', 'Hardness specification range 60-80');
INSERT INTO equipment (id, name, model, serial_number, status, location, purchase_date, warranty_expiry, description) VALUES
('equip-001', 'Spectrometer', 'ICP-700', NULL, 'ACTIVE', 'Lab A', NULL, NULL, NULL),
('equip-002', 'GC-MS Analyzer', 'Agilent 7890B', 'GC-2026-0042', 'ACTIVE', 'Lab A', '2024-03-15', '2027-03-15', 'Gas chromatography-mass spectrometry system'),
('equip-003', 'Universal Testing Machine', 'Instron 5967', 'UT-2025-0118', 'ACTIVE', 'Lab B', '2023-08-20', '2026-08-20', 'Electromechanical testing system'),
('equip-004', 'Salt Spray Chamber', 'Q-Fog CCT', 'SS-2024-0077', 'UNDER_REPAIR', 'Env Chamber', '2024-01-10', '2027-01-10', 'Cyclic corrosion test chamber'),
('equip-005', 'Hardness Tester', 'Zwick 3116', 'HT-2025-0033', 'ACTIVE', 'Lab B', '2025-02-01', '2028-02-01', 'Shore D hardness measurement device');
INSERT INTO equipment_repair (id, equipment_id, report_date, fault_description, repair_action, repair_cost, repaired_by, completion_date, status, created_by, updated_by) VALUES
('repair-pending-01', 'equip-004', '2026-06-02', 'Spray nozzle clogged, uneven fog distribution', NULL, NULL, NULL, NULL, 'REPAIRING', 'user-tech-001', 'user-tech-001'),
('repair-done-01', 'equip-002', '2026-05-15', 'Column oven temperature drift beyond 0.5C', 'Replaced oven heater element and recalibrated PID controller', 2800.00, 'user-tech-001', '2026-05-20', 'COMPLETED', 'user-tech-001', 'user-tech-001');
INSERT INTO holiday (id, date, name, type, year) VALUES
('hol-001', '2026-01-01', 'New Year''s Day', 'NATIONAL', 2026),
('hol-002', '2026-01-29', 'Spring Festival', 'NATIONAL', 2026),
('hol-003', '2026-01-30', 'Spring Festival Holiday', 'NATIONAL', 2026),
('hol-004', '2026-04-04', 'Qingming Festival', 'NATIONAL', 2026),
('hol-005', '2026-05-01', 'Labor Day', 'NATIONAL', 2026),
('hol-006', '2026-06-19', 'Dragon Boat Festival', 'NATIONAL', 2026),
('hol-007', '2026-10-01', 'National Day', 'NATIONAL', 2026),
('hol-008', '2026-12-25', 'Company Holiday', 'COMPANY', 2026);
INSERT INTO analysis_item (id, group_id, type_id, site_id, name, cost, is_active, sort_order, specification_id) VALUES
('item-001', 'group-001', 'atype-001', 'site-001', 'ICP Metal Analysis', 500.00, true, 1, 'spec-001'),
('item-002', 'group-001', 'atype-001', 'site-001', 'ICP Trace Elements', 800.00, true, 2, NULL),
('item-003', 'group-001', 'atype-002', 'site-001', 'VOC Screening', 600.00, true, 3, 'spec-002'),
('item-004', 'group-001', 'atype-001', 'site-001', 'Heavy Metal Screening', 750.00, true, 4, 'spec-001'),
('item-005', 'group-002', 'atype-003', 'site-002', 'Tensile Strength Test', 400.00, true, 1, 'spec-003'),
('item-006', 'group-002', 'atype-004', 'site-002', 'Hardness Measurement', 300.00, true, 2, 'spec-004'),
('item-007', 'group-003', 'atype-005', 'site-003', 'Salt Spray 500h', 1200.00, true, 1, NULL),
('item-008', 'group-001', 'atype-002', 'site-001', 'Residual Solvent Analysis', 550.00, true, 5, NULL);

-- Business flow test data
INSERT INTO request (id, request_no, brand_id, dept_id, type_id, requester_id, proxy_requester_id, real_requester_name, part_number, part_name, eco, supplier_code, supplier_name, request_reason, priority, status, due_date, sample_delivery_note, total_cost, submitted_at, assigned_at, created_by, updated_by) VALUES
('req-001', 'REQ-2026-0001', 'brand-001', 'dept-004', 'type-001', 'user-requester-001', NULL, NULL, 'PN-A100', 'Polymer Resin Batch 42', 'ECO-2026-015', 'SUP-BASF-01', 'BASF Shanghai', 'New batch qualification - incoming material inspection', 'NORMAL', 'COMPLETED', '2026-06-14', 'Deliver to Lab A reception desk before 10:00 AM', 1300.00, '2026-06-02 09:15:00', '2026-06-02 10:30:00', 'user-requester-001', 'user-engineer-001'),
('req-002', 'REQ-2026-0002', 'brand-002', 'dept-005', 'type-002', 'user-requester-001', 'user-admin-001', 'Zhang Wei', 'PN-B200', 'Steel Bracket Assembly', 'ECO-2026-018', 'SUP-DOW-02', 'Dow China', 'Component reliability test for new supplier qualification', 'HIGH', 'REPORTING', '2026-06-20', '2 units required, packed in anti-static bags', 1900.00, '2026-06-03 14:20:00', '2026-06-03 15:00:00', 'user-requester-001', 'user-engineer-001'),
('req-003', 'REQ-2026-0003', 'brand-003', 'dept-004', 'type-001', 'user-requester-001', NULL, NULL, 'PN-C300', 'Coating Solvent X-7', NULL, 'SUP-EVO-03', 'Evonik Asia', 'VOC compliance verification for regulatory submission', 'URGENT', 'SAMPLING', '2026-06-11', 'Sample must be kept below 25C, MSDS attached', 1350.00, '2026-06-04 08:45:00', '2026-06-04 09:10:00', 'user-requester-001', 'user-engineer-001'),
('req-004', 'REQ-2026-0004', 'brand-001', 'dept-006', 'type-003', 'user-requester-001', NULL, NULL, 'PN-D400', 'Injection Molded Housing', 'ECO-2026-022', 'SUP-BASF-01', 'BASF Shanghai', 'Field failure investigation - crack at mounting hole', 'URGENT', 'SUBMITTED', '2026-06-12', 'Failed part and 3 reference parts needed', NULL, '2026-06-05 16:30:00', NULL, 'user-requester-001', 'user-requester-001'),
('req-005', 'REQ-2026-0005', 'brand-004', 'dept-004', 'type-004', 'user-requester-001', NULL, NULL, 'PN-E500', 'Epoxy Adhesive EA-900', NULL, 'SUP-DOW-02', 'Dow China', 'New raw material qualification per SOP-QA-017', 'NORMAL', 'APPROVING', '2026-06-15', '6 sample containers, minimum 500g each', 3250.00, '2026-05-28 11:00:00', '2026-05-28 14:00:00', 'user-requester-001', 'user-engineer-001');

INSERT INTO analysis_task (id, request_id, item_id, assignee_id, status, delay_reason, started_at, completed_at, sort_order, created_by, updated_by) VALUES
('task-001', 'req-001', 'item-001', 'user-engineer-001', 'COMPLETED', NULL, '2026-06-03 08:00:00', '2026-06-05 16:30:00', 1, 'user-engineer-001', 'user-engineer-001'),
('task-002', 'req-001', 'item-004', 'user-tech-001', 'COMPLETED', NULL, '2026-06-03 09:00:00', '2026-06-06 11:00:00', 2, 'user-tech-001', 'user-tech-001'),
('task-003', 'req-002', 'item-005', 'user-engineer-001', 'IN_PROGRESS', NULL, '2026-06-04 10:00:00', NULL, 1, 'user-engineer-001', 'user-engineer-001'),
('task-004', 'req-002', 'item-006', 'user-tech-001', 'PENDING', NULL, NULL, NULL, 2, 'user-engineer-001', 'user-engineer-001'),
('task-005', 'req-003', 'item-003', 'user-engineer-001', 'IN_PROGRESS', NULL, '2026-06-05 08:30:00', NULL, 1, 'user-engineer-001', 'user-engineer-001'),
('task-006', 'req-003', 'item-004', 'user-tech-001', 'PENDING', NULL, NULL, NULL, 2, 'user-engineer-001', 'user-engineer-001'),
('task-007', 'req-005', 'item-001', 'user-engineer-001', 'COMPLETED', NULL, '2026-05-29 09:00:00', '2026-05-30 17:00:00', 1, 'user-engineer-001', 'user-engineer-001'),
('task-008', 'req-005', 'item-003', 'user-tech-001', 'DELAYED', 'GC-MS recalibration required', '2026-06-01 08:00:00', NULL, 2, 'user-tech-001', 'user-tech-001'),
('task-009', 'req-005', 'item-008', 'user-engineer-001', 'PENDING', NULL, NULL, NULL, 3, 'user-engineer-001', 'user-engineer-001');

INSERT INTO sample (id, request_id, received_by, received_at, preparation_status, preparation_detail, completed_at, created_by, updated_by) VALUES
('sample-001', 'req-001', 'user-tech-001', '2026-06-02 14:00:00', 'READY', 'Weighed 5g aliquots for ICP analysis x3', '2026-06-02 16:00:00', 'user-tech-001', 'user-tech-001'),
('sample-002', 'req-001', 'user-tech-001', '2026-06-02 14:00:00', 'READY', 'Retained 500g backup sample', '2026-06-02 15:30:00', 'user-tech-001', 'user-tech-001'),
('sample-003', 'req-002', 'user-tech-001', '2026-06-03 16:30:00', 'PREPARING', 'Mounting specimens on tensile test grips', NULL, 'user-tech-001', 'user-tech-001'),
('sample-004', 'req-002', 'user-tech-001', '2026-06-03 16:30:00', 'PENDING', NULL, NULL, 'user-tech-001', 'user-tech-001'),
('sample-005', 'req-003', 'user-tech-001', '2026-06-04 10:00:00', 'PREPARING', 'Diluting solvent sample 1:100 for GC-MS', NULL, 'user-tech-001', 'user-tech-001'),
('sample-006', 'req-004', NULL, NULL, 'PENDING', NULL, NULL, 'user-requester-001', 'user-requester-001'),
('sample-007', 'req-005', 'user-tech-001', '2026-05-29 09:30:00', 'READY', 'Divided into 6 test portions, labeled EA-900-A through F', '2026-05-29 14:00:00', 'user-tech-001', 'user-tech-001'),
('sample-008', 'req-005', 'user-tech-001', '2026-05-29 09:30:00', 'READY', 'Retained 3 sealed containers as backup', '2026-05-29 14:00:00', 'user-tech-001', 'user-tech-001');

INSERT INTO report (id, request_id, task_id, author_id, version_number, revision_note, status, file_url, pdf_url, approved_by, approved_at, submitted_at, created_by, updated_by) VALUES
('rpt-001', 'req-001', 'task-001', 'user-engineer-001', 'V1.1', 'Added heavy metal detail table', 'APPROVED', '/reports/rpt-001/V1.1.docx', '/reports/rpt-001/V1.1.pdf', 'user-manager-001', '2026-06-06 09:00:00', '2026-06-05 17:30:00', 'user-engineer-001', 'user-manager-001'),
('rpt-002', 'req-005', 'task-007', 'user-engineer-001', 'V1.0', 'Initial draft - partial results', 'IN_REVIEW', '/reports/rpt-002/V1.0.docx', '/reports/rpt-002/V1.0.pdf', NULL, NULL, NULL, 'user-engineer-001', 'user-engineer-001');

INSERT INTO report_revision (id, report_id, version_number, revision_note, file_url, pdf_url, archived_by, archived_at) VALUES
('rev-001', 'rpt-001', 'V1.0', 'Initial draft', '/reports/rpt-001/V1.0.docx', '/reports/rpt-001/V1.0.pdf', 'user-engineer-001', '2026-06-05 14:00:00'),
('rev-002', 'rpt-001', 'V1.1', 'Added heavy metal detail table per reviewer request', '/reports/rpt-001/V1.1.docx', '/reports/rpt-001/V1.1.pdf', 'user-engineer-001', '2026-06-05 16:30:00'),
('rev-003', 'rpt-002', 'V1.0', 'Initial draft - partial results', '/reports/rpt-002/V1.0.docx', '/reports/rpt-002/V1.0.pdf', 'user-engineer-001', '2026-06-04 11:00:00');

-- i18n bilingual entries
INSERT INTO sys_i18n_message (id, message_key, locale, message_value, created_at, updated_at) VALUES
('i18n-001', 'menu.dashboard', 'zh-CN', '仪表盘', NOW(), NOW()),
('i18n-002', 'menu.dashboard', 'en-US', 'Dashboard', NOW(), NOW()),
('i18n-003', 'menu.request', 'zh-CN', '委托单', NOW(), NOW()),
('i18n-004', 'menu.request', 'en-US', 'Request', NOW(), NOW()),
('i18n-005', 'menu.report', 'zh-CN', '报告', NOW(), NOW()),
('i18n-006', 'menu.report', 'en-US', 'Report', NOW(), NOW()),
('i18n-007', 'menu.equipment', 'zh-CN', '设备管理', NOW(), NOW()),
('i18n-008', 'menu.equipment', 'en-US', 'Equipment', NOW(), NOW()),
('i18n-009', 'menu.knowledge', 'zh-CN', '知识库', NOW(), NOW()),
('i18n-010', 'menu.knowledge', 'en-US', 'Knowledge Hub', NOW(), NOW()),
('i18n-011', 'status.PENDING', 'zh-CN', '待处理', NOW(), NOW()),
('i18n-012', 'status.PENDING', 'en-US', 'Pending', NOW(), NOW()),
('i18n-013', 'status.IN_PROGRESS', 'zh-CN', '进行中', NOW(), NOW()),
('i18n-014', 'status.IN_PROGRESS', 'en-US', 'In Progress', NOW(), NOW()),
('i18n-015', 'status.COMPLETED', 'zh-CN', '已完成', NOW(), NOW()),
('i18n-016', 'status.COMPLETED', 'en-US', 'Completed', NOW(), NOW()),
('i18n-017', 'status.DELAYED', 'zh-CN', '已延期', NOW(), NOW()),
('i18n-018', 'status.DELAYED', 'en-US', 'Delayed', NOW(), NOW()),
('i18n-019', 'status.DRAFT', 'zh-CN', '草稿', NOW(), NOW()),
('i18n-020', 'status.DRAFT', 'en-US', 'Draft', NOW(), NOW()),
('i18n-021', 'status.APPROVED', 'zh-CN', '已批准', NOW(), NOW()),
('i18n-022', 'status.APPROVED', 'en-US', 'Approved', NOW(), NOW()),
('i18n-023', 'priority.HIGH', 'zh-CN', '高优先级', NOW(), NOW()),
('i18n-024', 'priority.HIGH', 'en-US', 'High Priority', NOW(), NOW()),
('i18n-025', 'priority.URGENT', 'zh-CN', '紧急', NOW(), NOW()),
('i18n-026', 'priority.URGENT', 'en-US', 'Urgent', NOW(), NOW()),
('i18n-027', 'priority.NORMAL', 'zh-CN', '普通', NOW(), NOW()),
('i18n-028', 'priority.NORMAL', 'en-US', 'Normal', NOW(), NOW());
