# Form Test Data Design — Material-LIMS

**Date:** 2026-06-19
**Author:** binbinao (with Claude)
**Status:** Approved (pending user sign-off after spec review)
**Branch:** fix/issue-36-backend-small

## 1. Goal & Non-Goals

### Goal

Provide rich, idempotent, business-realistic seed data so that every form page in
`lims-web-ui/src/pages/` can be exercised in the dev environment with one
`./mvnw -pl lims-web spring-boot:run` and a fresh `podman-compose up -d postgres`.

### Non-Goals

- Production-grade anonymised data: this seed runs in dev only.
- Performance/load test fixtures: see Section 7 for what is intentionally out of scope.
- Mocking the backend in the frontend: the project already has `DevAuthFilter`
  and a real DB; no MSW/Mock.js needed.
- E2E test fixtures as a separate file: V3 seed is the single source of truth.
  Playwright tests should reference `req-101` etc. directly.

## 2. Architecture

Three-layer migration history:

```
lims-web/src/main/resources/db/migration/
├── V1__init.sql               (existing — 11-table schema)
├── V2__seed_dev.sql           (existing — base dictionary, 148 lines)
├── V3__seed_business.sql      (new — 30-100 rows per entity, business-realistic)
└── V4__seed_edge_cases.sql    (new — 5-10 boundary rows per major table)
```

Layering rationale:
- V1/V2 already ship; touching them risks breaking the migration checksum.
- V3 is the **exhibit** layer: every status, priority, role, and visibility has
  at least one visible row.
- V4 is the **stress** layer: every form's nullable, length, and character-set
  edge cases get a deliberate row.

## 3. Coverage Matrix

### 3.1 Core business tables — V3

| Table | Frontend forms | V3 row count | Notes |
|---|---|---|---|
| `request` | RequestList, RequestDetail, RequestCreate, RequestKanban, RequestDashboard | 35-50 | Covers all 7 statuses × 4 priorities |
| `analysis_task` | RequestDetail (task list) | 60-80 | One-to-many from in-flight requests |
| `sample` | RequestDetail (sample list) | 30-40 | Mirrors task state |
| `report` | ReportList, ReportDetail, ReportEdit, ReportArchive | 15-20 | All 5 report statuses |
| `report_revision` | ReportRevisions | 25-35 | History of approved/revised reports |
| `equipment_repair` | EquipmentRepairs | 10-15 | All 4 repair statuses |
| `knowledge_doc` | KnowledgeList | 8-12 | All 4 visibilities × 4 categories |

### 3.2 Dictionary tables — V3 (extend, do not replace)

| Table | Frontend form | New rows | Sample IDs |
|---|---|---|---|
| `brand` | BrandList | 5 | `brand-101`..`brand-105` (DuPont, 3M, Henkel, Wacker, Momentive) |
| `department` | DepartmentList | 4 | `dept-101`..`dept-104` (incl. child depts) |
| `request_type` | RequestTypeList | 3 | `type-101`..`type-103` |
| `request_note` | RequestNoteList | 4 | `note-101`..`note-104` |
| `test_group` | TestGroupList | 2 | `group-101`, `group-102` |
| `analysis_type` | AnalysisTypeList | 4 | `atype-101`..`atype-104` |
| `analysis_item` | AnalysisItemList | 6 | `item-101`..`item-106` (covers new analysis_type) |
| `specification` | SpecificationList | 4 | `spec-101`..`spec-104` |
| `test_site` | TestSiteList | 2 | `site-101`, `site-102` |
| `equipment` | EquipmentList | 4 | `equip-101`..`equip-104` (incl. INACTIVE/RETIRED) |
| `holiday` | HolidayList | 5 | `hol-101`..`hol-105` (incl. weekend make-up days) |
| `sys_user` | UserList | 5 | `user-101`..`user-105` (incl. MANAGER_APPROVER, QC_REVIEWER) |
| `sys_i18n_message` | I18nList | 25 | menu/role/status keys in zh-CN + en-US |

### 3.3 Edge case tables — V4

| Table | Row count | What it stresses |
|---|---|---|
| `request` | 10 (`req-edge-001`..`010`) | Long text, emoji, XSS, SQL inject, near/overdue, NULL fields, 0.01/999999.99 cost |
| `report` | 5 (`rpt-edge-001`..`005`) | 2000-char note, special chars, NULL file_url, huge version, DRAFT-with-PDF anomaly |
| `knowledge_doc` | 3 (`doc-edge-001`..`003`) | 10000-char body, Base64 image, 50-tag array |
| `equipment` | 2 (`equip-edge-001`..`002`) | Expired warranty, day-old purchase |

## 4. ID & Idempotency Strategy

### ID Convention

| Layer | Pattern | Example | Why |
|---|---|---|---|
| V1/V2 | `<table>-<NNN>` | `brand-001`, `req-001` | Pre-existing; do not touch |
| V3 | `<table>-<1NN+>` | `brand-101`, `req-101` | `1` prefix collides nowhere, sortable |
| V4 | `<table>-edge-<NNN>` | `req-edge-001` | `edge` segment makes intent obvious in `psql` output |

