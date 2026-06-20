-- V14: Add the `code` column to analysis_item.
--
-- The /test-data/analysis-items page renders a "Code" column and the
-- create/edit form has a required `code` field, but the underlying
-- column was never created — the entity class has no `code` field
-- and the DB table has no `code` column. As a result every row in
-- the list shows an empty Code cell, and any create/edit attempt
-- silently drops the value the user typed into the form.
--
-- This migration mirrors V13 (the same fix for request_type.code):
--   1. Adds `code VARCHAR(64)` (nullable for the backfill window).
--   2. Backfills the existing rows from `id` — the seeded ids are
--      stable identifiers like 'item-001', 'item-005', 'item-101'
--      and make perfectly good codes.
--   3. Promotes it to NOT NULL.
--   4. Adds a UNIQUE constraint so two rows can never share a code.
--
-- The entity change (private String code;) is in
-- lims-model/.../entity/AnalysisItem.java — both must ship together.

ALTER TABLE analysis_item ADD COLUMN code VARCHAR(64);
UPDATE analysis_item SET code = id WHERE code IS NULL;
ALTER TABLE analysis_item ALTER COLUMN code SET NOT NULL;
ALTER TABLE analysis_item ADD CONSTRAINT analysis_item_code_uk UNIQUE (code);
