ALTER TABLE audit_logs
DROP CONSTRAINT IF EXISTS audit_logs_action_type_check;

ALTER TABLE audit_logs
ADD CONSTRAINT audit_logs_action_type_check
CHECK (action_type IN ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'COMPUTE', 'ASSIGN_ROLE', 'REVOKE_ROLE'));
