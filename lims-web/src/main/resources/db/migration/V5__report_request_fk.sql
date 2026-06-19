-- V5: re-add report.request_id foreign key (issue #68 / P10)
--
-- Issue #8 removed all FKs to allow flexible migration order. Since then
-- P5 added a defensive requestMapper.selectById check in
-- ReportService.createReport. This migration is defense-in-depth: orphans
-- are now impossible at the DB layer too.
--
-- We use ALTER TABLE ADD CONSTRAINT rather than REFERENCES inline so
-- the constraint name is stable and migration-safe across schema reloads.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_report_request_id'
          AND table_name = 'report'
    ) THEN
        ALTER TABLE report
            ADD CONSTRAINT fk_report_request_id
            FOREIGN KEY (request_id)
            REFERENCES request(id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE;
    END IF;
END $$;
