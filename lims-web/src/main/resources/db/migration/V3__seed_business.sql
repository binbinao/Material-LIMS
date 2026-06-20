-- Flyway V3 — Business seed data
-- Purpose: provide 30-100 rows per entity so every form page in lims-web-ui
-- has rich, business-realistic data to render. Each INSERT is wrapped in
-- ON CONFLICT (id) DO NOTHING so the migration is idempotent.
--
-- ID conventions (see docs/superpowers/specs/2026-06-19-form-test-data-design.md):
--   - V3 dictionary/business rows: <table>-1NN (e.g. brand-101, req-101)
--   - V4 edge-case rows:          <table>-edge-NNN
--   - V2 base rows (-001..):      untouched
--
-- Schema corrections vs. spec (commit will reference these):
--   - report.status: REVISING (not REVISED); no REJECTED in enum
--   - equipment.status: ACTIVE/UNDER_REPAIR/DECOMMISSIONED (no INACTIVE/RETIRED)
--   - equipment_repair.status: REPORTING/REPAIRING/COMPLETED (no REPORTED/CANCELLED)
--   - knowledge_doc: only category (MANUAL/VIDEO), no visibility/tags columns

-- =============================================
-- 1. Dictionary tables — extend V2 with 4-6 rows each
-- =============================================

