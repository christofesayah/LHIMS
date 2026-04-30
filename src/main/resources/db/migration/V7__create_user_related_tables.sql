-- This script creates user-related tables.
-- It assumes that the 'health_facilities' table already exists from a previous migration.

-- Create roles table to store user roles
CREATE TABLE IF NOT EXISTS roles (
    role_id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    role_name VARCHAR(255) NOT NULL,
    description TEXT
);

-- Create user_account table for storing user information
CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN,
    is_approved BOOLEAN,
    created_at TIMESTAMP,
    assigned_facility_id BIGINT,
    CONSTRAINT fk_assigned_facility
        FOREIGN KEY(assigned_facility_id)
        REFERENCES health_facilities(facility_id)
);

-- Create user_roles table to link users with their roles
CREATE TABLE IF NOT EXISTS user_roles (
    user_role_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    assigned_by_user_id BIGINT,
    CONSTRAINT fk_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_role FOREIGN KEY(role_id) REFERENCES roles(role_id),
    CONSTRAINT fk_assigned_by FOREIGN KEY(assigned_by_user_id) REFERENCES users(user_id)
);

-- Insert default roles into the roles table
INSERT INTO roles (code, role_name, description)
SELECT * FROM (VALUES
    ('MINISTRY_OFFICIAL', 'Ministry Official', 'User with ministry-level permissions'),
    ('HOSPITAL_ADMIN', 'Hospital Admin', 'User with hospital administration permissions'),
    ('PUBLIC', 'Public Viewer', 'User with public data viewing permissions')
) AS v(code, role_name, description)
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.code = v.code);