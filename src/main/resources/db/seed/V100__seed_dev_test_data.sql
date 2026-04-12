-- Seed data for local testing when profile flyway-seed is enabled.
-- Assumes tables already exist (JPA is schema owner).

INSERT INTO roles (code, role_name, description)
SELECT 'MINISTRY_OFFICIAL', 'Ministry Official', 'Full analytical access'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'MINISTRY_OFFICIAL');

INSERT INTO roles (code, role_name, description)
SELECT 'HOSPITAL_ADMIN', 'Hospital Admin', 'Operational district-level access'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'HOSPITAL_ADMIN');

INSERT INTO roles (code, role_name, description)
SELECT 'PUBLIC', 'Public Viewer', 'Restricted risk-only view'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'PUBLIC');

INSERT INTO regions (name, type, parent_region_id, population)
SELECT 'Beirut', 'CAZA', NULL, 350000
WHERE NOT EXISTS (SELECT 1 FROM regions WHERE name = 'Beirut');

INSERT INTO users (username, email, password_hash, is_active, created_at)
SELECT 'ministry.demo', 'ministry.demo@lhims.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjQ8xk8Sx7n0ANKPOuUMGAfJOyrUo6a', TRUE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'ministry.demo@lhims.local');

INSERT INTO user_roles (user_id, role_id, assigned_at, assigned_by_user_id)
SELECT u.user_id, r.role_id, NOW(), u.user_id
FROM users u
JOIN roles r ON r.code = 'MINISTRY_OFFICIAL'
WHERE u.email = 'ministry.demo@lhims.local'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.user_id AND ur.role_id = r.role_id
  );

INSERT INTO computed_scores (region_id, scenario_id, reporting_period, hai_score, rvi_score, ciri_score, risk_category, last_computed_at)
SELECT reg.region_id, NULL, '2026-Q1', 0.48, 0.61, 0.55, 'MEDIUM', NOW()
FROM regions reg
WHERE reg.name = 'Beirut'
  AND NOT EXISTS (
      SELECT 1 FROM computed_scores cs
      WHERE cs.region_id = reg.region_id AND cs.reporting_period = '2026-Q1' AND cs.scenario_id IS NULL
  );
