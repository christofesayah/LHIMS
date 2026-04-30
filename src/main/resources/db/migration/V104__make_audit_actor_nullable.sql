-- This migration makes actor_user_id nullable in the audit_logs table.
-- This is necessary to support public registrations where no authenticated actor exists.

ALTER TABLE audit_logs ALTER COLUMN actor_user_id DROP NOT NULL;