INSERT INTO brand (id, name, description, sort_order, created_by, updated_by, created_at, updated_at) VALUES
  ('brand-101', 'DuPont',         'DuPont de Nemours Inc.',         6, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('brand-102', '3M',             '3M Company',                     7, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('brand-103', 'Henkel',         'Henkel AG & Co. KGaA',           8, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('brand-104', 'Wacker',         'Wacker Chemie AG',               9, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('brand-105', 'Momentive',      'Momentive Performance Materials',10, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO department (id, name, parent_id, level, sort_order, created_by, updated_by, created_at, updated_at) VALUES
  ('dept-101', 'Supply Chain',        NULL,         1, 4, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('dept-102', 'Procurement',         'dept-101',   2, 1, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('dept-103', 'Vendor Quality',      'dept-101',   2, 2, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('dept-104', 'Compliance',          NULL,         1, 5, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO request_type (id, name, task_duration_days, part_info_required, description, sort_order, created_by, updated_by, created_at, updated_at) VALUES
  ('type-101', 'Comparative Analysis',     12, TRUE,  'Side-by-side comparison against reference material',  5, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('type-102', 'Aging Study',              30, TRUE,  'Long-term aging under controlled conditions',          6, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('type-103', 'Reverse Engineering',      25, FALSE, 'Dismantle and analyse competitor product structure',   7, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO request_note (id, content, is_active, sort_order, created_by, updated_by, created_at, updated_at) VALUES
  ('note-101', 'MSDS must be attached and version < 24 months',                TRUE,  6, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('note-102', 'Customer requires report in both EN and CN',                  TRUE,  7, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('note-103', 'Hazardous sample — handle per SOP-SAF-002',                   TRUE,  8, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('note-104', 'Photo evidence required at sample receipt',                    TRUE,  9, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO test_group (id, name, description, sort_order, created_by, updated_by, created_at, updated_at) VALUES
  ('group-101', 'Thermal Analysis',    'DSC, TGA, DMA, thermal conductivity',        4, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('group-102', 'Rheology & Viscosity','Melt flow, rotational viscometry',          5, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO analysis_type (id, group_id, name, description, sort_order, created_by, updated_by, created_at, updated_at) VALUES
  ('atype-101', 'group-101', 'DSC',         'Differential Scanning Calorimetry',         6, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('atype-102', 'group-101', 'TGA',         'Thermogravimetric Analysis',               7, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('atype-103', 'group-102', 'MFR',         'Melt Flow Rate per ISO 1133',              8, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('atype-104', 'group-102', 'Viscosity',   'Rotational viscometry at controlled shear', 9, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO test_site (id, name, location, description, sort_order, created_by, updated_by, created_at, updated_at) VALUES
  ('site-101', 'Thermal Lab',  'Building 4 Floor 1', 'DSC/TGA/thermal-conductivity instruments', 4, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('site-102', 'Rheology Lab', 'Building 4 Floor 2', 'Melt indexer and rotational viscometer',  5, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO specification (id, group_id, name, unit, description, sort_order, created_by, updated_by, created_at, updated_at) VALUES
  ('spec-101', 'group-101', 'Glass Transition Temp',  'degC',    'Tg midpoint per ASTM D3418',         5, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('spec-102', 'group-101', 'Decomposition Onset',    'degC',    '5% mass loss temperature in N2',     6, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('spec-103', 'group-102', 'Melt Flow Index',        'g/10min', 'MFR per ISO 1133 condition 190/2.16',7, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('spec-104', 'group-102', 'Viscosity at 200/s',     'Pa.s',    'Capillary rheometer at 200 1/s',     8, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO equipment (id, name, model, serial_number, status, location, purchase_date, warranty_expiry, description, created_by, updated_by, created_at, updated_at) VALUES
  ('equip-101', 'DSC Calorimeter',          'TA Q200',       'DSC-2025-0011', 'ACTIVE',         'Thermal Lab',  '2025-04-12', '2028-04-12', 'Heat-cool-heat cycle -50C to 400C',     'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('equip-102', 'TGA Analyzer',             'TA Q500',       'TGA-2024-0044', 'ACTIVE',         'Thermal Lab',  '2024-07-22', '2027-07-22', 'Thermogravimetric up to 1000C',          'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('equip-103', 'Melt Flow Indexer',        'Tinius OP5',    'MFI-2023-0029', 'UNDER_REPAIR',   'Rheology Lab', '2023-09-05', '2026-09-05', 'Piston wear under recalibration',        'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('equip-104', 'Rotational Viscometer',    'Brookfield DV2','RV-2025-0007',  'DECOMMISSIONED', 'Rheology Lab', '2018-02-10', NULL,         'Replaced by DV3 model in 2025-Q2',     'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO holiday (id, date, name, type, year, created_by, updated_by, created_at, updated_at) VALUES
  ('hol-101', '2026-09-25', 'Mid-Autumn Festival',         'NATIONAL', 2026, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('hol-102', '2026-09-26', 'Mid-Autumn Holiday',         'NATIONAL', 2026, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('hol-103', '2026-09-27', 'Weekend Make-up Day (Sat)',  'COMPANY',  2026, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('hol-104', '2026-10-02', 'National Day Holiday',       'NATIONAL', 2026, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('hol-105', '2026-10-03', 'National Day Holiday',       'NATIONAL', 2026, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO analysis_item (id, group_id, site_id, type_id, name, equipment_id, test_standards, specification_id, cost, unit_price, unit, description, is_active, sort_order, created_by, updated_by, created_at, updated_at) VALUES
  ('item-101', 'group-101', 'site-101', 'atype-101', 'DSC Glass Transition',      'equip-101', 'ASTM D3418', 'spec-101', 450.00, 450.00, 'per sample', 'Standard DSC ramp 10C/min',         TRUE, 6, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('item-102', 'group-101', 'site-101', 'atype-101', 'DSC Melting/Crystallisation','equip-101', 'ASTM D3418', NULL,        500.00, 500.00, 'per sample', 'Cool-heat cycle to capture melt peaks', TRUE, 7, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('item-103', 'group-101', 'site-101', 'atype-102', 'TGA Thermal Stability',     'equip-102', 'ASTM E1131', 'spec-102', 600.00, 600.00, 'per sample', 'Ramp 10C/min to 800C in N2',      TRUE, 8, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('item-104', 'group-102', 'site-102', 'atype-103', 'Melt Flow Rate 190/2.16',   'equip-103', 'ISO 1133',   'spec-103', 250.00, 250.00, 'per sample', 'Standard MFI condition 190/2.16', TRUE, 3, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('item-105', 'group-102', 'site-102', 'atype-104', 'Rotational Viscosity 200/s','equip-104', 'ISO 3219',   'spec-104', 380.00, 380.00, 'per sample', 'Cone-plate at 25C and 200 1/s',   TRUE, 4, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('item-106', 'group-102', 'site-102', 'atype-104', 'Viscosity Sweep 1-1000/s',  'equip-104', 'ISO 3219',   NULL,        520.00, 520.00, 'per sample', 'Logarithmic shear sweep',          TRUE, 5, 'dev-user-0001', 'dev-user-0001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 2. Users (5 new) — extend V2's 6 users
-- =============================================

INSERT INTO sys_user (id, email, display_name, login_id, dept_id, roles, is_active, last_login_at, created_at, updated_at) VALUES
  ('user-101', 'approver@lims.local',    'Manager Approver',    'approver',   'dept-001', 'MANAGER_APPROVER', TRUE, '2026-06-19 08:30:00', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('user-102', 'qc.reviewer@lims.local', 'QC Reviewer',         'qc',         'dept-001', 'QC_REVIEWER',      TRUE, '2026-06-19 14:10:00', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('user-103', 'procurement@lims.local', 'Procurement Officer', 'procurement','dept-102', 'REQUESTER',        TRUE, '2026-06-19 11:00:00', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('user-104', 'compliance@lims.local',  'Compliance Officer',  'compliance', 'dept-104', 'COMPLIANCE',       TRUE, '2026-06-18 16:45:00', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('user-105', 'intern@lims.local',      'Lab Intern',          'intern',     'dept-004', 'TECHNICIAN',       TRUE, '2026-06-19 09:20:00', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 3. Knowledge documents (10) — V2 has none
-- =============================================
-- Schema only has category (MANUAL/VIDEO), no visibility/tags columns.

INSERT INTO knowledge_doc (id, title, category, file_url, file_size, description, created_by, updated_by, created_at, updated_at) VALUES
  ('doc-101', 'ICP-OES Operation Manual',                 'MANUAL', '/knowledge/icp-oes/manual-v3.pdf',       2400000,   'Detailed startup, calibration, and shutdown for the ICP-OES spectrometer', 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('doc-102', 'GC-MS Column Selection Guide',             'MANUAL', '/knowledge/gc-ms/column-guide.pdf',      1850000,   'Choosing the right column for VOC and semi-volatile analytes',               'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('doc-103', 'Tensile Test Specimen Prep SOP',           'MANUAL', '/knowledge/tensile/sop-qa-014.pdf',       980000,   'Step-by-step specimen machining and grip selection',                        'user-tech-001',      'user-tech-001',      '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('doc-104', 'Salt Spray Chamber Maintenance Video',     'VIDEO',  '/knowledge/salt-spray/maintenance.mp4', 125000000,  'Quarterly maintenance walkthrough for the Q-Fog CCT',                        'user-tech-001',      'user-tech-001',      '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('doc-105', 'DSC Quick Reference Card',                 'MANUAL', '/knowledge/thermal/dsc-quickref.pdf',      320000,   'Laminated desk card with method presets for common polymers',                'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('doc-106', 'TGA Atmosphere Selection Guide',           'MANUAL', '/knowledge/thermal/tga-atmospheres.pdf',   720000,   'When to use N2 vs air vs O2, with example curves',                           'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('doc-107', 'Hardness Testing Calibration Video',       'VIDEO',  '/knowledge/hardness/calibration.mp4',    89000000,  'Annual calibration procedure for the Zwick 3116',                            'user-tech-001',      'user-tech-001',      '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('doc-108', 'LIMS Request Submission Walkthrough',      'MANUAL', '/knowledge/lims/request-submit.pdf',       540000,   'How to fill the request form correctly the first time',                     'user-admin-001',     'user-admin-001',     '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('doc-109', 'Melt Flow Rate Troubleshooting',           'MANUAL', '/knowledge/rheology/mfr-troubleshoot.pdf', 670000,   'Common MFR measurement pitfalls and how to recognise them',                  'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('doc-110', 'Sample Receipt Best Practices',            'MANUAL', '/knowledge/general/sample-receipt.pdf',    430000,   'Photo, label, and storage guidance for incoming samples',                   'user-tech-001',      'user-tech-001',      '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 4. Equipment repairs (10)
-- =============================================

INSERT INTO equipment_repair (id, equipment_id, report_date, fault_description, repair_action, repair_cost, repaired_by, completion_date, status, created_by, updated_by, created_at, updated_at) VALUES
  ('repair-101', 'equip-101', '2026-06-12', 'Baseline drift on DSC sensor after 18 months use',                'Recalibrated sensor and replaced purge gas filter',    1850.00, 'user-tech-001', '2026-06-14', 'COMPLETED', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('repair-102', 'equip-101', '2026-06-18', 'Intermittent communication loss with controller',               'Pending vendor diagnosis',                             NULL,     NULL,            NULL,         'REPORTING',  'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('repair-103', 'equip-102', '2026-06-08', 'TGA balance calibration off by 0.5mg at 100mg reference',       'In-house recalibration using certified 100mg weight',  0.00,    'user-tech-001', '2026-06-09', 'COMPLETED', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('repair-104', 'equip-102', '2026-06-17', 'Heating furnace error E-04 during ramp above 700C',            'Vendor technician scheduled for next week',             NULL,     NULL,            NULL,         'REPAIRING',  'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('repair-105', 'equip-103', '2026-06-19', 'Piston rod showing wear marks; MFI readings drift upward',      'Piston replacement kit ordered from Tinius Olsen',      4200.00, 'user-tech-001', NULL,         'REPAIRING',  'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('repair-106', 'equip-103', '2026-06-10', 'Loading chute scratched, sample fragments accumulating',         NULL,                                                   NULL,     NULL,            NULL,         'REPORTING',  'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('repair-107', 'equip-104', '2026-05-30', 'Final maintenance before decommissioning',                      'Documented condition; scheduled for scrap pickup',     150.00,  'user-tech-001', '2026-06-01', 'COMPLETED', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('repair-108', 'equip-101', '2026-06-20', 'Auto-sampler carousel alignment off, samples dropped mid-run',  'Vendor remote session in progress',                    NULL,     NULL,            NULL,         'REPAIRING',  'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('repair-109', 'equip-102', '2026-06-15', 'N2 purge line flow rate dropped below 50 mL/min',               'Replaced flow restrictor and re-purged',                320.00,  'user-tech-001', '2026-06-15', 'COMPLETED', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('repair-110', 'equip-103', '2026-06-19', 'Temperature controller display intermittent',                  NULL,                                                   NULL,     NULL,            NULL,         'REPORTING',  'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 5. Requests (40 rows) — status x priority matrix
-- =============================================
-- Status: SUBMITTED, ASSIGNED, SAMPLING, REPORTING, APPROVING, COMPLETED, REJECTED
-- Priority: URGENT, HIGH, NORMAL, LOW
-- Total: 8+5+5+6+4+10+2 = 40 rows. IDs req-101..req-140, request_no REQ-2026-0101..0140

INSERT INTO request (id, request_no, brand_id, dept_id, type_id, requester_id, proxy_requester_id, real_requester_name, part_number, part_name, eco, supplier_code, supplier_name, request_reason, priority, status, due_date, sample_delivery_note, total_cost, submitted_at, assigned_at, created_by, updated_by, created_at, updated_at) VALUES
  -- SUBMITTED x {URGENT x2, HIGH x2, NORMAL x3, LOW x1} = 8
  ('req-101', 'REQ-2026-0101', 'brand-101', 'dept-004', 'type-001', 'user-requester-001', NULL, NULL, 'PN-T1001', 'TPE Compound TC-100',          'ECO-2026-101', 'SUP-DUP-11', 'DuPont China',           'Urgent qualification of new TPE batch before production launch', 'URGENT', 'SUBMITTED', '2026-06-25', 'Standard courier, 3kg',  NULL, '2026-06-20 08:30:00', NULL,         'user-requester-001', 'user-requester-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-102', 'REQ-2026-0102', 'brand-102', 'dept-005', 'type-002', 'user-requester-001', NULL, NULL, 'PN-T1002', 'Adhesive Tape AT-50',         'ECO-2026-102', 'SUP-3M-12',  '3M China',               'Urgent field complaint: tape failing at 60C',                       'URGENT', 'SUBMITTED', '2026-06-24', 'Hand carry from supplier',NULL, '2026-06-20 08:45:00', NULL,         'user-requester-001', 'user-requester-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-103', 'REQ-2026-0103', 'brand-103', 'dept-006', 'type-003', 'user-requester-001', NULL, NULL, 'PN-T1003', 'Epoxy Potting EP-220',         NULL,           'SUP-HEN-13', 'Henkel Asia',            'High-priority reverse engineering of competitor potting',            'HIGH',   'SUBMITTED', '2026-06-26', NULL,                   NULL, '2026-06-20 09:00:00', NULL,         'user-requester-001', 'user-requester-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-104', 'REQ-2026-0104', 'brand-104', 'dept-004', 'type-001', 'user-103',          NULL, NULL, 'PN-T1004', 'Silicone Rubber SR-40',       'ECO-2026-103', 'SUP-WAK-14', 'Wacker Shanghai',        'High-priority material change for heat-resistant gasket',           'HIGH',   'SUBMITTED', '2026-06-27', 'Cold chain 0-4C',      NULL, '2026-06-20 09:15:00', NULL,         'user-103',          'user-103',          '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-105', 'REQ-2026-0105', 'brand-105', 'dept-005', 'type-002', 'user-requester-001', NULL, NULL, 'PN-T1005', 'Polyurethane Foam PU-12',     'ECO-2026-104', 'SUP-MOM-15', 'Momentive China',        'New supplier qualification, normal timeline',                        'NORMAL', 'SUBMITTED', '2026-06-28', NULL,                   NULL, '2026-06-20 09:30:00', NULL,         'user-requester-001', 'user-requester-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-106', 'REQ-2026-0106', 'brand-101', 'dept-006', 'type-004', 'user-requester-001', NULL, NULL, 'PN-T1006', 'Lubricant Oil LO-5W30',       NULL,           'SUP-DUP-16', 'DuPont China',           'Raw material qualification for new production line',                  'NORMAL', 'SUBMITTED', '2026-06-30', '500ml in amber bottle', NULL, '2026-06-20 09:45:00', NULL,         'user-requester-001', 'user-requester-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-107', 'REQ-2026-0107', 'brand-102', 'dept-004', 'type-001', 'user-103',          NULL, NULL, 'PN-T1007', 'Acrylic Sheet AC-3mm',        'ECO-2026-105', 'SUP-3M-17',  '3M China',               'Routine incoming material check',                                    'NORMAL', 'SUBMITTED', '2026-07-01', NULL,                   NULL, '2026-06-20 10:00:00', NULL,         'user-103',          'user-103',          '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-108', 'REQ-2026-0108', 'brand-103', 'dept-005', 'type-002', 'user-requester-001', NULL, NULL, 'PN-T1008', 'Conformal Coating CC-UV',     NULL,           'SUP-HEN-18', 'Henkel Asia',            'Low-priority exploration of UV-cure coating',                        'LOW',    'SUBMITTED', '2026-07-10', 'Photosensitive, keep dark',NULL,'2026-06-20 10:15:00', NULL,         'user-requester-001', 'user-requester-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- ASSIGNED x {URGENT x1, HIGH x1, NORMAL x2, LOW x1} = 5
  ('req-109', 'REQ-2026-0109', 'brand-104', 'dept-006', 'type-003', 'user-requester-001', NULL, NULL, 'PN-T1009', 'Thermal Pad TP-2W',           'ECO-2026-106', 'SUP-WAK-19', 'Wacker Shanghai',        'Failure analysis on customer-returned thermal pad',                  'URGENT', 'ASSIGNED',  '2026-06-25', 'Returned unit + 5 reference',NULL,'2026-06-19 14:00:00', '2026-06-19 15:00:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-110', 'REQ-2026-0110', 'brand-105', 'dept-004', 'type-001', 'user-requester-001', NULL, NULL, 'PN-T1010', 'FKM Seal Compound',           'ECO-2026-107', 'SUP-MOM-20', 'Momentive China',        'New FKM grade for chemical-resistance application',                  'HIGH',   'ASSIGNED',  '2026-06-26', NULL,                   500.00, '2026-06-19 14:30:00', '2026-06-19 15:30:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-111', 'REQ-2026-0111', 'brand-101', 'dept-005', 'type-002', 'user-103',          NULL, NULL, 'PN-T1011', 'PSA Tape PSA-25',             NULL,           'SUP-DUP-21', 'DuPont China',           'Standard double-sided PSA evaluation',                                'NORMAL', 'ASSIGNED',  '2026-06-29', '5 sample rolls',        800.00, '2026-06-19 15:00:00', '2026-06-19 16:00:00', 'user-103',          'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-112', 'REQ-2026-0112', 'brand-102', 'dept-006', 'type-004', 'user-requester-001', NULL, NULL, 'PN-T1012', 'Alumina Powder AL-99',        'ECO-2026-108', 'SUP-3M-22',  '3M China',               'Raw material qualification for ceramic substrate',                   'NORMAL', 'ASSIGNED',  '2026-07-02', '1kg sealed bag',         1200.00, '2026-06-19 15:30:00', '2026-06-19 16:30:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-113', 'REQ-2026-0113', 'brand-103', 'dept-004', 'type-001', 'user-requester-001', NULL, NULL, 'PN-T1013', 'Phenolic Resin PF-220',        NULL,           'SUP-HEN-23', 'Henkel Asia',            'Low-priority exploration of bio-based phenolic',                     'LOW',    'ASSIGNED',  '2026-07-15', NULL,                   600.00, '2026-06-19 16:00:00', '2026-06-19 17:00:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- SAMPLING x {URGENT x1, HIGH x1, NORMAL x2, LOW x1} = 5
  ('req-114', 'REQ-2026-0114', 'brand-104', 'dept-005', 'type-002', 'user-requester-001', NULL, NULL, 'PN-T1014', 'Gasket Rubber GR-70',         'ECO-2026-109', 'SUP-WAK-24', 'Wacker Shanghai',        'Customer line-down due to gasket swelling; urgent',                  'URGENT', 'SAMPLING',  '2026-06-23', '10 returned gaskets',   900.00, '2026-06-18 09:00:00', '2026-06-18 09:30:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-115', 'REQ-2026-0115', 'brand-105', 'dept-006', 'type-003', 'user-103',          NULL, NULL, 'PN-T1015', 'Cable Jacket CJ-PVC',         'ECO-2026-110', 'SUP-MOM-25', 'Momentive China',        'Field return analysis: jacket cracking',                              'HIGH',   'SAMPLING',  '2026-06-24', '3 cable sections, 30cm',1200.00, '2026-06-18 10:00:00', '2026-06-18 10:30:00', 'user-103',          'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-116', 'REQ-2026-0116', 'brand-101', 'dept-004', 'type-001', 'user-requester-001', NULL, NULL, 'PN-T1016', 'PBT Resin PBT-G30',           'ECO-2026-111', 'SUP-DUP-26', 'DuPont China',           'Glass-fibre filled PBT - new colour, R&D interest',                  'NORMAL', 'SAMPLING',  '2026-06-28', '2kg pellets',           700.00, '2026-06-18 11:00:00', '2026-06-18 11:30:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-117', 'REQ-2026-0117', 'brand-102', 'dept-005', 'type-002', 'user-requester-001', NULL, NULL, 'PN-T1017', 'EMI Gasket EMI-Cu',           NULL,           'SUP-3M-27',  '3M China',               'Conductive gasket qualification for new enclosure',                  'NORMAL', 'SAMPLING',  '2026-07-03', '6 strips, 10cm',         1100.00, '2026-06-18 11:30:00', '2026-06-18 12:00:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-118', 'REQ-2026-0118', 'brand-103', 'dept-006', 'type-004', 'user-requester-001', NULL, NULL, 'PN-T1018', 'Solvent Cleaner SC-IPA',      NULL,           'SUP-HEN-28', 'Henkel Asia',            'Low-priority alternative IPA source exploration',                    'LOW',    'SAMPLING',  '2026-07-15', '4 x 500ml bottles',     400.00, '2026-06-18 12:00:00', '2026-06-18 12:30:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- REPORTING x {URGENT x2, HIGH x1, NORMAL x2, LOW x1} = 6
  ('req-119', 'REQ-2026-0119', 'brand-104', 'dept-004', 'type-001', 'user-requester-001', NULL, NULL, 'PN-T1019', 'Solder Paste SP-Sn63',         'ECO-2026-112', 'SUP-WAK-29', 'Wacker Shanghai',        'Urgent customer escalation: solder joint reliability',               'URGENT', 'REPORTING', '2026-06-21', '1 jar, 250g',           1500.00, '2026-06-15 09:00:00', '2026-06-15 09:30:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-120', 'REQ-2026-0120', 'brand-105', 'dept-005', 'type-002', 'user-103',          'user-admin-001', 'Li Ming', 'PN-T1020', 'Bearing Steel BS-SKF',     'ECO-2026-113', 'SUP-MOM-30', 'Momentive China',        'Proxy request from procurement; urgent lead-time',                   'URGENT', 'REPORTING', '2026-06-22', '10 bearings, in oil',   2200.00, '2026-06-15 10:00:00', '2026-06-15 10:30:00', 'user-103',          'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-121', 'REQ-2026-0121', 'brand-101', 'dept-006', 'type-003', 'user-requester-001', NULL, NULL, 'PN-T1021', 'Carbon Black CB-N550',        NULL,           'SUP-DUP-31', 'DuPont China',           'Reverse engineering competitor rubber compound',                     'HIGH',   'REPORTING', '2026-06-25', '500g powder',            1300.00, '2026-06-15 11:00:00', '2026-06-15 11:30:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-122', 'REQ-2026-0122', 'brand-102', 'dept-004', 'type-001', 'user-requester-001', NULL, NULL, 'PN-T1022', 'PVDF Coating PV-30',          'ECO-2026-114', 'SUP-3M-32',  '3M China',               'PVDF coating durability study for outdoor enclosures',               'NORMAL', 'REPORTING', '2026-06-28', '5 coated panels',       1000.00, '2026-06-15 12:00:00', '2026-06-15 12:30:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-123', 'REQ-2026-0123', 'brand-103', 'dept-005', 'type-002', 'user-requester-001', NULL, NULL, 'PN-T1023', 'Ceramic Fiber CF-1260',       NULL,           'SUP-HEN-33', 'Henkel Asia',            'Thermal insulation blanket evaluation',                              'NORMAL', 'REPORTING', '2026-06-30', '4 squares 30x30cm',     850.00,  '2026-06-15 13:00:00', '2026-06-15 13:30:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-124', 'REQ-2026-0124', 'brand-104', 'dept-006', 'type-004', 'user-requester-001', NULL, NULL, 'PN-T1024', 'Glass Bead GB-200',           NULL,           'SUP-WAK-34', 'Wacker Shanghai',        'Low-priority survey of bead-blasting media options',                  'LOW',    'REPORTING', '2026-07-20', '2kg beads',             450.00,  '2026-06-15 14:00:00', '2026-06-15 14:30:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- APPROVING x {URGENT x1, HIGH x1, NORMAL x1, LOW x1} = 4
  ('req-125', 'REQ-2026-0125', 'brand-105', 'dept-004', 'type-001', 'user-requester-001', NULL, NULL, 'PN-T1025', 'Zinc Plating Soln ZN-15',     'ECO-2026-115', 'SUP-MOM-35', 'Momentive China',        'Urgent plater chemistry change approval needed',                     'URGENT', 'APPROVING', '2026-06-21', '5L solution',            1600.00, '2026-06-12 09:00:00', '2026-06-12 09:30:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-126', 'REQ-2026-0126', 'brand-101', 'dept-005', 'type-002', 'user-requester-001', NULL, NULL, 'PN-T1026', 'Polycarbonate PC-Lexan',      'ECO-2026-116', 'SUP-DUP-36', 'DuPont China',           'Polycarbonate grade change requires QA sign-off',                    'HIGH',   'APPROVING', '2026-06-22', '2 panels 1m x 1m',      1400.00, '2026-06-12 10:00:00', '2026-06-12 10:30:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-127', 'REQ-2026-0127', 'brand-102', 'dept-006', 'type-003', 'user-103',          NULL, NULL, 'PN-T1027', 'Adhesive Failure Sample',     NULL,           'SUP-3M-37',  '3M China',               'Standard failure analysis, awaiting manager approval',               'NORMAL', 'APPROVING', '2026-06-23', 'Failed bonded part + ref',1100.00, '2026-06-12 11:00:00', '2026-06-12 11:30:00', 'user-103',          'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-128', 'REQ-2026-0128', 'brand-103', 'dept-004', 'type-001', 'user-requester-001', NULL, NULL, 'PN-T1028', 'Tin-Bismuth Solder TB-42',    NULL,           'SUP-HEN-38', 'Henkel Asia',            'Low-priority lead-free solder option survey',                        'LOW',    'APPROVING', '2026-07-10', '100g solder wire',      500.00,  '2026-06-12 12:00:00', '2026-06-12 12:30:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- COMPLETED x {URGENT x2, HIGH x3, NORMAL x4, LOW x1} = 10
  ('req-129', 'REQ-2026-0129', 'brand-104', 'dept-005', 'type-002', 'user-requester-001', NULL, NULL, 'PN-T1029', 'Nylon 66 FR Compound',         'ECO-2026-117', 'SUP-WAK-39', 'Wacker Shanghai',        'Urgent FR compound qualification done; report approved',              'URGENT', 'COMPLETED', '2026-06-15', '10kg bag',              2400.00, '2026-06-08 09:00:00', '2026-06-08 09:30:00', 'user-requester-001', 'user-manager-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-130', 'REQ-2026-0130', 'brand-105', 'dept-006', 'type-003', 'user-103',          NULL, NULL, 'PN-T1030', 'Injection Mould Failure',       'ECO-2026-118', 'SUP-MOM-40', 'Momentive China',        'Urgent mould failure root-cause, closed',                            'URGENT', 'COMPLETED', '2026-06-14', 'Failed mould insert',   1800.00, '2026-06-07 09:00:00', '2026-06-07 09:30:00', 'user-103',          'user-manager-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-131', 'REQ-2026-0131', 'brand-101', 'dept-004', 'type-001', 'user-requester-001', NULL, NULL, 'PN-T1031', 'Epoxy Moulding EM-1000',       'ECO-2026-119', 'SUP-DUP-41', 'DuPont China',           'High-priority epoxy moulding comparison vs. incumbent',              'HIGH',   'COMPLETED', '2026-06-16', '2 moulded samples',     1100.00, '2026-06-08 10:00:00', '2026-06-08 10:30:00', 'user-requester-001', 'user-manager-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-132', 'REQ-2026-0132', 'brand-102', 'dept-005', 'type-002', 'user-requester-001', NULL, NULL, 'PN-T1032', 'Sintered Bronze BZ-Sint',     'ECO-2026-120', 'SUP-3M-42',  '3M China',               'High-priority sintered bronze bushing evaluation',                   'HIGH',   'COMPLETED', '2026-06-17', '10 bushings',           1300.00, '2026-06-08 11:00:00', '2026-06-08 11:30:00', 'user-requester-001', 'user-manager-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-133', 'REQ-2026-0133', 'brand-103', 'dept-006', 'type-004', 'user-requester-001', NULL, NULL, 'PN-T1033', 'Cleaning Agent CA-Citrus',    'ECO-2026-121', 'SUP-HEN-43', 'Henkel Asia',            'High-priority citrus-based cleaner qualification',                   'HIGH',   'COMPLETED', '2026-06-18', '5L container',          900.00,  '2026-06-08 12:00:00', '2026-06-08 12:30:00', 'user-requester-001', 'user-manager-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-134', 'REQ-2026-0134', 'brand-104', 'dept-004', 'type-001', 'user-103',          NULL, NULL, 'PN-T1034', 'TPU Film TPU-100',            'ECO-2026-122', 'SUP-WAK-44', 'Wacker Shanghai',        'Standard TPU film comparison',                                       'NORMAL', 'COMPLETED', '2026-06-13', '5 film samples A4',     700.00,  '2026-06-05 09:00:00', '2026-06-05 09:30:00', 'user-103',          'user-manager-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-135', 'REQ-2026-0135', 'brand-105', 'dept-005', 'type-002', 'user-requester-001', NULL, NULL, 'PN-T1035', 'Hard Anodised Al Plate',     'ECO-2026-123', 'SUP-MOM-45', 'Momentive China',        'Standard hard anodising thickness check',                            'NORMAL', 'COMPLETED', '2026-06-14', '3 plates 10x10cm',      600.00,  '2026-06-05 10:00:00', '2026-06-05 10:30:00', 'user-requester-001', 'user-manager-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-136', 'REQ-2026-0136', 'brand-101', 'dept-006', 'type-003', 'user-requester-001', NULL, NULL, 'PN-T1036', 'Cracked Plastic Housing',     'ECO-2026-124', 'SUP-DUP-46', 'DuPont China',           'Standard failure analysis on customer return',                       'NORMAL', 'COMPLETED', '2026-06-15', 'Failed + 3 reference',  1200.00, '2026-06-05 11:00:00', '2026-06-05 11:30:00', 'user-requester-001', 'user-manager-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-137', 'REQ-2026-0137', 'brand-102', 'dept-004', 'type-001', 'user-103',          NULL, NULL, 'PN-T1037', 'Polyolefin Hot-Melt Adh',    'ECO-2026-125', 'SUP-3M-47',  '3M China',               'Standard hot-melt adhesive screening',                                'NORMAL', 'COMPLETED', '2026-06-16', '5 sticks',              500.00,  '2026-06-05 12:00:00', '2026-06-05 12:30:00', 'user-103',          'user-manager-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-138', 'REQ-2026-0138', 'brand-103', 'dept-005', 'type-002', 'user-requester-001', NULL, NULL, 'PN-T1038', 'Low-Smoke Halogen-Free Cable',NULL,           'SUP-HEN-48', 'Henkel Asia',            'Low-priority LSZH cable exploration',                                 'LOW',    'COMPLETED', '2026-06-20', '3 cable samples, 1m',   400.00,  '2026-06-05 13:00:00', '2026-06-05 13:30:00', 'user-requester-001', 'user-manager-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- REJECTED x {URGENT x1, NORMAL x1} = 2
  ('req-139', 'REQ-2026-0139', 'brand-104', 'dept-006', 'type-003', 'user-103',          NULL, NULL, 'PN-T1039', 'Mystery Black Powder',         NULL,           'SUP-WAK-49', 'Wacker Shanghai',        'Unidentified powder — out of scope, rejected',                        'URGENT', 'REJECTED',  '2026-06-19', 'Sealed bag, 50g',       NULL,    '2026-06-13 09:00:00', '2026-06-13 09:30:00', 'user-103',          'user-manager-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('req-140', 'REQ-2026-0140', 'brand-105', 'dept-004', 'type-001', 'user-requester-001', NULL, NULL, 'PN-T1040', 'Outsourced Calibration',      'ECO-2026-126', 'SUP-MOM-50', 'Momentive China',        'Calibration should be done in-house, request rejected',              'NORMAL', 'REJECTED',  '2026-06-18', NULL,                   NULL,    '2026-06-13 10:00:00', '2026-06-13 10:30:00', 'user-requester-001', 'user-manager-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 6. Analysis tasks (~70 rows) — 1-3 per in-flight/completed request
-- =============================================
-- Status mix: 10 PENDING, 11 IN_PROGRESS, 39 COMPLETED, 3 DELAYED = 63 rows
-- IDs task-101..task-163

INSERT INTO analysis_task (id, request_id, item_id, assignee_id, status, delay_reason, started_at, completed_at, sort_order, created_by, updated_by, created_at, updated_at) VALUES
  -- ASSIGNED reqs (req-109..113): 1 PENDING each = 5
  ('task-101', 'req-109', 'item-101', 'user-engineer-001', 'PENDING',     NULL, NULL, NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-102', 'req-110', 'item-102', 'user-engineer-001', 'PENDING',     NULL, NULL, NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-103', 'req-111', 'item-103', 'user-tech-001',      'PENDING',     NULL, NULL, NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-104', 'req-112', 'item-104', 'user-engineer-001', 'PENDING',     NULL, NULL, NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-105', 'req-113', 'item-105', 'user-tech-001',      'PENDING',     NULL, NULL, NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- SAMPLING reqs (req-114..118): 1 PENDING + 1 IN_PROGRESS each = 5 PENDING + 5 IN_PROGRESS
  ('task-106', 'req-114', 'item-101', 'user-engineer-001', 'PENDING',     NULL, NULL, NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-107', 'req-114', 'item-104', 'user-tech-001',      'IN_PROGRESS', NULL, '2026-06-19 09:00:00', NULL, 2, 'user-engineer-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-108', 'req-115', 'item-102', 'user-engineer-001', 'PENDING',     NULL, NULL, NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-109', 'req-115', 'item-105', 'user-tech-001',      'IN_PROGRESS', NULL, '2026-06-19 10:00:00', NULL, 2, 'user-engineer-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-110', 'req-116', 'item-103', 'user-engineer-001', 'PENDING',     NULL, NULL, NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-111', 'req-116', 'item-106', 'user-tech-001',      'IN_PROGRESS', NULL, '2026-06-19 11:00:00', NULL, 2, 'user-engineer-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-112', 'req-117', 'item-101', 'user-engineer-001', 'PENDING',     NULL, NULL, NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-113', 'req-117', 'item-104', 'user-tech-001',      'IN_PROGRESS', NULL, '2026-06-19 12:00:00', NULL, 2, 'user-engineer-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-114', 'req-118', 'item-102', 'user-engineer-001', 'PENDING',     NULL, NULL, NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-115', 'req-118', 'item-105', 'user-tech-001',      'IN_PROGRESS', NULL, '2026-06-19 13:00:00', NULL, 2, 'user-engineer-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- REPORTING reqs (req-119..124): 1 IN_PROGRESS + 1 COMPLETED each = 6 IN_PROGRESS + 6 COMPLETED
  ('task-116', 'req-119', 'item-101', 'user-engineer-001', 'IN_PROGRESS', NULL, '2026-06-19 08:00:00', NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-117', 'req-119', 'item-104', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-18 08:00:00', '2026-06-19 16:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-118', 'req-120', 'item-102', 'user-engineer-001', 'IN_PROGRESS', NULL, '2026-06-19 09:00:00', NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-119', 'req-120', 'item-105', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-18 09:00:00', '2026-06-19 17:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-120', 'req-121', 'item-103', 'user-engineer-001', 'IN_PROGRESS', NULL, '2026-06-19 10:00:00', NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-121', 'req-121', 'item-106', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-18 10:00:00', '2026-06-19 18:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-122', 'req-122', 'item-101', 'user-engineer-001', 'IN_PROGRESS', NULL, '2026-06-19 11:00:00', NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-123', 'req-122', 'item-104', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-18 11:00:00', '2026-06-19 19:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-124', 'req-123', 'item-102', 'user-engineer-001', 'IN_PROGRESS', NULL, '2026-06-19 12:00:00', NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-125', 'req-123', 'item-105', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-18 12:00:00', '2026-06-19 20:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-126', 'req-124', 'item-103', 'user-engineer-001', 'IN_PROGRESS', NULL, '2026-06-19 13:00:00', NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-127', 'req-124', 'item-106', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-18 13:00:00', '2026-06-19 21:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- APPROVING reqs (req-125..128): 2 COMPLETED each = 8 COMPLETED
  ('task-128', 'req-125', 'item-101', 'user-engineer-001', 'COMPLETED',   NULL, '2026-06-15 08:00:00', '2026-06-17 16:00:00', 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-129', 'req-125', 'item-104', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-15 09:00:00', '2026-06-17 17:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-130', 'req-126', 'item-102', 'user-engineer-001', 'COMPLETED',   NULL, '2026-06-15 10:00:00', '2026-06-17 18:00:00', 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-131', 'req-126', 'item-105', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-15 11:00:00', '2026-06-17 19:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-132', 'req-127', 'item-103', 'user-engineer-001', 'COMPLETED',   NULL, '2026-06-15 12:00:00', '2026-06-17 20:00:00', 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-133', 'req-127', 'item-106', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-15 13:00:00', '2026-06-17 21:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-134', 'req-128', 'item-101', 'user-engineer-001', 'COMPLETED',   NULL, '2026-06-15 14:00:00', '2026-06-17 22:00:00', 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-135', 'req-128', 'item-104', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-15 15:00:00', '2026-06-17 23:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- COMPLETED reqs (req-129..138): 2-3 COMPLETED each, some DELAYED history = 25 COMPLETED + 3 DELAYED
  ('task-136', 'req-129', 'item-101', 'user-engineer-001', 'COMPLETED',   NULL, '2026-06-10 08:00:00', '2026-06-12 16:00:00', 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-137', 'req-129', 'item-104', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-10 09:00:00', '2026-06-12 17:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-138', 'req-130', 'item-102', 'user-engineer-001', 'DELAYED',     'TGA furnace E-04 fault paused run 24h', '2026-06-09 08:00:00', NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-139', 'req-130', 'item-105', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-09 09:00:00', '2026-06-12 18:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-140', 'req-131', 'item-103', 'user-engineer-001', 'COMPLETED',   NULL, '2026-06-10 10:00:00', '2026-06-12 19:00:00', 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-141', 'req-131', 'item-106', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-10 11:00:00', '2026-06-12 20:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-142', 'req-132', 'item-101', 'user-engineer-001', 'COMPLETED',   NULL, '2026-06-10 12:00:00', '2026-06-13 10:00:00', 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-143', 'req-132', 'item-104', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-10 13:00:00', '2026-06-13 11:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-144', 'req-133', 'item-102', 'user-engineer-001', 'DELAYED',     'Awaiting consumable shipment from Henkel', '2026-06-10 14:00:00', NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-145', 'req-133', 'item-105', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-10 15:00:00', '2026-06-13 12:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-146', 'req-134', 'item-103', 'user-engineer-001', 'COMPLETED',   NULL, '2026-06-08 08:00:00', '2026-06-10 16:00:00', 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-147', 'req-134', 'item-106', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-08 09:00:00', '2026-06-10 17:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-148', 'req-135', 'item-101', 'user-engineer-001', 'COMPLETED',   NULL, '2026-06-08 10:00:00', '2026-06-10 18:00:00', 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-149', 'req-135', 'item-104', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-08 11:00:00', '2026-06-10 19:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-150', 'req-136', 'item-102', 'user-engineer-001', 'COMPLETED',   NULL, '2026-06-08 12:00:00', '2026-06-10 20:00:00', 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-151', 'req-136', 'item-105', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-08 13:00:00', '2026-06-10 21:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-152', 'req-137', 'item-103', 'user-engineer-001', 'COMPLETED',   NULL, '2026-06-08 14:00:00', '2026-06-10 22:00:00', 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-153', 'req-137', 'item-106', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-08 15:00:00', '2026-06-10 23:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-154', 'req-138', 'item-101', 'user-engineer-001', 'DELAYED',     'LSZH cable not in stock, awaiting arrival', '2026-06-08 16:00:00', NULL, 1, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-155', 'req-138', 'item-104', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-08 17:00:00', '2026-06-11 09:00:00', 2, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-156', 'req-130', 'item-106', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-11 10:00:00', '2026-06-12 16:00:00', 3, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-157', 'req-131', 'item-101', 'user-engineer-001', 'COMPLETED',   NULL, '2026-06-11 11:00:00', '2026-06-12 17:00:00', 3, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-158', 'req-132', 'item-105', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-11 12:00:00', '2026-06-13 12:00:00', 3, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-159', 'req-133', 'item-106', 'user-tech-001',      'COMPLETED',   NULL, '2026-06-11 13:00:00', '2026-06-13 13:00:00', 3, 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-160', 'req-135', 'item-101', 'user-engineer-001', 'COMPLETED',   NULL, '2026-06-09 08:00:00', '2026-06-10 16:00:00', 3, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-161', 'req-137', 'item-101', 'user-engineer-001', 'COMPLETED',   NULL, '2026-06-09 09:00:00', '2026-06-10 17:00:00', 3, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-162', 'req-138', 'item-101', 'user-engineer-001', 'COMPLETED',   NULL, '2026-06-09 10:00:00', '2026-06-11 10:00:00', 3, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('task-163', 'req-130', 'item-103', 'user-engineer-001', 'COMPLETED',   NULL, '2026-06-11 11:00:00', '2026-06-12 18:00:00', 4, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 7. Samples (35 rows) — 1-2 per in-flight/completed request
-- =============================================
-- Status mix: 10 PENDING, 8 PREPARING, 17 READY
-- IDs sample-101..sample-135

INSERT INTO sample (id, request_id, received_by, received_at, preparation_status, preparation_detail, completed_at, created_by, updated_by, created_at, updated_at) VALUES
  -- SAMPLING (5 reqs) — PENDING + PREPARING mix
  ('sample-101', 'req-114', 'user-tech-001', '2026-06-19 09:00:00', 'PREPARING', 'Sectioning 10 gaskets into 25x10mm coupons', NULL,         'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-102', 'req-114', 'user-tech-001', '2026-06-19 09:00:00', 'PENDING',   NULL,                                            NULL,         'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-103', 'req-115', 'user-tech-001', '2026-06-19 10:00:00', 'PREPARING', 'Striping jacket, inspecting copper conductor',   NULL,         'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-104', 'req-115', 'user-tech-001', '2026-06-19 10:00:00', 'PENDING',   NULL,                                            NULL,         'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-105', 'req-116', 'user-tech-001', '2026-06-19 11:00:00', 'PREPARING', 'Drying pellets at 80C for 4h before DSC',         NULL,         'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-106', 'req-116', 'user-tech-001', '2026-06-19 11:00:00', 'PENDING',   NULL,                                            NULL,         'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-107', 'req-117', 'user-tech-001', '2026-06-19 12:00:00', 'PREPARING', 'Cutting 6 strips to 10x50mm',                    NULL,         'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-108', 'req-117', 'user-tech-001', '2026-06-19 12:00:00', 'PENDING',   NULL,                                            NULL,         'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-109', 'req-118', 'user-tech-001', '2026-06-19 13:00:00', 'PREPARING', 'Aliquoting 4x500ml into labelled HDPE bottles',  NULL,         'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-110', 'req-118', 'user-tech-001', '2026-06-19 13:00:00', 'PENDING',   NULL,                                            NULL,         'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- REPORTING (6 reqs) — mostly READY
  ('sample-111', 'req-119', 'user-tech-001', '2026-06-16 09:00:00', 'READY',     'Weighed 1g into 3 crucibles for TGA',           '2026-06-16 11:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-112', 'req-119', 'user-tech-001', '2026-06-16 09:00:00', 'READY',     'Retained 50g for archive',                       '2026-06-16 11:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-113', 'req-120', 'user-tech-001', '2026-06-16 10:00:00', 'READY',     'Bearings ultrasonically cleaned and dried',       '2026-06-16 12:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-114', 'req-120', 'user-tech-001', '2026-06-16 10:00:00', 'PREPARING', 'Cross-sectioning bearing #3',                    NULL,         'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-115', 'req-121', 'user-tech-001', '2026-06-16 11:00:00', 'READY',     'Carbon black dried in oven at 105C for 2h',      '2026-06-16 13:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-116', 'req-122', 'user-tech-001', '2026-06-16 12:00:00', 'READY',     'Coated panels wiped with IPA, edge-sealed',       '2026-06-16 14:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-117', 'req-123', 'user-tech-001', '2026-06-16 13:00:00', 'READY',     'Fiber squares conditioned at 23C/50%RH for 24h',  '2026-06-16 15:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-118', 'req-124', 'user-tech-001', '2026-06-16 14:00:00', 'READY',     'Beads sieved to 200-250um fraction',             '2026-06-16 16:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- APPROVING (4 reqs) — all READY
  ('sample-119', 'req-125', 'user-tech-001', '2026-06-14 09:00:00', 'READY',     'Solution diluted 1:10 for ICP',                  '2026-06-14 11:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-120', 'req-125', 'user-tech-001', '2026-06-14 09:00:00', 'READY',     'Retained 500ml archive sample',                  '2026-06-14 11:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-121', 'req-126', 'user-tech-001', '2026-06-14 10:00:00', 'READY',     'Panels cut to 100x100mm test specimens',          '2026-06-14 12:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-122', 'req-127', 'user-tech-001', '2026-06-14 11:00:00', 'READY',     'Failed joint sectioned to 5mm thickness',        '2026-06-14 13:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-123', 'req-127', 'user-tech-001', '2026-06-14 11:00:00', 'READY',     'Reference joint archived',                       '2026-06-14 13:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-124', 'req-128', 'user-tech-001', '2026-06-14 12:00:00', 'READY',     'Solder wire coiled into 5 test loops',           '2026-06-14 14:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- COMPLETED (10 reqs) — all READY
  ('sample-125', 'req-129', 'user-tech-001', '2026-06-10 09:00:00', 'READY',     'Pellets dried at 80C/4h, archived 500g',         '2026-06-10 11:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-126', 'req-130', 'user-tech-001', '2026-06-09 09:00:00', 'READY',     'Failed mould cross-sectioned',                   '2026-06-09 11:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-127', 'req-131', 'user-tech-001', '2026-06-10 10:00:00', 'READY',     '2 moulded samples conditioned 24h',              '2026-06-10 12:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-128', 'req-132', 'user-tech-001', '2026-06-10 11:00:00', 'READY',     'Bushings degreased, weighed',                    '2026-06-10 13:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-129', 'req-133', 'user-tech-001', '2026-06-10 12:00:00', 'READY',     'Cleaning agent diluted to working concentration','2026-06-10 14:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-130', 'req-134', 'user-tech-001', '2026-06-08 09:00:00', 'READY',     'Film samples cut and labelled A-E',              '2026-06-08 11:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-131', 'req-135', 'user-tech-001', '2026-06-08 10:00:00', 'READY',     'Plates cleaned and weighed',                     '2026-06-08 12:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-132', 'req-136', 'user-tech-001', '2026-06-08 11:00:00', 'READY',     'Failed housing photographed before sectioning',  '2026-06-08 13:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-133', 'req-137', 'user-tech-001', '2026-06-08 12:00:00', 'READY',     '5 hot-melt sticks conditioned 4h',               '2026-06-08 14:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-134', 'req-138', 'user-tech-001', '2026-06-08 13:00:00', 'READY',     'Cable samples cut and stripped',                 '2026-06-08 15:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('sample-135', 'req-129', 'user-tech-001', '2026-06-10 09:00:00', 'READY',     'Retained 2kg as long-term archive',              '2026-06-10 11:00:00', 'user-tech-001', 'user-tech-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 8. Reports (16 rows) — 4 DRAFT, 5 IN_REVIEW, 4 APPROVED, 2 REVISING, 1 (no REJECTED in enum)
-- =============================================
-- IDs rpt-101..rpt-116
-- REJECTED was in spec but not in schema CHECK constraint; using REVISING instead

INSERT INTO report (id, request_id, task_id, author_id, version_number, revision_note, status, file_url, pdf_url, approved_by, approved_at, submitted_at, created_by, updated_by, created_at, updated_at) VALUES
  -- DRAFT (4) — being written
  ('rpt-101', 'req-119', 'task-116', 'user-engineer-001', 'V1.0', 'Initial draft, TGA section incomplete',                  'DRAFT',     '/reports/rpt-101/V1.0.docx', NULL,                    NULL,             NULL,         NULL,         'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('rpt-102', 'req-120', 'task-118', 'user-engineer-001', 'V1.0', 'Draft - awaiting bearing hardness data',                 'DRAFT',     '/reports/rpt-102/V1.0.docx', NULL,                    NULL,             NULL,         NULL,         'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('rpt-103', 'req-121', 'task-120', 'user-engineer-001', 'V1.0', 'Draft - reverse engineering analysis in progress',      'DRAFT',     '/reports/rpt-103/V1.0.docx', NULL,                    NULL,             NULL,         NULL,         'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('rpt-104', 'req-122', 'task-122', 'user-engineer-001', 'V1.0', 'Draft - PVDF outdoor exposure matrix',                  'DRAFT',     '/reports/rpt-104/V1.0.docx', NULL,                    NULL,             NULL,         NULL,         'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- IN_REVIEW (5)
  ('rpt-105', 'req-123', 'task-124', 'user-engineer-001', 'V1.0', 'Submitted to QC for review',                            'IN_REVIEW', '/reports/rpt-105/V1.0.docx', '/reports/rpt-105/V1.0.pdf', NULL,          NULL,         '2026-06-19 17:00:00', 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('rpt-106', 'req-124', 'task-126', 'user-engineer-001', 'V1.0', 'Submitted to QC, awaiting first-pass comments',         'IN_REVIEW', '/reports/rpt-106/V1.0.docx', '/reports/rpt-106/V1.0.pdf', NULL,          NULL,         '2026-06-19 18:00:00', 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('rpt-107', 'req-125', 'task-128', 'user-engineer-001', 'V1.0', 'Plating chemistry report, in QC review',                'IN_REVIEW', '/reports/rpt-107/V1.0.docx', '/reports/rpt-107/V1.0.pdf', NULL,          NULL,         '2026-06-19 19:00:00', 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('rpt-108', 'req-126', 'task-130', 'user-engineer-001', 'V1.0', 'Polycarbonate grade change report under review',        'IN_REVIEW', '/reports/rpt-108/V1.0.docx', '/reports/rpt-108/V1.0.pdf', NULL,          NULL,         '2026-06-19 20:00:00', 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('rpt-109', 'req-127', 'task-132', 'user-engineer-001', 'V1.0', 'Adhesive failure analysis, QC reviewing',               'IN_REVIEW', '/reports/rpt-109/V1.0.docx', '/reports/rpt-109/V1.0.pdf', NULL,          NULL,         '2026-06-19 21:00:00', 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- APPROVED (4) — with revision history
  ('rpt-110', 'req-129', 'task-136', 'user-engineer-001', 'V1.1', 'Approved with final FR compound data table',            'APPROVED',  '/reports/rpt-110/V1.1.docx', '/reports/rpt-110/V1.1.pdf', 'user-manager-001', '2026-06-13 09:00:00', '2026-06-12 17:00:00', 'user-engineer-001', 'user-manager-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('rpt-111', 'req-130', 'task-139', 'user-engineer-001', 'V1.1', 'Approved with mould failure root cause',                'APPROVED',  '/reports/rpt-111/V1.1.docx', '/reports/rpt-111/V1.1.pdf', 'user-manager-001', '2026-06-13 10:00:00', '2026-06-12 18:00:00', 'user-engineer-001', 'user-manager-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('rpt-112', 'req-131', 'task-140', 'user-engineer-001', 'V1.1', 'Approved epoxy moulding comparison finalised',          'APPROVED',  '/reports/rpt-112/V1.1.docx', '/reports/rpt-112/V1.1.pdf', 'user-manager-001', '2026-06-13 11:00:00', '2026-06-12 19:00:00', 'user-engineer-001', 'user-manager-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('rpt-113', 'req-132', 'task-142', 'user-engineer-001', 'V1.1', 'Approved sintered bronze bushing report',               'APPROVED',  '/reports/rpt-113/V1.1.docx', '/reports/rpt-113/V1.1.pdf', 'user-manager-001', '2026-06-13 12:00:00', '2026-06-12 20:00:00', 'user-engineer-001', 'user-manager-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- REVISING (2) — multi-revision history
  ('rpt-114', 'req-133', 'task-144', 'user-engineer-001', 'V1.2', 'In revision per QC V1.1 comments, supplier data refresh','REVISING',  '/reports/rpt-114/V1.2.docx', '/reports/rpt-114/V1.2.pdf', NULL,          NULL,         NULL,         'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  ('rpt-115', 'req-134', 'task-146', 'user-engineer-001', 'V1.2', 'In revision: clarifying TPU vs PVC section',            'REVISING',  '/reports/rpt-115/V1.2.docx', '/reports/rpt-115/V1.2.pdf', NULL,          NULL,         NULL,         'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00'),

  -- One extra IN_REVIEW to reach 16
  ('rpt-116', 'req-135', 'task-148', 'user-engineer-001', 'V1.0', 'Hard anodised plate report under QC review',            'IN_REVIEW', '/reports/rpt-116/V1.0.docx', '/reports/rpt-116/V1.0.pdf', NULL,          NULL,         '2026-06-19 22:00:00', 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 9. Report revisions (28 rows) — 3 per APPROVED, 3 per REVISING
-- =============================================
-- IDs rev-101..rev-128

INSERT INTO report_revision (id, report_id, version_number, revision_note, file_url, pdf_url, archived_by, archived_at) VALUES
  -- rpt-110 (APPROVED): V1.0 -> V1.1
  ('rev-101', 'rpt-110', 'V1.0', 'Initial draft - FR compound screening',                 '/reports/rpt-110/V1.0.docx', '/reports/rpt-110/V1.0.pdf', 'user-engineer-001', '2026-06-12 14:00:00'),
  ('rev-102', 'rpt-110', 'V1.1', 'Added LOI values and FR mechanism discussion',          '/reports/rpt-110/V1.1.docx', '/reports/rpt-110/V1.1.pdf', 'user-engineer-001', '2026-06-12 16:30:00'),
  ('rev-103', 'rpt-110', 'V1.1', 'Final approved - manager signed off',                  '/reports/rpt-110/V1.1.docx', '/reports/rpt-110/V1.1.pdf', 'user-manager-001',  '2026-06-13 09:00:00'),

  -- rpt-111 (APPROVED): V1.0 -> V1.1
  ('rev-104', 'rpt-111', 'V1.0', 'Initial draft - mould failure observations',           '/reports/rpt-111/V1.0.docx', '/reports/rpt-111/V1.0.pdf', 'user-engineer-001', '2026-06-12 15:00:00'),
  ('rev-105', 'rpt-111', 'V1.1', 'Added SEM images of fracture surface',                 '/reports/rpt-111/V1.1.docx', '/reports/rpt-111/V1.1.pdf', 'user-engineer-001', '2026-06-12 17:30:00'),
  ('rev-106', 'rpt-111', 'V1.1', 'Final approved with corrective action recommendations', '/reports/rpt-111/V1.1.docx', '/reports/rpt-111/V1.1.pdf', 'user-manager-001',  '2026-06-13 10:00:00'),

  -- rpt-112 (APPROVED): V1.0 -> V1.1
  ('rev-107', 'rpt-112', 'V1.0', 'Initial epoxy moulding comparison',                    '/reports/rpt-112/V1.0.docx', '/reports/rpt-112/V1.0.pdf', 'user-engineer-001', '2026-06-12 16:00:00'),
  ('rev-108', 'rpt-112', 'V1.1', 'Added DMA curves and Tg comparison table',             '/reports/rpt-112/V1.1.docx', '/reports/rpt-112/V1.1.pdf', 'user-engineer-001', '2026-06-12 18:30:00'),
  ('rev-109', 'rpt-112', 'V1.1', 'Final approved',                                       '/reports/rpt-112/V1.1.docx', '/reports/rpt-112/V1.1.pdf', 'user-manager-001',  '2026-06-13 11:00:00'),

  -- rpt-113 (APPROVED): V1.0 -> V1.1
  ('rev-110', 'rpt-113', 'V1.0', 'Initial sintered bronze draft',                        '/reports/rpt-113/V1.0.docx', '/reports/rpt-113/V1.0.pdf', 'user-engineer-001', '2026-06-12 17:00:00'),
  ('rev-111', 'rpt-113', 'V1.1', 'Added wear-rate and hardness data',                    '/reports/rpt-113/V1.1.docx', '/reports/rpt-113/V1.1.pdf', 'user-engineer-001', '2026-06-12 19:30:00'),
  ('rev-112', 'rpt-113', 'V1.1', 'Final approved',                                       '/reports/rpt-113/V1.1.docx', '/reports/rpt-113/V1.1.pdf', 'user-manager-001',  '2026-06-13 12:00:00'),

  -- rpt-114 (REVISING): V1.0 -> V1.1 -> V1.2
  ('rev-113', 'rpt-114', 'V1.0', 'Initial cleaner evaluation',                           '/reports/rpt-114/V1.0.docx', '/reports/rpt-114/V1.0.pdf', 'user-engineer-001', '2026-06-12 18:00:00'),
  ('rev-114', 'rpt-114', 'V1.1', 'QC V1.0 round 1: need supplier MSDS appendix',         '/reports/rpt-114/V1.1.docx', '/reports/rpt-114/V1.1.pdf', 'user-engineer-001', '2026-06-13 10:00:00'),
  ('rev-115', 'rpt-114', 'V1.2', 'V1.1 addressed, now QC reviewing with new comments',   '/reports/rpt-114/V1.2.docx', '/reports/rpt-114/V1.2.pdf', 'user-engineer-001', '2026-06-14 09:00:00'),

  -- rpt-115 (REVISING): V1.0 -> V1.1 -> V1.2
  ('rev-116', 'rpt-115', 'V1.0', 'Initial TPU film comparison',                          '/reports/rpt-115/V1.0.docx', '/reports/rpt-115/V1.0.pdf', 'user-engineer-001', '2026-06-12 19:00:00'),
  ('rev-117', 'rpt-115', 'V1.1', 'QC V1.0 round 1: clarify TPU vs PVC section',         '/reports/rpt-115/V1.1.docx', '/reports/rpt-115/V1.1.pdf', 'user-engineer-001', '2026-06-13 11:00:00'),
  ('rev-118', 'rpt-115', 'V1.2', 'V1.1 updated, still pending QC round 2',               '/reports/rpt-115/V1.2.docx', '/reports/rpt-115/V1.2.pdf', 'user-engineer-001', '2026-06-14 10:00:00'),

  -- IN_REVIEW reports: one prior version each
  ('rev-119', 'rpt-105', 'V1.0', 'Initial draft - ceramic fiber evaluation',             '/reports/rpt-105/V1.0.docx', '/reports/rpt-105/V1.0.pdf', 'user-engineer-001', '2026-06-19 17:00:00'),
  ('rev-120', 'rpt-106', 'V1.0', 'Initial draft - glass bead blasting',                 '/reports/rpt-106/V1.0.docx', '/reports/rpt-106/V1.0.pdf', 'user-engineer-001', '2026-06-19 18:00:00'),
  ('rev-121', 'rpt-107', 'V1.0', 'Initial draft - zinc plating chemistry',               '/reports/rpt-107/V1.0.docx', '/reports/rpt-107/V1.0.pdf', 'user-engineer-001', '2026-06-19 19:00:00'),
  ('rev-122', 'rpt-108', 'V1.0', 'Initial draft - polycarbonate grade',                  '/reports/rpt-108/V1.0.docx', '/reports/rpt-108/V1.0.pdf', 'user-engineer-001', '2026-06-19 20:00:00'),
  ('rev-123', 'rpt-109', 'V1.0', 'Initial draft - adhesive failure',                     '/reports/rpt-109/V1.0.docx', '/reports/rpt-109/V1.0.pdf', 'user-engineer-001', '2026-06-19 21:00:00'),
  ('rev-124', 'rpt-110', 'V1.0', 'Original archival copy (pre-approval)',                '/reports/rpt-110/V1.0.docx', '/reports/rpt-110/V1.0.pdf', 'user-engineer-001', '2026-06-12 14:00:00'),
  ('rev-125', 'rpt-111', 'V1.0', 'Original archival copy (pre-approval)',                '/reports/rpt-111/V1.0.docx', '/reports/rpt-111/V1.0.pdf', 'user-engineer-001', '2026-06-12 15:00:00'),
  ('rev-126', 'rpt-116', 'V1.0', 'Initial draft - hard anodised plates',                 '/reports/rpt-116/V1.0.docx', '/reports/rpt-116/V1.0.pdf', 'user-engineer-001', '2026-06-19 22:00:00'),
  ('rev-127', 'rpt-112', 'V1.0', 'Original archival copy (pre-approval)',                '/reports/rpt-112/V1.0.docx', '/reports/rpt-112/V1.0.pdf', 'user-engineer-001', '2026-06-12 16:00:00'),
  ('rev-128', 'rpt-113', 'V1.0', 'Original archival copy (pre-approval)',                '/reports/rpt-113/V1.0.docx', '/reports/rpt-113/V1.0.pdf', 'user-engineer-001', '2026-06-12 17:00:00')
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 10. i18n messages (24 new) — extend V2's 28
-- =============================================
-- Role keys, action keys, additional menu keys in zh-CN + en-US

INSERT INTO sys_i18n_message (id, message_key, locale, message_value, created_at, updated_at) VALUES
  -- New menu keys
  ('i18n-029', 'menu.admin',          'zh-CN', '系统管理',   NOW(), NOW()),
  ('i18n-030', 'menu.admin',          'en-US', 'Admin',      NOW(), NOW()),
  ('i18n-031', 'menu.workspace',      'zh-CN', '工作台',     NOW(), NOW()),
  ('i18n-032', 'menu.workspace',      'en-US', 'Workspace',  NOW(), NOW()),
  ('i18n-033', 'menu.testData',       'zh-CN', '测试数据',   NOW(), NOW()),
  ('i18n-034', 'menu.testData',       'en-US', 'Test Data',  NOW(), NOW()),
  ('i18n-035', 'menu.basicData',      'zh-CN', '基础数据',   NOW(), NOW()),
  ('i18n-036', 'menu.basicData',      'en-US', 'Basic Data', NOW(), NOW()),
  -- Role keys
  ('i18n-037', 'role.ADMIN',              'zh-CN', '系统管理员',     NOW(), NOW()),
  ('i18n-038', 'role.ADMIN',              'en-US', 'System Admin',   NOW(), NOW()),
  ('i18n-039', 'role.MANAGER',            'zh-CN', '经理',           NOW(), NOW()),
  ('i18n-040', 'role.MANAGER',            'en-US', 'Manager',        NOW(), NOW()),
  ('i18n-041', 'role.ENGINEER',           'zh-CN', '工程师',         NOW(), NOW()),
  ('i18n-042', 'role.ENGINEER',           'en-US', 'Engineer',       NOW(), NOW()),
  ('i18n-043', 'role.TECHNICIAN',         'zh-CN', '技术员',         NOW(), NOW()),
  ('i18n-044', 'role.TECHNICIAN',         'en-US', 'Technician',     NOW(), NOW()),
  ('i18n-045', 'role.REQUESTER',          'zh-CN', '委托人',         NOW(), NOW()),
  ('i18n-046', 'role.REQUESTER',          'en-US', 'Requester',      NOW(), NOW()),
  ('i18n-047', 'role.MANAGER_APPROVER',   'zh-CN', '审批经理',       NOW(), NOW()),
  ('i18n-048', 'role.MANAGER_APPROVER',   'en-US', 'Manager Approver',NOW(), NOW()),
  ('i18n-049', 'role.QC_REVIEWER',        'zh-CN', '质检审核员',     NOW(), NOW()),
  ('i18n-050', 'role.QC_REVIEWER',        'en-US', 'QC Reviewer',    NOW(), NOW()),
  ('i18n-051', 'role.COMPLIANCE',         'zh-CN', '合规专员',       NOW(), NOW()),
  ('i18n-052', 'role.COMPLIANCE',         'en-US', 'Compliance',     NOW(), NOW())
ON CONFLICT (id) DO NOTHING;



