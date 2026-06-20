-- V13: Add the `code` column to request_type.
--
-- The /basic-data/request-types page renders a "Code" column and the
-- create/edit form has a required `code` field, but the underlying
-- column was never created — the entity class has no `code` field
-- and the DB table has no `code` column. As a result every row in
-- the list shows an empty Code cell and any create/edit attempt
-- silently drops the value.
--
-- This migration:
--   1. Adds `code VARCHAR(64)` (nullable for the backfill window).
--   2. Backfills the existing rows from `id` — the seeded ids are
--      stable identifiers like 'type-001', 'type-002', 'type-101'
--      and make perfectly good codes.
--   3. Promotes it to NOT NULL.
--   4. Adds a UNIQUE constraint so two rows can never share a code.
--
-- The entity change (private String code;) is in
-- lims-model/.../entity/RequestType.java — both must ship together.

ALTER TABLE request_type ADD COLUMN code VARCHAR(64);
UPDATE request_type SET code = id WHERE code IS NULL;
ALTER TABLE request_type ALTER COLUMN code SET NOT NULL;
ALTER TABLE request_type ADD CONSTRAINT request_type_code_uk UNIQUE (code);
