ALTER TABLE simulation_scenarios
ADD COLUMN IF NOT EXISTS region_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_scenarios_region'
          AND table_name = 'simulation_scenarios'
    ) THEN
        ALTER TABLE simulation_scenarios
        ADD CONSTRAINT fk_scenarios_region
        FOREIGN KEY (region_id) REFERENCES regions(region_id);
    END IF;
END $$;
