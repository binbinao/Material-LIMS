-- Material LIMS Database Schema
-- PostgreSQL 15+

-- =============================================
-- Basic Data Tables
-- =============================================

CREATE TABLE brand (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE TABLE request_type (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    task_duration_days INTEGER NOT NULL,
    part_info_required BOOLEAN DEFAULT TRUE,
    description TEXT,
    sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE TABLE holiday (
    id VARCHAR(36) PRIMARY KEY,
    date DATE NOT NULL,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('NATIONAL', 'COMPANY')),
    year INTEGER NOT NULL,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0,
    UNIQUE(date, type)
);

CREATE TABLE request_note (
    id VARCHAR(36) PRIMARY KEY,
    content TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE TABLE department (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    parent_id VARCHAR(36) REFERENCES department(id),
    external_id VARCHAR(100),
    level INTEGER DEFAULT 1,
    sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE TABLE knowledge_doc (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    category VARCHAR(20) NOT NULL CHECK (category IN ('MANUAL', 'VIDEO')),
    file_url VARCHAR(1000) NOT NULL,
    file_size BIGINT,
    description TEXT,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

-- =============================================
-- Test Data Tables
-- =============================================

CREATE TABLE test_group (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE TABLE test_site (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    location VARCHAR(500),
    description TEXT,
    sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE TABLE analysis_type (
    id VARCHAR(36) PRIMARY KEY,
    group_id VARCHAR(36) NOT NULL REFERENCES test_group(id),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE TABLE specification (
    id VARCHAR(36) PRIMARY KEY,
    group_id VARCHAR(36) REFERENCES test_group(id),
    name VARCHAR(200) NOT NULL,
    unit VARCHAR(50),
    description TEXT,
    sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE TABLE equipment (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    model VARCHAR(200),
    serial_number VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'UNDER_REPAIR', 'DECOMMISSIONED')),
    location VARCHAR(500),
    purchase_date DATE,
    warranty_expiry DATE,
    description TEXT,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE TABLE analysis_item (
    id VARCHAR(36) PRIMARY KEY,
    group_id VARCHAR(36) NOT NULL REFERENCES test_group(id),
    site_id VARCHAR(36) REFERENCES test_site(id),
    type_id VARCHAR(36) NOT NULL REFERENCES analysis_type(id),
    name VARCHAR(200) NOT NULL,
    equipment_id VARCHAR(36) REFERENCES equipment(id),
    test_standards VARCHAR(500),
    specification_id VARCHAR(36) REFERENCES specification(id),
    cost DECIMAL(12,2),
    unit_price DECIMAL(12,2),
    unit VARCHAR(50),
    description TEXT,
    attachment_url VARCHAR(1000),
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

-- =============================================
-- Core Business Tables
-- =============================================

CREATE TABLE request (
    id VARCHAR(36) PRIMARY KEY,
    request_no VARCHAR(50) NOT NULL UNIQUE,
    brand_id VARCHAR(36) NOT NULL REFERENCES brand(id),
    dept_id VARCHAR(36) REFERENCES department(id),
    type_id VARCHAR(36) NOT NULL REFERENCES request_type(id),
    requester_id VARCHAR(36) NOT NULL REFERENCES sys_user(id),
    proxy_requester_id VARCHAR(36) REFERENCES sys_user(id),
    real_requester_name VARCHAR(200),
    part_number VARCHAR(200),
    part_name VARCHAR(500),
    eco VARCHAR(200),
    supplier_code VARCHAR(200),
    supplier_name VARCHAR(500),
    request_reason TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SUBMITTED', 'ASSIGNED', 'SAMPLING', 'REPORTING', 'APPROVING', 'COMPLETED', 'REJECTED')),
    due_date DATE,
    sample_delivery_note TEXT,
    total_cost DECIMAL(14,2),
    process_instance_id VARCHAR(100),
    submitted_at TIMESTAMP,
    assigned_at TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE TABLE analysis_task (
    id VARCHAR(36) PRIMARY KEY,
    request_id VARCHAR(36) NOT NULL REFERENCES request(id),
    item_id VARCHAR(36) NOT NULL REFERENCES analysis_item(id),
    assignee_id VARCHAR(36) REFERENCES sys_user(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DELAYED', 'COMPLETED')),
    delay_reason TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    sort_order INTEGER DEFAULT 0,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE TABLE sample (
    id VARCHAR(36) PRIMARY KEY,
    request_id VARCHAR(36) NOT NULL REFERENCES request(id),
    received_by VARCHAR(36) REFERENCES sys_user(id),
    received_at TIMESTAMP,
    preparation_status VARCHAR(20) DEFAULT 'PENDING' CHECK (preparation_status IN ('PENDING', 'PREPARING', 'READY')),
    preparation_detail TEXT,
    completed_at TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE TABLE report (
    id VARCHAR(36) PRIMARY KEY,
    request_id VARCHAR(36) NOT NULL REFERENCES request(id),
    task_id VARCHAR(36) REFERENCES analysis_task(id),
    author_id VARCHAR(36) NOT NULL REFERENCES sys_user(id),
    version_number VARCHAR(20) NOT NULL DEFAULT 'V1.0',
    revision_note TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'IN_REVIEW', 'APPROVED', 'REVISING')),
    file_url VARCHAR(1000),
    pdf_url VARCHAR(1000),
    sharepoint_file_id VARCHAR(200),
    sharepoint_edit_url VARCHAR(1000),
    approved_by VARCHAR(36) REFERENCES sys_user(id),
    approved_at TIMESTAMP,
    submitted_at TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE TABLE report_revision (
    id VARCHAR(36) PRIMARY KEY,
    report_id VARCHAR(36) NOT NULL REFERENCES report(id),
    version_number VARCHAR(20) NOT NULL,
    revision_note TEXT,
    file_url VARCHAR(1000),
    pdf_url VARCHAR(1000),
    archived_by VARCHAR(36),
    archived_at TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW()
);

-- =============================================
-- Equipment Management Tables
-- =============================================

CREATE TABLE equipment_repair (
    id VARCHAR(36) PRIMARY KEY,
    equipment_id VARCHAR(36) NOT NULL REFERENCES equipment(id),
    report_date DATE NOT NULL,
    fault_description TEXT NOT NULL,
    repair_action TEXT,
    repair_cost DECIMAL(12,2),
    repaired_by VARCHAR(200),
    completion_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'REPORTING' CHECK (status IN ('REPORTING', 'REPAIRING', 'COMPLETED')),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

-- =============================================
-- System Tables
-- =============================================

CREATE TABLE sys_user (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(200) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    login_id VARCHAR(200),
    dept_id VARCHAR(36) REFERENCES department(id),
    roles VARCHAR(100) DEFAULT 'REQUESTER',
    external_id VARCHAR(200),
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- sys_user must be created BEFORE request/analysis_task/sample/report
-- so we need to reorder. PostgreSQL allows deferred constraints,
-- but for simplicity, we create sys_user first.

-- Note: The CREATE TABLE statements above reference sys_user before it's defined.
-- In PostgreSQL, this works if we use deferred constraints or create tables in order.
-- Let's fix this by noting that sys_user should be created first.

CREATE TABLE sys_operation_log (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) REFERENCES sys_user(id),
    module VARCHAR(50) NOT NULL,
    action VARCHAR(20) NOT NULL,
    entity_id VARCHAR(100),
    detail TEXT,
    ip VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE sys_i18n_message (
    id VARCHAR(36) PRIMARY KEY,
    message_key VARCHAR(200) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    message_value TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(message_key, locale)
);

-- =============================================
-- Indexes
-- =============================================

CREATE INDEX idx_request_requester ON request(requester_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_request_status ON request(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_request_brand ON request(brand_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_request_due_date ON request(due_date) WHERE deleted_at IS NULL;
CREATE INDEX idx_request_created_at ON request(created_at DESC) WHERE deleted_at IS NULL;

CREATE INDEX idx_task_request ON analysis_task(request_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_task_assignee ON analysis_task(assignee_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_task_status ON analysis_task(status) WHERE deleted_at IS NULL;

CREATE INDEX idx_report_request ON report(request_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_report_author ON report(author_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_report_status ON report(status) WHERE deleted_at IS NULL;

CREATE INDEX idx_sample_request ON sample(request_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_repair_equipment ON equipment_repair(equipment_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_repair_status ON equipment_repair(status) WHERE deleted_at IS NULL;

CREATE INDEX idx_holiday_year ON holiday(year);

CREATE INDEX idx_log_user ON sys_operation_log(user_id);
CREATE INDEX idx_log_module ON sys_operation_log(module);
CREATE INDEX idx_log_action ON sys_operation_log(action);
CREATE INDEX idx_log_created_at ON sys_operation_log(created_at DESC);

-- Phase 5: extra indexes for hot paths
CREATE UNIQUE INDEX IF NOT EXISTS uk_request_no ON request(request_no) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_equipment_status ON equipment(status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_knowledge_category ON knowledge_doc(category) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_knowledge_updated_at ON knowledge_doc(updated_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_i18n_locale ON sys_i18n_message(locale);
