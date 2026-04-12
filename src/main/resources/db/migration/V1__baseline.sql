CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS regions (
	region_id BIGSERIAL PRIMARY KEY,
	name VARCHAR(255) NOT NULL UNIQUE,
	type VARCHAR(50) NOT NULL,
	parent_region_id BIGINT,
	population INTEGER,
	geometry geometry(MultiPolygon, 4326),
	CONSTRAINT fk_regions_parent FOREIGN KEY (parent_region_id) REFERENCES regions(region_id)
);

CREATE TABLE IF NOT EXISTS users (
	user_id BIGSERIAL PRIMARY KEY,
	username VARCHAR(255) NOT NULL UNIQUE,
	email VARCHAR(255) NOT NULL UNIQUE,
	password_hash VARCHAR(255) NOT NULL,
	is_active BOOLEAN NOT NULL DEFAULT TRUE,
	created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS roles (
	role_id BIGSERIAL PRIMARY KEY,
	code VARCHAR(50) NOT NULL UNIQUE,
	role_name VARCHAR(255) NOT NULL,
	description TEXT
);

CREATE TABLE IF NOT EXISTS user_roles (
	user_role_id BIGSERIAL PRIMARY KEY,
	user_id BIGINT NOT NULL,
	role_id BIGINT NOT NULL,
	assigned_at TIMESTAMP NOT NULL,
	assigned_by_user_id BIGINT,
	CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(user_id),
	CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(role_id),
	CONSTRAINT fk_user_roles_assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS health_facilities (
	facility_id BIGSERIAL PRIMARY KEY,
	region_id BIGINT NOT NULL,
	name VARCHAR(255) NOT NULL,
	facility_type VARCHAR(50) NOT NULL,
	ownership_type VARCHAR(50) NOT NULL,
	latitude DOUBLE PRECISION,
	longitude DOUBLE PRECISION,
	geometry geometry(Point, 4326),
	CONSTRAINT fk_facilities_region FOREIGN KEY (region_id) REFERENCES regions(region_id)
);

CREATE TABLE IF NOT EXISTS facility_capacity (
	capacity_id BIGSERIAL PRIMARY KEY,
	facility_id BIGINT NOT NULL,
	reporting_date DATE NOT NULL,
	total_beds INTEGER,
	icu_beds INTEGER,
	doctors_count INTEGER,
	nurses_count INTEGER,
	operational_status VARCHAR(50),
	CONSTRAINT fk_capacity_facility FOREIGN KEY (facility_id) REFERENCES health_facilities(facility_id)
);

CREATE TABLE IF NOT EXISTS regional_vulnerability (
	vulnerability_id BIGSERIAL PRIMARY KEY,
	region_id BIGINT NOT NULL,
	reporting_period VARCHAR(100) NOT NULL,
	poverty_rate DOUBLE PRECISION,
	mortality_indicator DOUBLE PRECISION,
	demographic_pressure DOUBLE PRECISION,
	CONSTRAINT fk_vulnerability_region FOREIGN KEY (region_id) REFERENCES regions(region_id)
);

CREATE TABLE IF NOT EXISTS regional_access_metrics (
	access_metric_id BIGSERIAL PRIMARY KEY,
	region_id BIGINT NOT NULL,
	reporting_period VARCHAR(100) NOT NULL,
	hospitals_per_capita DOUBLE PRECISION,
	doctors_per_capita DOUBLE PRECISION,
	icu_beds_per_capita DOUBLE PRECISION,
	CONSTRAINT fk_access_region FOREIGN KEY (region_id) REFERENCES regions(region_id)
);

CREATE TABLE IF NOT EXISTS simulation_scenarios (
	scenario_id BIGSERIAL PRIMARY KEY,
	name VARCHAR(255) NOT NULL,
	description TEXT,
	baseline_period VARCHAR(100),
	created_by_user_id BIGINT NOT NULL,
	CONSTRAINT fk_scenarios_user FOREIGN KEY (created_by_user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS scenario_actions (
	action_id BIGSERIAL PRIMARY KEY,
	scenario_id BIGINT NOT NULL,
	region_id BIGINT NOT NULL,
	facility_id BIGINT,
	action_type VARCHAR(50) NOT NULL,
	old_value TEXT,
	new_value TEXT,
	CONSTRAINT fk_actions_scenario FOREIGN KEY (scenario_id) REFERENCES simulation_scenarios(scenario_id) ON DELETE CASCADE,
	CONSTRAINT fk_actions_region FOREIGN KEY (region_id) REFERENCES regions(region_id),
	CONSTRAINT fk_actions_facility FOREIGN KEY (facility_id) REFERENCES health_facilities(facility_id)
);

CREATE TABLE IF NOT EXISTS scenario_results (
	result_id BIGSERIAL PRIMARY KEY,
	scenario_id BIGINT NOT NULL,
	region_id BIGINT NOT NULL,
	simulated_hai DOUBLE PRECISION,
	simulated_rvi DOUBLE PRECISION,
	simulated_ciri DOUBLE PRECISION,
	impact_delta DOUBLE PRECISION,
	CONSTRAINT fk_results_scenario FOREIGN KEY (scenario_id) REFERENCES simulation_scenarios(scenario_id) ON DELETE CASCADE,
	CONSTRAINT fk_results_region FOREIGN KEY (region_id) REFERENCES regions(region_id)
);

CREATE TABLE IF NOT EXISTS computed_scores (
	score_id BIGSERIAL PRIMARY KEY,
	region_id BIGINT NOT NULL,
	scenario_id BIGINT,
	reporting_period VARCHAR(100) NOT NULL,
	hai_score DOUBLE PRECISION,
	rvi_score DOUBLE PRECISION,
	ciri_score DOUBLE PRECISION,
	risk_category VARCHAR(50),
	last_computed_at TIMESTAMP,
	CONSTRAINT fk_scores_region FOREIGN KEY (region_id) REFERENCES regions(region_id),
	CONSTRAINT fk_scores_scenario FOREIGN KEY (scenario_id) REFERENCES simulation_scenarios(scenario_id)
);

CREATE TABLE IF NOT EXISTS indicator_definitions (
	indicator_id BIGSERIAL PRIMARY KEY,
	code VARCHAR(100) NOT NULL UNIQUE,
	indicator_name VARCHAR(255) NOT NULL,
	formula_expression TEXT,
	normalization_method VARCHAR(100),
	weight DOUBLE PRECISION,
	normalization_min DOUBLE PRECISION,
	normalization_max DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS indicator_weights (
	weight_id BIGSERIAL PRIMARY KEY,
	indicator_id BIGINT NOT NULL,
	scenario_id BIGINT NOT NULL,
	weight_value DOUBLE PRECISION NOT NULL,
	effective_from DATE,
	effective_to DATE,
	CONSTRAINT fk_indicator_weights_indicator FOREIGN KEY (indicator_id) REFERENCES indicator_definitions(indicator_id),
	CONSTRAINT fk_indicator_weights_scenario FOREIGN KEY (scenario_id) REFERENCES simulation_scenarios(scenario_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_logs (
	audit_id BIGSERIAL PRIMARY KEY,
	actor_user_id BIGINT NOT NULL,
	target_user_id BIGINT,
	action_type VARCHAR(50) NOT NULL,
	entity_name VARCHAR(255),
	entity_id VARCHAR(255),
	old_value TEXT,
	new_value TEXT,
	ip_address VARCHAR(255),
	user_agent VARCHAR(512),
	created_at TIMESTAMP NOT NULL,
	CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users(user_id),
	CONSTRAINT fk_audit_target FOREIGN KEY (target_user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS notifications (
	notification_id BIGSERIAL PRIMARY KEY,
	user_id BIGINT NOT NULL,
	message TEXT NOT NULL,
	is_read BOOLEAN NOT NULL DEFAULT FALSE,
	created_at TIMESTAMP NOT NULL,
	CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS revoked_tokens (
	revoked_token_id BIGSERIAL PRIMARY KEY,
	token TEXT NOT NULL UNIQUE,
	revoked_at TIMESTAMP NOT NULL
);

INSERT INTO roles (code, role_name, description)
SELECT 'MINISTRY_OFFICIAL', 'Ministry Official', 'Full analytical access'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'MINISTRY_OFFICIAL');

INSERT INTO roles (code, role_name, description)
SELECT 'HOSPITAL_ADMIN', 'Hospital Admin', 'Operational district-level access'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'HOSPITAL_ADMIN');

INSERT INTO roles (code, role_name, description)
SELECT 'PUBLIC', 'Public Viewer', 'Restricted risk-only view'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'PUBLIC');
