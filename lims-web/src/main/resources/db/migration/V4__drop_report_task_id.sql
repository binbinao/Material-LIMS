-- V4: drop dead report.task_id column (issue #66 / P9)
--
-- The task_id column was reserved for a "report linked to a specific task"
-- design that was never implemented — reports are tied to requests, not
-- tasks. The column was always NULL. Drop it to keep the schema honest.

ALTER TABLE report DROP COLUMN IF EXISTS task_id;
