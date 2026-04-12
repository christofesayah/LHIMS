ALTER TABLE audit_logs
ADD COLUMN IF NOT EXISTS region_id BIGINT;

ALTER TABLE audit_logs
DROP CONSTRAINT IF EXISTS fk_audit_region;

ALTER TABLE audit_logs
ADD CONSTRAINT fk_audit_region
FOREIGN KEY (region_id) REFERENCES regions(region_id);
