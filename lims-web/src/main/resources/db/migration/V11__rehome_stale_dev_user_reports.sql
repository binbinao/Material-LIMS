-- V11: Re-home reports whose author_id is the legacy 'dev-user-0001'
-- placeholder to 'user-engineer-001'.
--
-- Background: before DevAuthFilter was taught to honor the X-Dev-User
-- header (lims-web/.../security/DevAuthFilter.java), every dev
-- session inserted as 'dev-user-0001'. After the fix, the dev
-- "engineer" principal is 'user-engineer-001' (see V2__seed_dev.sql).
-- Six rows in the report table are still authored by
-- 'dev-user-0001':
--
--   rpt-003  req-001  DRAFT
--   rpt-004  req-001  DRAFT
--   rpt-005  req-001  DRAFT
--   rpt-006  req-002  DRAFT
--   rpt-007  req-002  DRAFT
--   rpt-117  req-001  IN_REVIEW
--
-- rpt-117 happens to be in IN_REVIEW and the manager dev user can
-- approve it (author != approver still holds, because approver is
-- 'user-manager-001'), so the four-eyes check is unaffected. The five
-- DRAFT rows, however, are stuck: ReportService.submitReport enforces
-- ownership (validateReportOwnership rejects when authorId !=
-- currentUserId), so the engineer dev user — the only one with
-- ENGINEER role — gets a confusing 3002 ACCESS_DENIED on every
-- attempt to submit them. Users reported "rpt-007 报告进程无法迁移"
-- because of this.
--
-- Mapping: every 'dev-user-0001' author becomes 'user-engineer-001'.
-- That's the right home because:
--   * Engineer is the only role that authors reports in this app.
--   * 'user-engineer-001' is exactly the user that DevAuthFilter
--     hands back for X-Dev-User: engineer in dev profile.
--   * Doing it row-by-row rather than a blanket UPDATE keeps the
--     migration auditable and easy to reason about.

UPDATE report SET author_id = 'user-engineer-001' WHERE id = 'rpt-003';
UPDATE report SET author_id = 'user-engineer-001' WHERE id = 'rpt-004';
UPDATE report SET author_id = 'user-engineer-001' WHERE id = 'rpt-005';
UPDATE report SET author_id = 'user-engineer-001' WHERE id = 'rpt-006';
UPDATE report SET author_id = 'user-engineer-001' WHERE id = 'rpt-007';
UPDATE report SET author_id = 'user-engineer-001' WHERE id = 'rpt-117';
