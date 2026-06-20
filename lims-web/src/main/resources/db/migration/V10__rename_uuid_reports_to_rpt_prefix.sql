-- V10: Normalize report.id to the rpt-NNN convention.
--
-- The Report entity extends BaseEntity, whose @TableId uses
-- IdType.ASSIGN_UUID. So every "Generate Report" click from the UI
-- inserts a row with a random UUID primary key (e.g.
-- '34212c11c2c004b30643b27ab0bc055e'). Meanwhile the seed migrations
-- (V2/V6/V7) use hand-curated ids like 'rpt-001', 'rpt-101',
-- 'rpt-edge-001'. The result is two id styles in the same table,
-- which the /report/list page surfaces verbatim.
--
-- This migration:
--   1. Renames the 5 UUID-shaped rows from the live environment to
--      'rpt-003'..'rpt-007' (the next free numbers — V2 used
--      001/002, V6 jumped to 101, V7 added rpt-edge-001..005).
--   2. Adds a CHECK constraint enforcing the rpt-* format on the
--      report table so future code regressions are caught at the DB
--      layer instead of leaking through the API.
--
-- The companion code change lives in
-- ReportIdGenerator (lims-common) + Report entity, which make every
-- newly-inserted Report also produce an rpt-NNN id, keeping the
-- application and DB in sync.

UPDATE report SET id = 'rpt-003' WHERE id = '204cc3720796f0560dc306a848150dde';
UPDATE report SET id = 'rpt-004' WHERE id = '3973ecc080a34bce3861ea91effe55e8';
UPDATE report SET id = 'rpt-005' WHERE id = '34212c11c2c004b30643b27ab0bc055e';
UPDATE report SET id = 'rpt-006' WHERE id = '8232bd82fce71414c159dd831cf269b7';
UPDATE report SET id = 'rpt-007' WHERE id = '950386eece00cfc5b182e56900f2ff03';

-- Enforce the rpt-* prefix going forward. The constraint name is
-- stable so it can be referenced / dropped in a future migration
-- if the convention ever changes.
ALTER TABLE report
    ADD CONSTRAINT report_id_rpt_prefix_chk
    CHECK (id ~ '^rpt-[A-Za-z0-9_-]+$');
