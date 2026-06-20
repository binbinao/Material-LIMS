-- Flyway V4 — Edge-case seed data
-- Purpose: stress-test forms with boundary conditions: long text, emoji,
-- XSS / SQL-injection strings, NULL fields, near-/over-due dates, and
-- large numeric ranges. All rows use ON CONFLICT (id) DO NOTHING so the
-- migration is idempotent.
--
-- ID convention: <table>-edge-NNN — see design doc
-- docs/superpowers/specs/2026-06-19-form-test-data-design.md

-- =============================================
-- 1. request edge cases (10 rows)
-- =============================================

-- req-edge-001: 1500-character request_reason with newlines
INSERT INTO request (id, request_no, brand_id, dept_id, type_id, requester_id, request_reason, priority, status, due_date, total_cost, submitted_at, assigned_at, created_by, updated_by, created_at, updated_at) VALUES
  ('req-edge-001', 'REQ-2026-9001', 'brand-101', 'dept-004', 'type-001', 'user-requester-001',
   'This is an intentionally very long request reason for stress testing the form layout and database column. It contains 1500 characters, multiple line breaks, and references to long compound names. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Section two: chemical analysis requested for new polymer batch with glass-fibre reinforcement at 30 percent by weight; tests must include melt flow index per ISO 1133 condition 190 over 2.16, differential scanning calorimetry per ASTM D3418 from negative fifty to three hundred Celsius, and thermogravimetric analysis per ASTM E1131 in nitrogen atmosphere from ambient to eight hundred Celsius. Additionally, please include tensile test per ISO 527 on Type 1A specimens cut from injection-moulded plaques conditioned at twenty-three Celsius and fifty percent relative humidity for at least forty hours. End of long request reason.',
   'NORMAL', 'SUBMITTED', '2026-07-15', NULL, '2026-06-20 09:00:00', NULL, 'user-requester-001', 'user-requester-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- req-edge-002: emoji + multilingual reason
INSERT INTO request (id, request_no, brand_id, dept_id, type_id, requester_id, request_reason, priority, status, due_date, sample_delivery_note, submitted_at, created_by, updated_by, created_at, updated_at) VALUES
  ('req-edge-002', 'REQ-2026-9002', 'brand-102', 'dept-005', 'type-002', 'user-requester-001',
   '🚀🔬✨ Multilingual stress test: 中文测试 日本語テスト 한국어 테스트 العربية اختبار עברית בדיקה русский тест ελληνικά δοκιμή. Standard analysis requested per SOP.',
   'NORMAL', 'SUBMITTED', '2026-07-10', '🌡️ Keep below 25°C ⚠️ hazardous material 🚫 no direct sunlight', '2026-06-20 09:00:00', 'user-requester-001', 'user-requester-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- req-edge-003: XSS/HTML injection in reason
INSERT INTO request (id, request_no, brand_id, dept_id, type_id, requester_id, request_reason, priority, status, due_date, sample_delivery_note, submitted_at, created_by, updated_by, created_at, updated_at) VALUES
  ('req-edge-003', 'REQ-2026-9003', 'brand-103', 'dept-006', 'type-003', 'user-requester-001',
   'Standard analysis requested. <script>alert("xss")</script><img src=x onerror=alert(1)> And <iframe src="javascript:alert(2)"></iframe> Also &lt;script&gt;encoded&lt;/script&gt;',
   'NORMAL', 'SUBMITTED', '2026-07-05', '<b>bold</b> & "quotes" ''apostrophes'' /slashes\\ %percent < >brackets', '2026-06-20 09:00:00', 'user-requester-001', 'user-requester-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- req-edge-004: SQL injection probe in supplier_name
INSERT INTO request (id, request_no, brand_id, dept_id, type_id, requester_id, supplier_code, supplier_name, request_reason, priority, status, due_date, submitted_at, created_by, updated_by, created_at, updated_at) VALUES
  ('req-edge-004', 'REQ-2026-9004', 'brand-104', 'dept-004', 'type-001', 'user-requester-001',
   'SUP-001''', 'Acme Corp''; DROP TABLE request; --',
   'Routine incoming inspection. Note: supplier_name contains SQL probe characters to verify backend uses parameterized queries.',
   'NORMAL', 'SUBMITTED', '2026-07-08', '2026-06-20 09:00:00', 'user-requester-001', 'user-requester-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- req-edge-005: near-due (T+2 days from today 2026-06-20, so due 2026-06-22).
-- Use ASSIGNED status (request status enum: ASSIGNED/SAMPLING/... — IN_PROGRESS is for analysis_task, not request).
INSERT INTO request (id, request_no, brand_id, dept_id, type_id, requester_id, request_reason, priority, status, due_date, submitted_at, assigned_at, created_by, updated_by, created_at, updated_at) VALUES
  ('req-edge-005', 'REQ-2026-9005', 'brand-105', 'dept-005', 'type-002', 'user-requester-001',
   'Due in 2 days — UI should show yellow/orange warning badge.',
   'HIGH', 'ASSIGNED', '2026-06-22', '2026-06-15 09:00:00', '2026-06-15 10:00:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- req-edge-006: severely overdue (due 2026-05-21, T-30 from today)
INSERT INTO request (id, request_no, brand_id, dept_id, type_id, requester_id, request_reason, priority, status, due_date, sample_delivery_note, submitted_at, assigned_at, created_by, updated_by, created_at, updated_at) VALUES
  ('req-edge-006', 'REQ-2026-9006', 'brand-101', 'dept-006', 'type-003', 'user-requester-001',
   'Overdue by 30 days — UI should show red/danger badge. Stalled during sample prep.',
   'URGENT', 'SAMPLING', '2026-05-21', 'Original sample rejected, awaiting reshipment', '2026-04-15 09:00:00', '2026-04-15 10:00:00', 'user-requester-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- req-edge-007: all optional fields NULL
INSERT INTO request (id, request_no, brand_id, dept_id, type_id, requester_id, request_reason, priority, status, due_date, submitted_at, created_by, updated_by, created_at, updated_at) VALUES
  ('req-edge-007', 'REQ-2026-9007', 'brand-102', 'dept-004', 'type-001', 'user-requester-001',
   'Minimal request — all optional fields null, exercises NOT NULL vs nullable column rendering.',
   'LOW', 'SUBMITTED', NULL, '2026-06-20 09:00:00', 'user-requester-001', 'user-requester-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- req-edge-008: tiny cost (0.01)
INSERT INTO request (id, request_no, brand_id, dept_id, type_id, requester_id, request_reason, priority, status, due_date, total_cost, submitted_at, created_by, updated_by, created_at, updated_at) VALUES
  ('req-edge-008', 'REQ-2026-9008', 'brand-103', 'dept-005', 'type-002', 'user-requester-001',
   'Minimal cost test — exercises DECIMAL(14,2) lower bound rendering.',
   'NORMAL', 'SUBMITTED', '2026-07-12', 0.01, '2026-06-20 09:00:00', 'user-requester-001', 'user-requester-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- req-edge-009: huge cost (999999.99)
INSERT INTO request (id, request_no, brand_id, dept_id, type_id, requester_id, request_reason, priority, status, due_date, total_cost, submitted_at, created_by, updated_by, created_at, updated_at) VALUES
  ('req-edge-009', 'REQ-2026-9009', 'brand-104', 'dept-006', 'type-003', 'user-requester-001',
   'Maximum cost test — exercises DECIMAL(14,2) upper bound. Large multi-vendor qualification campaign, all in one request for simplicity.',
   'URGENT', 'SUBMITTED', '2026-07-20', 999999.99, '2026-06-20 09:00:00', 'user-requester-001', 'user-requester-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- req-edge-010: full/half-width + nested quotes
INSERT INTO request (id, request_no, brand_id, dept_id, type_id, requester_id, supplier_code, supplier_name, request_reason, priority, status, due_date, submitted_at, created_by, updated_by, created_at, updated_at) VALUES
  ('req-edge-010', 'REQ-2026-9010', 'brand-105', 'dept-004', 'type-001', 'user-requester-001',
   'SUP-MIX-01', '"科思创" ''上海'' 有限公司（Ｃｏｖｅｓｔｒｏ Ｓｈａｎｇｈａｉ）',
   'Full/half-width character mix test. Supplier name contains CJK, nested single+double quotes, full-width parentheses. Part name ＰＮ-ＡＢＣ-001 全角.',
   'NORMAL', 'SUBMITTED', '2026-07-18', '2026-06-20 09:00:00', 'user-requester-001', 'user-requester-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 2. report edge cases (5 rows)
-- =============================================
-- Use existing V3 tasks as the source so request_id + task_id are valid FKs

-- rpt-edge-001: 2000-character revision_note with JSON/Markdown/URL
INSERT INTO report (id, request_id, task_id, author_id, version_number, revision_note, status, file_url, pdf_url, submitted_at, created_by, updated_by, created_at, updated_at) VALUES
  ('rpt-edge-001', 'req-129', 'task-136', 'user-engineer-001', 'V1.0',
   'Long revision note. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Embedded JSON: {"test":"FR-Nylon-66","method":"ASTM D2863","result":"LOI=28%","conclusion":"pass"}. Markdown header: ## Findings\n\n- See [data table](https://example.com/reports/123)\n- See **appendix B**\n\n```sql\nSELECT * FROM analysis_task WHERE request_id = ''req-129'';\n```\n\nEnd of 2000-character note.',
   'DRAFT', '/reports/rpt-edge-001/V1.0.docx', NULL, NULL, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- rpt-edge-002: special characters in revision_note
INSERT INTO report (id, request_id, task_id, author_id, version_number, revision_note, status, file_url, pdf_url, submitted_at, created_by, updated_by, created_at, updated_at) VALUES
  ('rpt-edge-002', 'req-130', 'task-138', 'user-engineer-001', 'V1.0',
   'Special chars: < > & " '' / \ % $ # @ ! ? ~ ` ^ ( ) [ ] { } | ; : , .',
   'DRAFT', '/reports/rpt-edge-002/V1.0.docx', NULL, NULL, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- rpt-edge-003: NULL file_url
INSERT INTO report (id, request_id, task_id, author_id, version_number, revision_note, status, file_url, pdf_url, submitted_at, created_by, updated_by, created_at, updated_at) VALUES
  ('rpt-edge-003', 'req-131', 'task-140', 'user-engineer-001', 'V1.0',
   'File not yet uploaded — exercises nullable file_url rendering on list/detail pages.',
   'DRAFT', NULL, NULL, NULL, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- rpt-edge-004: huge version number
INSERT INTO report (id, request_id, task_id, author_id, version_number, revision_note, status, file_url, pdf_url, submitted_at, created_by, updated_by, created_at, updated_at) VALUES
  ('rpt-edge-004', 'req-132', 'task-142', 'user-engineer-001', 'V999.999',
   'Version number boundary test — exercises VARCHAR(20) truncation handling and sort behaviour.',
   'DRAFT', '/reports/rpt-edge-004/V999.999.docx', NULL, NULL, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- rpt-edge-005: DRAFT with PDF (anomalous state — author uploaded PDF before final draft)
INSERT INTO report (id, request_id, task_id, author_id, version_number, revision_note, status, file_url, pdf_url, submitted_at, created_by, updated_by, created_at, updated_at) VALUES
  ('rpt-edge-005', 'req-133', 'task-144', 'user-engineer-001', 'V0.5',
   'Anomaly: status=DRAFT but pdf_url populated. Used to test whether the UI gates PDF download on status.',
   'DRAFT', '/reports/rpt-edge-005/V0.5.docx', '/reports/rpt-edge-005/V0.5.pdf', NULL, 'user-engineer-001', 'user-engineer-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 3. knowledge_doc edge cases (2 rows; no tags column in schema)
-- =============================================

-- doc-edge-001: 10000-character description (pagination/lazy-load stress)
INSERT INTO knowledge_doc (id, title, category, file_url, file_size, description, created_by, updated_by, created_at, updated_at) VALUES
  ('doc-edge-001', 'Mega Procedure Manual (Edge Test)', 'MANUAL',
   '/knowledge/edge/mega-procedure.pdf', 12000000,
   'A' || repeat('B', 4990) || ' ' || repeat('C', 5000) || ' End of 10000-char description used to test pagination and lazy loading of long TEXT fields in the knowledge doc list and detail pages.',
   'user-admin-001', 'user-admin-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- doc-edge-002: Base64 image data in description (10 KB)
INSERT INTO knowledge_doc (id, title, category, file_url, file_size, description, created_by, updated_by, created_at, updated_at) VALUES
  ('doc-edge-002', 'Inline-Image Embedding Test', 'MANUAL',
   '/knowledge/edge/inline-image.pdf', 3500000,
   'Base64-encoded inline image data follows: ' ||
   'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=' ||
   repeat('A', 10000) ||
   ' End of 10KB+ Base64 test payload. Exercises rich-text rendering and XSS filtering on large strings.',
   'user-admin-001', 'user-admin-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- =============================================
-- 4. equipment edge cases (2 rows)
-- =============================================

-- equip-edge-001: warranty expired 6 years ago
INSERT INTO equipment (id, name, model, serial_number, status, location, purchase_date, warranty_expiry, description, created_by, updated_by, created_at, updated_at) VALUES
  ('equip-edge-001', 'Legacy FTIR Spectrometer', 'PerkinElmer Frontier', 'FTIR-2020-0001', 'DECOMMISSIONED', 'Storage Room B',
   '2020-01-15', '2020-01-15', 'Old FTIR, no warranty. Decommissioned in 2024 but kept for legacy reference. UI should flag warranty_expired when status=ACTIVE, but here status is DECOMMISSIONED so it should render as historical.',
   'user-admin-001', 'user-admin-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- equip-edge-002: brand new, purchased 2 days ago
INSERT INTO equipment (id, name, model, serial_number, status, location, purchase_date, warranty_expiry, description, created_by, updated_by, created_at, updated_at) VALUES
  ('equip-edge-002', 'Benchtop pH Meter', 'Mettler Toledo SevenCompact', 'pH-2026-0019', 'ACTIVE', 'Wet Lab Bench 3',
   '2026-06-18', '2029-06-18', 'Brand-new pH meter, commissioned yesterday. UI should highlight <7-day purchase.',
   'user-admin-001', 'user-admin-001', '2026-06-20 09:00:00', '2026-06-20 09:00:00')
ON CONFLICT (id) DO NOTHING;