### SQL pattern (PostgreSQL)

```sql
INSERT INTO brand (id, name, description, sort_order) VALUES
  ('brand-101', 'DuPont', 'DuPont de Nemours Inc.', 6),
  ('brand-102', '3M', '3M Company', 7),
  ('brand-103', 'Henkel', 'Henkel AG & Co. KGaA', 8),
  ('brand-104', 'Wacker', 'Wacker Chemie AG', 9),
  ('brand-105', 'Momentive', 'Momentive Performance Materials', 10)
ON CONFLICT (id) DO NOTHING;
```

For tables with a business unique key (e.g. `request.request_no`), list it in
the `ON CONFLICT` clause:

```sql
ON CONFLICT (id) DO NOTHING;
-- request_no is generated, so id is the only conflict target.
-- For other tables: ON CONFLICT (request_no) DO NOTHING;
```

### Rules

- No `TRUNCATE`, no `DELETE` — preserve V2 rows unconditionally.
- Do not edit V1 or V2 — Flyway checksum will fail otherwise.
- All `created_at` / `updated_at` set explicitly to a fixed timestamp
  (`'2026-06-19 09:00:00'`) so date-filter tests are deterministic.
- All foreign keys reference real V2/V3 IDs — no orphan rows.

## 5. V3 Distribution Design

### 5.1 request status × priority matrix (target ≥2 per cell)

| status \ priority | URGENT | HIGH | NORMAL | LOW |
|---|---|---|---|---|
| SUBMITTED | 2 | 2 | 3 | 1 |
| ASSIGNED | 1 | 1 | 2 | 1 |
| SAMPLING | 1 | 1 | 2 | 1 |
| REPORTING | 2 | 1 | 2 | 1 |
| APPROVING | 1 | 1 | 1 | 1 |
| COMPLETED | 2 | 3 | 4 | 1 |
| REJECTED | 1 | 0 | 1 | 0 |

Total: 35 rows. Each row has at least one of `analysis_task` / `sample` / `report`
child row(s) so the form's detail page is never empty.

### 5.2 analysis_task status distribution

| Status | % of tasks | Notes |
|---|---|---|
| PENDING | 30% | Queued behind a running task |
| IN_PROGRESS | 30% | Mid-flight |
| COMPLETED | 30% | Closed out |
| DELAYED | 10% | Includes `delay_reason` populated |

### 5.3 sample status distribution

| Status | % of samples |
|---|---|
| READY | 60% |
| PREPARING | 25% |
| PENDING | 15% |

### 5.4 report status distribution

| Status | Count | Has multiple revisions? |
|---|---|---|
| DRAFT | 4 | no |
| IN_REVIEW | 5 | no |
| APPROVED | 4 | yes (V1.0 + V1.1) |
| REVISED | 2 | yes (V1.0 + V1.1 + V1.2) |
| REJECTED | 1 | no |

### 5.5 knowledge_doc visibility × category

| visibility \ category | CHEMICAL | PHYSICAL | ENVIRONMENTAL | COMPLIANCE |
|---|---|---|---|---|
| PRIVATE | 1 | 1 | 0 | 0 |
| DEPARTMENT | 1 | 0 | 1 | 0 |
| PUBLIC | 1 | 1 | 1 | 0 |
| INTERNAL | 0 | 1 | 0 | 1 |

Total: 8 rows; 1-2 with attachments.

### 5.6 equipment_repair status distribution

| Status | Count |
|---|---|
| REPORTED | 4 |
| REPAIRING | 3 |
| COMPLETED | 3 |
| CANCELLED | 1 |

## 6. V4 Edge Case Catalogue

### 6.1 request — 10 rows

| ID | Scenario | Critical field value |
|---|---|---|
| `req-edge-001` | Long reason (1500 chars) | `request_reason` = 1500-char text with newlines |
| `req-edge-002` | Emoji + multilingual | `request_reason` = `🚀 🔬 ✨ 测试 テスト 테스트 测试 العربية` |
| `req-edge-003` | XSS/HTML injection | `request_reason` = `<script>alert('xss')</script><img src=x onerror=alert(1)>` |
| `req-edge-004` | SQL injection probe | `supplier_name` = `Acme'; DROP TABLE request; --` |
| `req-edge-005` | Near-due (T+2) | `due_date` = 2026-06-21, status = IN_PROGRESS |
| `req-edge-006` | Severely overdue (T-30) | `due_date` = 2026-05-20, status = SAMPLING |
| `req-edge-007` | All optional fields NULL | `eco`, `proxy_requester_id`, `sample_delivery_note` = NULL |
| `req-edge-008` | Tiny cost | `total_cost` = 0.01 |
| `req-edge-009` | Huge cost | `total_cost` = 999999.99 |
| `req-edge-010` | Full/half-width + nested quotes | `supplier_name` = `"科思创" '上海' 有限公司` |

### 6.2 report — 5 rows

