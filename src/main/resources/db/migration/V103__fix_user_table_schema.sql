-- This migration ensures the users table has the correct columns expected by the Java entities.
-- It fixes the discrepancy between V3 and V7.

ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(255) NOT NULL DEFAULT 'Unknown';
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(255) NOT NULL DEFAULT 'Unknown';

-- Ensure we have the correct facility relationship column
ALTER TABLE users ADD COLUMN IF NOT EXISTS assigned_facility_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.constraint_column_usage 
        WHERE table_name = 'users' AND constraint_name = 'fk_assigned_facility'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT fk_assigned_facility 
        FOREIGN KEY (assigned_facility_id) REFERENCES health_facilities(facility_id);
    END IF;
END $$;
