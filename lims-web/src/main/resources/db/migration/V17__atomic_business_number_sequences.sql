-- Atomic counters for business identifiers.
-- Values are initialized from existing numeric identifiers so deployment is non-breaking.
CREATE SEQUENCE IF NOT EXISTS request_no_seq;
SELECT setval(
    'request_no_seq',
    COALESCE((
        SELECT MAX(CAST(SUBSTRING(request_no FROM 11) AS INTEGER))
        FROM request
        WHERE request_no ~ '^REQ-[0-9]{4}-[0-9]+$'
    ), 0),
    true
);

CREATE SEQUENCE IF NOT EXISTS report_id_seq;
SELECT setval(
    'report_id_seq',
    COALESCE((
        SELECT MAX(CAST(SUBSTRING(id FROM 5) AS INTEGER))
        FROM report
        WHERE id ~ '^rpt-[0-9]+$'
    ), 0),
    true
);