| ID | Scenario | Critical field |
|---|---|---|
| `rpt-edge-001` | 2000-char `revision_note` | Includes JSON, Markdown, URL |
| `rpt-edge-002` | Special chars | `< > & " ' / \ %` all present |
| `rpt-edge-003` | NULL `file_url` | Simulates upload-not-yet-complete |
| `rpt-edge-004` | Huge version number | `version_number` = `V999.999` |
| `rpt-edge-005` | DRAFT with PDF (anomaly) | `status` = DRAFT, `pdf_url` populated |

### 6.3 knowledge_doc — 3 rows

| ID | Scenario |
|---|---|
| `doc-edge-001` | 10 000-character body (pagination/lazy load) |
| `doc-edge-002` | Body contains 10 KB Base64 image data |
| `doc-edge-003` | `tags` array of 50 entries |

### 6.4 equipment — 2 rows

| ID | Scenario |
|---|---|
| `equip-edge-001` | `warranty_expiry` = 2020-01-01 (expired) |
| `equip-edge-002` | `purchase_date` = 2026-06-18 (yesterday) |

### Edge case rules

1. Stay within DB constraints: no `VARCHAR` overflow, no FK violation, no `NOT NULL` violation.
2. Every edge row's status is a valid enum value — no invalid enum strings.
3. Edge rows use small `sort_order` so they appear on the first page of any list.
4. Edge IDs (`*-edge-*`) and V3 IDs (`-1NN`) are disjoint — no overlap.

## 7. Verification Plan

### Step 1: First-run Flyway migration

```bash
podman-compose down -v
podman-compose up -d postgres
./mvnw -pl lims-web spring-boot:run -Dspring-boot.run.profiles=dev
```

Pass criteria:
- Log shows `Migrating schema "public" to version "3 - seed business"`
- Log shows `Migrating schema "public" to version "4 - seed edge cases"`
- Log shows `Successfully applied 2 migrations to schema "public"`
- No `ERROR` / `Migration failed`

### Step 2: Re-run for idempotency

```bash
./mvnw -pl lims-web spring-boot:run -Dspring-boot.run.profiles=dev
```

Pass criteria:
- Log shows `Schema "public" is up to date. No migration necessary.`
- No `duplicate key value violates unique constraint` errors
- `SELECT COUNT(*) FROM request;` returns the same number as after first run

### Step 3: Distribution spot-check (one-shot SQL)

```sql
SELECT status, priority, COUNT(*) FROM request GROUP BY status, priority ORDER BY status, priority;
SELECT COUNT(*) FROM request WHERE id LIKE 'req-edge-%';   -- expect 10
SELECT COUNT(*) FROM report WHERE id LIKE 'rpt-edge-%';    -- expect 5
SELECT COUNT(*) FROM analysis_task WHERE status = 'DELAYED';  -- expect ≥3
```

Pass criteria: every status has ≥2 rows, every priority has ≥2 rows,
edge rows present, FK orphan check returns 0.

### Step 4: Frontend form render check

`cd lims-web-ui && npm start` then in browser (or Playwright snapshot):

| Form | Check | Pass |
|---|---|---|
| RequestList | First page contains a `req-1*` row | ✓ |
| RequestDetail (`/request/req-101`) | Renders ≥1 task and ≥1 sample | ✓ |
| ReportList filtered by `APPROVED` | Shows at least one `rpt-1*` | ✓ |
| ReportRevisions | An approved report has 2 versions | ✓ |
| KnowledgeList filtered by `PUBLIC` | Shows at least one `doc-1*` | ✓ |
| EquipmentRepairs filtered by `COMPLETED` | Shows at least one new row | ✓ |
| BrandList | `brand-101..105` visible in order | ✓ |
| RequestTypeList | `type-101` shows `task_duration_days` | ✓ |

### Step 5: E2E smoke

Update `tests/e2e/features/request-lifecycle.spec.ts` to use `req-101` as the
test target (instead of `req-001`). `npx playwright test` should still be green.

## 8. Out of Scope

- Anonymised production-grade data: not needed for dev.
- Massive datasets (>500 rows/table) for perf: the project's load test is a
  separate concern; this seed is sized for UI exercising.
- Frontend mock data: the project uses a real DB + `DevAuthFilter`; no MSW.
- Per-test fixtures: Playwright tests share the V3 seed; no per-spec file.
- Refreshing V2: V2 rows stay; V3/V4 only add.

## 9. File Touch List

| File | Change |
|---|---|
| `lims-web/src/main/resources/db/migration/V3__seed_business.sql` | NEW — 800-1200 lines |
| `lims-web/src/main/resources/db/migration/V4__seed_edge_cases.sql` | NEW — 200-400 lines |
| `tests/e2e/features/request-lifecycle.spec.ts` | UPDATE — switch fixture to `req-101` |
| `docs/superpowers/specs/2026-06-19-form-test-data-design.md` | NEW — this file |

## 10. Open Questions

None at design time. If the first migration run reveals schema gaps, V3 is the
right place to fix them (V1 schema changes would force a re-init).
