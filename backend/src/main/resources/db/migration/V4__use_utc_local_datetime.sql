-- Store application LocalDateTime values as UTC timestamp without time zone.
-- Convert existing instants explicitly in UTC; keep V1/V2 unchanged for Flyway checksums.

ALTER TABLE roles
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE permissions
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE role_permissions
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE users
    ALTER COLUMN last_login_at TYPE timestamp(6) without time zone USING (last_login_at AT TIME ZONE 'UTC'),
    ALTER COLUMN password_changed_at TYPE timestamp(6) without time zone USING (password_changed_at AT TIME ZONE 'UTC'),
    ALTER COLUMN locked_until TYPE timestamp(6) without time zone USING (locked_until AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE persons
    ALTER COLUMN deleted_at TYPE timestamp(6) without time zone USING (deleted_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE person_contacts
    ALTER COLUMN verified_at TYPE timestamp(6) without time zone USING (verified_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE person_consents
    ALTER COLUMN recorded_at DROP DEFAULT,
    ALTER COLUMN recorded_at TYPE timestamp(6) without time zone USING (recorded_at AT TIME ZONE 'UTC'),
    ALTER COLUMN recorded_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN revoked_at TYPE timestamp(6) without time zone USING (revoked_at AT TIME ZONE 'UTC');

ALTER TABLE pipelines
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE pipeline_stages
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE lead_sources
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE leads
    ALTER COLUMN first_contact_at TYPE timestamp(6) without time zone USING (first_contact_at AT TIME ZONE 'UTC'),
    ALTER COLUMN converted_at TYPE timestamp(6) without time zone USING (converted_at AT TIME ZONE 'UTC'),
    ALTER COLUMN deleted_at TYPE timestamp(6) without time zone USING (deleted_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE customers
    ALTER COLUMN became_customer_at DROP DEFAULT,
    ALTER COLUMN became_customer_at TYPE timestamp(6) without time zone USING (became_customer_at AT TIME ZONE 'UTC'),
    ALTER COLUMN became_customer_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE lead_conversions
    ALTER COLUMN converted_at DROP DEFAULT,
    ALTER COLUMN converted_at TYPE timestamp(6) without time zone USING (converted_at AT TIME ZONE 'UTC'),
    ALTER COLUMN converted_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE student_profiles
    ALTER COLUMN opened_at DROP DEFAULT,
    ALTER COLUMN opened_at TYPE timestamp(6) without time zone USING (opened_at AT TIME ZONE 'UTC'),
    ALTER COLUMN opened_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN closed_at TYPE timestamp(6) without time zone USING (closed_at AT TIME ZONE 'UTC'),
    ALTER COLUMN deleted_at TYPE timestamp(6) without time zone USING (deleted_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE student_profile_status_history
    ALTER COLUMN changed_at DROP DEFAULT,
    ALTER COLUMN changed_at TYPE timestamp(6) without time zone USING (changed_at AT TIME ZONE 'UTC'),
    ALTER COLUMN changed_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE schools
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE applications
    ALTER COLUMN submitted_at TYPE timestamp(6) without time zone USING (submitted_at AT TIME ZONE 'UTC'),
    ALTER COLUMN decision_at TYPE timestamp(6) without time zone USING (decision_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE application_status_history
    ALTER COLUMN changed_at DROP DEFAULT,
    ALTER COLUMN changed_at TYPE timestamp(6) without time zone USING (changed_at AT TIME ZONE 'UTC'),
    ALTER COLUMN changed_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE activities
    ALTER COLUMN occurred_at DROP DEFAULT,
    ALTER COLUMN occurred_at TYPE timestamp(6) without time zone USING (occurred_at AT TIME ZONE 'UTC'),
    ALTER COLUMN occurred_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE tasks
    ALTER COLUMN due_at TYPE timestamp(6) without time zone USING (due_at AT TIME ZONE 'UTC'),
    ALTER COLUMN completed_at TYPE timestamp(6) without time zone USING (completed_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE document_types
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE documents
    ALTER COLUMN reviewed_at TYPE timestamp(6) without time zone USING (reviewed_at AT TIME ZONE 'UTC'),
    ALTER COLUMN uploaded_at DROP DEFAULT,
    ALTER COLUMN uploaded_at TYPE timestamp(6) without time zone USING (uploaded_at AT TIME ZONE 'UTC'),
    ALTER COLUMN uploaded_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE contracts
    ALTER COLUMN signed_at TYPE timestamp(6) without time zone USING (signed_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE invoices
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE invoice_items
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE installments
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE payment_transactions
    ALTER COLUMN received_at TYPE timestamp(6) without time zone USING (received_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at DROP DEFAULT,
    ALTER COLUMN updated_at TYPE timestamp(6) without time zone USING (updated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN updated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE payment_allocations
    ALTER COLUMN allocated_at DROP DEFAULT,
    ALTER COLUMN allocated_at TYPE timestamp(6) without time zone USING (allocated_at AT TIME ZONE 'UTC'),
    ALTER COLUMN allocated_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE stage_history
    ALTER COLUMN changed_at DROP DEFAULT,
    ALTER COLUMN changed_at TYPE timestamp(6) without time zone USING (changed_at AT TIME ZONE 'UTC'),
    ALTER COLUMN changed_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE lead_status_history
    ALTER COLUMN changed_at DROP DEFAULT,
    ALTER COLUMN changed_at TYPE timestamp(6) without time zone USING (changed_at AT TIME ZONE 'UTC'),
    ALTER COLUMN changed_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE audit_events
    ALTER COLUMN occurred_at DROP DEFAULT,
    ALTER COLUMN occurred_at TYPE timestamp(6) without time zone USING (occurred_at AT TIME ZONE 'UTC'),
    ALTER COLUMN occurred_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE refresh_token
    ALTER COLUMN expires_at TYPE timestamp(6) without time zone USING (expires_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN created_at TYPE timestamp(6) without time zone USING (created_at AT TIME ZONE 'UTC'),
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'UTC');

ALTER TABLE invalidated_token
    ALTER COLUMN expiry_time TYPE timestamp(6) without time zone USING (expiry_time AT TIME ZONE 'UTC');

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = now() AT TIME ZONE 'UTC';
    RETURN NEW;
END;
$$;
