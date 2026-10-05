-- Initial schema for the single-organization Korean Pathway CRM.
-- PostgreSQL 14+ (gen_random_uuid is available in core).

-- ============================================================
-- ACCESS CONTROL
-- ============================================================

CREATE TABLE roles (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code            varchar(80) NOT NULL UNIQUE,
    name            varchar(100) NOT NULL,
    description     varchar(500),
    is_system       boolean NOT NULL DEFAULT false,
    status          varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_roles_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE permissions (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code            varchar(100) NOT NULL UNIQUE,
    name            varchar(150) NOT NULL,
    description     varchar(500),
    module          varchar(80),
    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE role_permissions (
    role_id         uuid NOT NULL REFERENCES roles(id),
    permission_id   uuid NOT NULL REFERENCES permissions(id),
    created_at      timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX ix_role_permissions_permission_id ON role_permissions(permission_id);

CREATE TABLE users (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    username            varchar(100) NOT NULL,
    email               varchar(255) NOT NULL,
    password_hash       varchar(255) NOT NULL,
    full_name           varchar(200) NOT NULL,
    phone               varchar(50),
    role_id             uuid NOT NULL REFERENCES roles(id),
    is_active           boolean NOT NULL DEFAULT true,
    last_login_at       timestamptz,
    password_changed_at timestamptz,
    failed_login_count  integer NOT NULL DEFAULT 0,
    locked_until        timestamptz,
    created_by          uuid REFERENCES users(id),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_by          uuid REFERENCES users(id),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_failed_login_count CHECK (failed_login_count >= 0)
);

-- Case-insensitive uniqueness avoids accounts differing only by letter case.
CREATE UNIQUE INDEX uq_users_username_lower ON users (lower(username));
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));
CREATE INDEX ix_users_role_id ON users(role_id);

-- ============================================================
-- PERSONS, CONTACTS, AND COMMUNICATION CONSENT
-- ============================================================

CREATE TABLE persons (
    id                          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name                   varchar(200) NOT NULL,
    date_of_birth               date,
    gender                      varchar(30),
    nationality                 varchar(100),
    identity_number_encrypted   text,
    identity_number_last4       varchar(4),
    notes                       text,
    deleted_at                  timestamptz,
    created_by                  uuid REFERENCES users(id),
    created_at                  timestamptz NOT NULL DEFAULT now(),
    updated_by                  uuid REFERENCES users(id),
    updated_at                  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_persons_full_name ON persons(full_name);

CREATE TABLE person_contacts (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id           uuid NOT NULL REFERENCES persons(id),
    contact_type        varchar(30) NOT NULL,
    value               varchar(255) NOT NULL,
    normalized_value    varchar(255) NOT NULL,
    label               varchar(80),
    is_primary          boolean NOT NULL DEFAULT false,
    is_verified         boolean NOT NULL DEFAULT false,
    verified_at         timestamptz,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_person_contacts_type
        CHECK (contact_type IN ('PHONE', 'EMAIL', 'ZALO', 'KAKAO', 'OTHER')),
    CONSTRAINT ck_person_contacts_verified_at
        CHECK (NOT is_verified OR verified_at IS NOT NULL),
    CONSTRAINT uq_person_contact_value
        UNIQUE (person_id, contact_type, normalized_value)
);

CREATE INDEX ix_person_contacts_lookup
    ON person_contacts(contact_type, normalized_value);
CREATE UNIQUE INDEX uq_person_primary_contact_type
    ON person_contacts(person_id, contact_type)
    WHERE is_primary;

CREATE TABLE person_consents (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id       uuid NOT NULL REFERENCES persons(id),
    purpose         varchar(80) NOT NULL,
    channel         varchar(30) NOT NULL,
    is_granted      boolean NOT NULL,
    recorded_at     timestamptz NOT NULL DEFAULT now(),
    revoked_at      timestamptz,
    recorded_by     uuid REFERENCES users(id),
    notes           varchar(500),
    CONSTRAINT ck_person_consents_channel
        CHECK (channel IN ('PHONE', 'EMAIL', 'SMS', 'ZALO', 'KAKAO', 'OTHER')),
    CONSTRAINT ck_person_consents_revoked_at
        CHECK (revoked_at IS NULL OR revoked_at >= recorded_at)
);

CREATE INDEX ix_person_consents_person_purpose
    ON person_consents(person_id, purpose, channel, recorded_at DESC);

-- ============================================================
-- PIPELINES, STAGES, AND LEAD SOURCES
-- ============================================================

CREATE TABLE pipelines (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code            varchar(80) NOT NULL UNIQUE,
    name            varchar(150) NOT NULL,
    entity_type     varchar(30) NOT NULL,
    is_default      boolean NOT NULL DEFAULT false,
    status          varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_pipelines_entity_type
        CHECK (entity_type IN ('LEAD', 'STUDENT_PROFILE')),
    CONSTRAINT ck_pipelines_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uq_pipelines_default_per_entity
    ON pipelines(entity_type)
    WHERE is_default AND status = 'ACTIVE';

CREATE TABLE pipeline_stages (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    pipeline_id     uuid NOT NULL REFERENCES pipelines(id),
    code            varchar(80) NOT NULL,
    name            varchar(150) NOT NULL,
    order_index     integer NOT NULL,
    category        varchar(30) NOT NULL,
    is_initial      boolean NOT NULL DEFAULT false,
    is_terminal     boolean NOT NULL DEFAULT false,
    color           varchar(20),
    status          varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_pipeline_stage_code UNIQUE (pipeline_id, code),
    CONSTRAINT uq_pipeline_stage_order UNIQUE (pipeline_id, order_index),
    CONSTRAINT uq_pipeline_stage_id_pair UNIQUE (pipeline_id, id),
    CONSTRAINT ck_pipeline_stages_order CHECK (order_index >= 0),
    CONSTRAINT ck_pipeline_stages_category
        CHECK (category IN ('OPEN', 'WON', 'LOST')),
    CONSTRAINT ck_pipeline_stages_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uq_pipeline_initial_stage
    ON pipeline_stages(pipeline_id)
    WHERE is_initial AND status = 'ACTIVE';
CREATE INDEX ix_pipeline_stages_pipeline_order
    ON pipeline_stages(pipeline_id, order_index);

CREATE TABLE lead_sources (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code            varchar(80) NOT NULL UNIQUE,
    name            varchar(150) NOT NULL,
    status          varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_lead_sources_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- ============================================================
-- LEADS, CUSTOMERS, AND CONVERSION HISTORY
-- ============================================================

CREATE TABLE leads (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id           uuid NOT NULL REFERENCES persons(id),
    source_id           uuid REFERENCES lead_sources(id),
    pipeline_id         uuid NOT NULL,
    stage_id            uuid NOT NULL,
    assigned_to         uuid REFERENCES users(id),
    status              varchar(20) NOT NULL DEFAULT 'OPEN',
    campaign_name       varchar(150),
    estimated_budget    numeric(14,2),
    currency_code       char(3) NOT NULL DEFAULT 'VND',
    lost_reason         varchar(500),
    first_contact_at    timestamptz,
    converted_at        timestamptz,
    deleted_at          timestamptz,
    created_by          uuid REFERENCES users(id),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_by          uuid REFERENCES users(id),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_leads_pipeline_stage
        FOREIGN KEY (pipeline_id, stage_id)
        REFERENCES pipeline_stages(pipeline_id, id),
    CONSTRAINT ck_leads_status
        CHECK (status IN ('OPEN', 'CONVERTED', 'LOST', 'ARCHIVED')),
    CONSTRAINT ck_leads_budget CHECK (estimated_budget IS NULL OR estimated_budget >= 0),
    CONSTRAINT ck_leads_currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_leads_converted_at
        CHECK ((status = 'CONVERTED') = (converted_at IS NOT NULL)),
    CONSTRAINT ck_leads_lost_reason
        CHECK (status <> 'LOST' OR lost_reason IS NOT NULL)
);

CREATE INDEX ix_leads_status_created ON leads(status, created_at DESC);
CREATE INDEX ix_leads_assigned_status ON leads(assigned_to, status);
CREATE INDEX ix_leads_pipeline_stage_status ON leads(pipeline_id, stage_id, status);
CREATE INDEX ix_leads_person ON leads(person_id);
CREATE INDEX ix_leads_source ON leads(source_id);
CREATE UNIQUE INDEX uq_leads_one_open_per_person
    ON leads(person_id)
    WHERE status = 'OPEN' AND deleted_at IS NULL;

CREATE TABLE customers (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    person_id           uuid NOT NULL UNIQUE REFERENCES persons(id),
    customer_number     varchar(50) NOT NULL UNIQUE,
    status              varchar(20) NOT NULL DEFAULT 'ACTIVE',
    became_customer_at  timestamptz NOT NULL DEFAULT now(),
    created_by          uuid REFERENCES users(id),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_by          uuid REFERENCES users(id),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_customers_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'))
);

CREATE INDEX ix_customers_status ON customers(status);

-- A lead can convert once; a customer can have several original leads.
CREATE TABLE lead_conversions (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id             uuid NOT NULL UNIQUE REFERENCES leads(id),
    customer_id         uuid NOT NULL REFERENCES customers(id),
    converted_by        uuid REFERENCES users(id),
    converted_at        timestamptz NOT NULL DEFAULT now(),
    conversion_notes    text
);

CREATE INDEX ix_lead_conversions_customer ON lead_conversions(customer_id);

-- Keep lead and customer identity consistent on conversion.
CREATE FUNCTION validate_lead_conversion_person() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM leads l
        JOIN customers c ON c.id = NEW.customer_id
        WHERE l.id = NEW.lead_id
          AND l.person_id = c.person_id
    ) THEN
        RAISE EXCEPTION 'Converted lead and customer must reference the same person';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validate_lead_conversion_person
BEFORE INSERT OR UPDATE ON lead_conversions
FOR EACH ROW EXECUTE FUNCTION validate_lead_conversion_person();

-- ============================================================
-- STUDENT PROFILES, SCHOOLS, AND APPLICATIONS
-- ============================================================

CREATE TABLE visa_types (
    code            varchar(30) PRIMARY KEY,
    name            varchar(150) NOT NULL,
    description     varchar(500),
    status          varchar(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT ck_visa_types_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE student_profiles (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id         uuid NOT NULL REFERENCES customers(id),
    profile_number      varchar(50) NOT NULL UNIQUE,
    pipeline_id         uuid NOT NULL,
    stage_id            uuid NOT NULL,
    counselor_id        uuid REFERENCES users(id),
    target_degree       varchar(100),
    target_major        varchar(150),
    target_intake       varchar(50),
    visa_type_code      varchar(30) REFERENCES visa_types(code),
    service_fee         numeric(14,2),
    tuition_estimate    numeric(14,2),
    translation_fee     numeric(14,2),
    currency_code       char(3) NOT NULL DEFAULT 'VND',
    status              varchar(20) NOT NULL DEFAULT 'OPEN',
    opened_at           timestamptz NOT NULL DEFAULT now(),
    closed_at           timestamptz,
    deleted_at          timestamptz,
    created_by          uuid REFERENCES users(id),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_by          uuid REFERENCES users(id),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_student_profiles_pipeline_stage
        FOREIGN KEY (pipeline_id, stage_id)
        REFERENCES pipeline_stages(pipeline_id, id),
    CONSTRAINT ck_student_profiles_status
        CHECK (status IN ('OPEN', 'ON_HOLD', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_student_profiles_fees
        CHECK (
            (service_fee IS NULL OR service_fee >= 0) AND
            (tuition_estimate IS NULL OR tuition_estimate >= 0) AND
            (translation_fee IS NULL OR translation_fee >= 0)
        ),
    CONSTRAINT ck_student_profiles_currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_student_profiles_closed_at
        CHECK (status NOT IN ('COMPLETED', 'CANCELLED') OR closed_at IS NOT NULL)
);

CREATE INDEX ix_student_profiles_customer_status ON student_profiles(customer_id, status);
CREATE INDEX ix_student_profiles_counselor_status ON student_profiles(counselor_id, status);
CREATE INDEX ix_student_profiles_pipeline_stage ON student_profiles(pipeline_id, stage_id, status);

CREATE TABLE student_profile_status_history (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    student_profile_id  uuid NOT NULL REFERENCES student_profiles(id),
    from_status         varchar(20),
    to_status           varchar(20) NOT NULL,
    changed_by          uuid REFERENCES users(id),
    changed_at          timestamptz NOT NULL DEFAULT now(),
    reason              varchar(1000),
    CONSTRAINT ck_profile_status_history_from
        CHECK (from_status IS NULL OR from_status IN ('OPEN', 'ON_HOLD', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_profile_status_history_to
        CHECK (to_status IN ('OPEN', 'ON_HOLD', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX ix_profile_status_history_time
    ON student_profile_status_history(student_profile_id, changed_at DESC);

CREATE TABLE schools (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code            varchar(80) UNIQUE,
    name            varchar(200) NOT NULL,
    name_local      varchar(200),
    country_code    char(2) NOT NULL DEFAULT 'KR',
    city            varchar(120),
    website         varchar(500),
    status          varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_schools_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_schools_country_code CHECK (country_code ~ '^[A-Z]{2}$')
);

CREATE INDEX ix_schools_country_name ON schools(country_code, name);

CREATE TABLE applications (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    student_profile_id  uuid NOT NULL REFERENCES student_profiles(id),
    school_id           uuid NOT NULL REFERENCES schools(id),
    program_name        varchar(200),
    degree_level        varchar(100),
    intake              varchar(50),
    application_number  varchar(100),
    status              varchar(30) NOT NULL DEFAULT 'PLANNING',
    submitted_at        timestamptz,
    decision_at         timestamptz,
    decision_notes      text,
    created_by          uuid REFERENCES users(id),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_by          uuid REFERENCES users(id),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_applications_status CHECK (
        status IN ('PLANNING', 'PREPARING', 'SUBMITTED', 'UNDER_REVIEW',
                   'ACCEPTED', 'REJECTED', 'WITHDRAWN')
    ),
    CONSTRAINT ck_applications_submitted_at
        CHECK (status NOT IN ('SUBMITTED', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED')
               OR submitted_at IS NOT NULL),
    CONSTRAINT ck_applications_decision_at
        CHECK (status NOT IN ('ACCEPTED', 'REJECTED') OR decision_at IS NOT NULL)
);

-- COALESCE treats a NULL program name as the same program for duplicate checks.
CREATE UNIQUE INDEX uq_applications_profile_school_program_intake
    ON applications(
        student_profile_id,
        school_id,
        coalesce(program_name, ''),
        coalesce(intake, '')
    );
CREATE UNIQUE INDEX uq_applications_application_number
    ON applications(application_number)
    WHERE application_number IS NOT NULL;
CREATE INDEX ix_applications_status_submitted ON applications(status, submitted_at);

CREATE TABLE application_status_history (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  uuid NOT NULL REFERENCES applications(id),
    from_status     varchar(30),
    to_status       varchar(30) NOT NULL,
    changed_by      uuid REFERENCES users(id),
    changed_at      timestamptz NOT NULL DEFAULT now(),
    reason          varchar(1000),
    CONSTRAINT ck_application_status_history_from CHECK (
        from_status IS NULL OR from_status IN ('PLANNING', 'PREPARING', 'SUBMITTED',
                                               'UNDER_REVIEW', 'ACCEPTED', 'REJECTED', 'WITHDRAWN')
    ),
    CONSTRAINT ck_application_status_history_to CHECK (
        to_status IN ('PLANNING', 'PREPARING', 'SUBMITTED', 'UNDER_REVIEW',
                      'ACCEPTED', 'REJECTED', 'WITHDRAWN')
    )
);

CREATE INDEX ix_application_status_history_time
    ON application_status_history(application_id, changed_at DESC);

-- ============================================================
-- ACTIVITIES AND FOLLOW-UP TASKS
-- ============================================================

CREATE TABLE activities (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id             uuid REFERENCES leads(id),
    customer_id         uuid REFERENCES customers(id),
    student_profile_id  uuid REFERENCES student_profiles(id),
    owner_id            uuid REFERENCES users(id),
    activity_type       varchar(30) NOT NULL,
    subject             varchar(200),
    summary             text,
    occurred_at         timestamptz NOT NULL DEFAULT now(),
    duration_minutes    integer,
    outcome             varchar(100),
    created_by          uuid REFERENCES users(id),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_by          uuid REFERENCES users(id),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_activities_one_target
        CHECK (num_nonnulls(lead_id, customer_id, student_profile_id) = 1),
    CONSTRAINT ck_activities_type
        CHECK (activity_type IN ('CALL', 'MEETING', 'CHAT', 'EMAIL', 'NOTE', 'OTHER')),
    CONSTRAINT ck_activities_duration
        CHECK (duration_minutes IS NULL OR duration_minutes >= 0)
);

CREATE INDEX ix_activities_lead_time ON activities(lead_id, occurred_at DESC);
CREATE INDEX ix_activities_customer_time ON activities(customer_id, occurred_at DESC);
CREATE INDEX ix_activities_profile_time ON activities(student_profile_id, occurred_at DESC);
CREATE INDEX ix_activities_owner_time ON activities(owner_id, occurred_at DESC);

CREATE TABLE tasks (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id             uuid REFERENCES leads(id),
    customer_id         uuid REFERENCES customers(id),
    student_profile_id  uuid REFERENCES student_profiles(id),
    assigned_to         uuid REFERENCES users(id),
    title               varchar(200) NOT NULL,
    description         text,
    priority            varchar(20) NOT NULL DEFAULT 'NORMAL',
    status              varchar(20) NOT NULL DEFAULT 'OPEN',
    due_at              timestamptz,
    completed_at        timestamptz,
    created_by          uuid REFERENCES users(id),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_by          uuid REFERENCES users(id),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_tasks_one_target
        CHECK (num_nonnulls(lead_id, customer_id, student_profile_id) = 1),
    CONSTRAINT ck_tasks_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    CONSTRAINT ck_tasks_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    CONSTRAINT ck_tasks_completed_at
        CHECK ((status = 'DONE') = (completed_at IS NOT NULL))
);

CREATE INDEX ix_tasks_assignee_status_due ON tasks(assigned_to, status, due_at);
CREATE INDEX ix_tasks_lead_status ON tasks(lead_id, status);
CREATE INDEX ix_tasks_customer_status ON tasks(customer_id, status);
CREATE INDEX ix_tasks_profile_status ON tasks(student_profile_id, status);

-- ============================================================
-- DOCUMENT TYPES AND UPLOADED FILE VERSIONS
-- ============================================================

CREATE TABLE document_types (
    id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code                    varchar(80) NOT NULL UNIQUE,
    name                    varchar(150) NOT NULL,
    applies_to              varchar(30) NOT NULL,
    is_required             boolean NOT NULL DEFAULT false,
    default_validity_days   integer,
    status                  varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_document_types_applies_to
        CHECK (applies_to IN ('STUDENT_PROFILE', 'APPLICATION')),
    CONSTRAINT ck_document_types_validity
        CHECK (default_validity_days IS NULL OR default_validity_days >= 0),
    CONSTRAINT ck_document_types_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE documents (
    id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    student_profile_id      uuid REFERENCES student_profiles(id),
    application_id          uuid REFERENCES applications(id),
    document_type_id        uuid NOT NULL REFERENCES document_types(id),
    version_no              integer NOT NULL DEFAULT 1,
    file_name               varchar(255),
    storage_key             varchar(1000),
    mime_type               varchar(150),
    file_size_bytes         bigint,
    checksum_sha256         varchar(64),
    status                  varchar(30) NOT NULL DEFAULT 'UPLOADED',
    issued_at               date,
    expires_at              date,
    reviewed_at             timestamptz,
    reviewed_by             uuid REFERENCES users(id),
    rejection_reason        varchar(1000),
    uploaded_by             uuid REFERENCES users(id),
    uploaded_at             timestamptz NOT NULL DEFAULT now(),
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_by              uuid REFERENCES users(id),
    updated_at              timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_documents_one_target
        CHECK (num_nonnulls(student_profile_id, application_id) = 1),
    CONSTRAINT ck_documents_version CHECK (version_no > 0),
    CONSTRAINT ck_documents_file_size
        CHECK (file_size_bytes IS NULL OR file_size_bytes >= 0),
    CONSTRAINT ck_documents_status
        CHECK (status IN ('REQUESTED', 'UPLOADED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT ck_documents_file_presence
        CHECK (status = 'REQUESTED' OR (file_name IS NOT NULL AND storage_key IS NOT NULL)),
    CONSTRAINT ck_documents_expiry
        CHECK (issued_at IS NULL OR expires_at IS NULL OR expires_at >= issued_at),
    CONSTRAINT ck_documents_reviewed_at
        CHECK (status NOT IN ('APPROVED', 'REJECTED') OR reviewed_at IS NOT NULL),
    CONSTRAINT ck_documents_rejection_reason
        CHECK (status <> 'REJECTED' OR rejection_reason IS NOT NULL)
);

CREATE UNIQUE INDEX uq_documents_profile_type_version
    ON documents(student_profile_id, document_type_id, version_no)
    WHERE student_profile_id IS NOT NULL;
CREATE UNIQUE INDEX uq_documents_application_type_version
    ON documents(application_id, document_type_id, version_no)
    WHERE application_id IS NOT NULL;
CREATE INDEX ix_documents_status_expiry ON documents(status, expires_at);

-- ============================================================
-- CONTRACTS, INVOICES, INSTALLMENTS, AND MONEY TRANSACTIONS
-- ============================================================

CREATE TABLE contracts (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id         uuid NOT NULL REFERENCES customers(id),
    student_profile_id  uuid REFERENCES student_profiles(id),
    contract_number     varchar(80) NOT NULL UNIQUE,
    status              varchar(30) NOT NULL DEFAULT 'DRAFT',
    signed_at           timestamptz,
    effective_at        date,
    expires_at          date,
    total_amount        numeric(14,2),
    currency_code       char(3) NOT NULL DEFAULT 'VND',
    file_storage_key    varchar(1000),
    notes               text,
    created_by          uuid REFERENCES users(id),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_by          uuid REFERENCES users(id),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_contracts_status
        CHECK (status IN ('DRAFT', 'SENT', 'SIGNED', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'TERMINATED')),
    CONSTRAINT ck_contracts_amount CHECK (total_amount IS NULL OR total_amount >= 0),
    CONSTRAINT ck_contracts_currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_contracts_dates
        CHECK (effective_at IS NULL OR expires_at IS NULL OR expires_at >= effective_at),
    CONSTRAINT ck_contracts_signed_at CHECK (status NOT IN ('SIGNED', 'ACTIVE', 'COMPLETED') OR signed_at IS NOT NULL)
);

CREATE INDEX ix_contracts_customer_status ON contracts(customer_id, status);
CREATE INDEX ix_contracts_profile ON contracts(student_profile_id);

CREATE TABLE invoices (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id         uuid NOT NULL REFERENCES customers(id),
    student_profile_id  uuid REFERENCES student_profiles(id),
    contract_id         uuid REFERENCES contracts(id),
    invoice_number      varchar(80) NOT NULL UNIQUE,
    status              varchar(30) NOT NULL DEFAULT 'DRAFT',
    issue_date          date,
    due_date            date,
    currency_code       char(3) NOT NULL DEFAULT 'VND',
    subtotal            numeric(14,2) NOT NULL DEFAULT 0,
    discount_amount     numeric(14,2) NOT NULL DEFAULT 0,
    tax_amount          numeric(14,2) NOT NULL DEFAULT 0,
    total_amount        numeric(14,2) NOT NULL DEFAULT 0,
    notes               text,
    created_by          uuid REFERENCES users(id),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_by          uuid REFERENCES users(id),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_invoices_status
        CHECK (status IN ('DRAFT', 'ISSUED', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'VOID')),
    CONSTRAINT ck_invoices_amounts
        CHECK (subtotal >= 0 AND discount_amount >= 0 AND tax_amount >= 0 AND total_amount >= 0),
    CONSTRAINT ck_invoices_currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_invoices_dates CHECK (issue_date IS NULL OR due_date IS NULL OR due_date >= issue_date)
);

CREATE INDEX ix_invoices_status_due ON invoices(status, due_date);
CREATE INDEX ix_invoices_customer ON invoices(customer_id);
CREATE INDEX ix_invoices_profile ON invoices(student_profile_id);
CREATE INDEX ix_invoices_contract ON invoices(contract_id);

CREATE TABLE invoice_items (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id      uuid NOT NULL REFERENCES invoices(id),
    description     varchar(500) NOT NULL,
    quantity        numeric(12,3) NOT NULL DEFAULT 1,
    unit_price      numeric(14,2) NOT NULL,
    amount          numeric(14,2) NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_invoice_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_invoice_items_amounts CHECK (unit_price >= 0 AND amount >= 0)
);

CREATE INDEX ix_invoice_items_invoice ON invoice_items(invoice_id);

CREATE TABLE installments (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id          uuid NOT NULL REFERENCES invoices(id),
    installment_number  integer NOT NULL,
    title               varchar(200) NOT NULL,
    amount              numeric(14,2) NOT NULL,
    due_date            date NOT NULL,
    status              varchar(30) NOT NULL DEFAULT 'PENDING',
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_installments_number UNIQUE (invoice_id, installment_number),
    CONSTRAINT ck_installments_number CHECK (installment_number > 0),
    CONSTRAINT ck_installments_amount CHECK (amount > 0),
    CONSTRAINT ck_installments_status
        CHECK (status IN ('PENDING', 'PARTIALLY_PAID', 'PAID', 'WAIVED', 'CANCELLED'))
);

CREATE INDEX ix_installments_status_due ON installments(status, due_date);

CREATE TABLE payment_transactions (
    id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id             uuid NOT NULL REFERENCES customers(id),
    transaction_number      varchar(100) NOT NULL UNIQUE,
    transaction_type        varchar(20) NOT NULL,
    status                  varchar(20) NOT NULL DEFAULT 'PENDING',
    amount                  numeric(14,2) NOT NULL,
    currency_code           char(3) NOT NULL DEFAULT 'VND',
    payment_method          varchar(30),
    provider_reference      varchar(200),
    related_transaction_id  uuid REFERENCES payment_transactions(id),
    received_at             timestamptz,
    recorded_by             uuid REFERENCES users(id),
    notes                   text,
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_payment_transactions_type
        CHECK (transaction_type IN ('PAYMENT', 'REFUND', 'REVERSAL')),
    CONSTRAINT ck_payment_transactions_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'FAILED', 'VOID')),
    CONSTRAINT ck_payment_transactions_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_transactions_currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_payment_transactions_method
        CHECK (payment_method IS NULL OR payment_method IN ('CASH', 'BANK_TRANSFER', 'CARD', 'ONLINE', 'OTHER')),
    CONSTRAINT ck_payment_transactions_related
        CHECK ((transaction_type = 'PAYMENT') = (related_transaction_id IS NULL)),
    CONSTRAINT ck_payment_transactions_received_at
        CHECK (status <> 'CONFIRMED' OR received_at IS NOT NULL)
);

CREATE UNIQUE INDEX uq_payment_provider_reference
    ON payment_transactions(provider_reference)
    WHERE provider_reference IS NOT NULL;
CREATE INDEX ix_payment_transactions_customer_received
    ON payment_transactions(customer_id, received_at DESC);
CREATE INDEX ix_payment_transactions_status_created
    ON payment_transactions(status, created_at DESC);

CREATE FUNCTION validate_related_payment_transaction() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    original_type     varchar(20);
    original_status   varchar(20);
    original_customer uuid;
    original_currency char(3);
BEGIN
    IF NEW.transaction_type = 'PAYMENT' THEN
        RETURN NEW;
    END IF;

    SELECT transaction_type, status, customer_id, currency_code
      INTO original_type, original_status, original_customer, original_currency
    FROM payment_transactions
    WHERE id = NEW.related_transaction_id;

    IF original_type IS DISTINCT FROM 'PAYMENT'
       OR original_status IS DISTINCT FROM 'CONFIRMED'
       OR original_customer IS DISTINCT FROM NEW.customer_id
       OR original_currency IS DISTINCT FROM NEW.currency_code THEN
        RAISE EXCEPTION 'Refund/reversal must reference a confirmed payment for the same customer and currency';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_related_payment_transaction
BEFORE INSERT OR UPDATE OF transaction_type, related_transaction_id, customer_id, currency_code
ON payment_transactions
FOR EACH ROW EXECUTE FUNCTION validate_related_payment_transaction();

CREATE TABLE payment_allocations (
    id                          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_transaction_id      uuid NOT NULL REFERENCES payment_transactions(id),
    installment_id              uuid NOT NULL REFERENCES installments(id),
    amount                      numeric(14,2) NOT NULL,
    allocated_at                timestamptz NOT NULL DEFAULT now(),
    allocated_by                uuid REFERENCES users(id),
    CONSTRAINT uq_payment_allocation UNIQUE (payment_transaction_id, installment_id),
    CONSTRAINT ck_payment_allocations_amount CHECK (amount > 0)
);

CREATE INDEX ix_payment_allocations_installment ON payment_allocations(installment_id);

-- ============================================================
-- STAGE HISTORY, LEAD STATUS HISTORY, AND AUDIT EVENTS
-- ============================================================

CREATE TABLE stage_history (
    id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    pipeline_id             uuid NOT NULL REFERENCES pipelines(id),
    lead_id                 uuid REFERENCES leads(id),
    student_profile_id      uuid REFERENCES student_profiles(id),
    from_stage_id           uuid,
    to_stage_id             uuid NOT NULL,
    changed_by              uuid REFERENCES users(id),
    changed_at              timestamptz NOT NULL DEFAULT now(),
    reason                  varchar(1000),
    CONSTRAINT ck_stage_history_one_target
        CHECK (num_nonnulls(lead_id, student_profile_id) = 1),
    CONSTRAINT fk_stage_history_from_stage
        FOREIGN KEY (pipeline_id, from_stage_id)
        REFERENCES pipeline_stages(pipeline_id, id),
    CONSTRAINT fk_stage_history_to_stage
        FOREIGN KEY (pipeline_id, to_stage_id)
        REFERENCES pipeline_stages(pipeline_id, id)
);

CREATE INDEX ix_stage_history_lead_time ON stage_history(lead_id, changed_at DESC);
CREATE INDEX ix_stage_history_profile_time ON stage_history(student_profile_id, changed_at DESC);

CREATE TABLE lead_status_history (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id         uuid NOT NULL REFERENCES leads(id),
    from_status     varchar(20),
    to_status       varchar(20) NOT NULL,
    changed_by      uuid REFERENCES users(id),
    changed_at      timestamptz NOT NULL DEFAULT now(),
    reason          varchar(1000),
    CONSTRAINT ck_lead_status_history_from
        CHECK (from_status IS NULL OR from_status IN ('OPEN', 'CONVERTED', 'LOST', 'ARCHIVED')),
    CONSTRAINT ck_lead_status_history_to
        CHECK (to_status IN ('OPEN', 'CONVERTED', 'LOST', 'ARCHIVED'))
);

CREATE INDEX ix_lead_status_history_lead_time
    ON lead_status_history(lead_id, changed_at DESC);

CREATE TABLE audit_events (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id   uuid REFERENCES users(id),
    action          varchar(100) NOT NULL,
    entity_type     varchar(100) NOT NULL,
    entity_id       uuid,
    occurred_at     timestamptz NOT NULL DEFAULT now(),
    ip_address      inet,
    user_agent      varchar(1000),
    changes         jsonb
);

CREATE INDEX ix_audit_events_actor_time ON audit_events(actor_user_id, occurred_at DESC);
CREATE INDEX ix_audit_events_entity_time ON audit_events(entity_type, entity_id, occurred_at DESC);
CREATE INDEX ix_audit_events_time ON audit_events(occurred_at DESC);

-- ============================================================
-- CROSS-TABLE BUSINESS INVARIANTS
-- ============================================================

CREATE FUNCTION validate_pipeline_entity_type() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    expected_type varchar(30);
    actual_type   varchar(30);
BEGIN
    IF TG_TABLE_NAME = 'leads' THEN
        expected_type := 'LEAD';
    ELSIF TG_TABLE_NAME = 'student_profiles' THEN
        expected_type := 'STUDENT_PROFILE';
    ELSIF TG_TABLE_NAME = 'stage_history' THEN
        IF NEW.lead_id IS NOT NULL THEN
            expected_type := 'LEAD';
        ELSE
            expected_type := 'STUDENT_PROFILE';
        END IF;
    ELSE
        RAISE EXCEPTION 'Unexpected table for pipeline validation: %', TG_TABLE_NAME;
    END IF;

    SELECT entity_type INTO actual_type FROM pipelines WHERE id = NEW.pipeline_id;
    IF actual_type IS DISTINCT FROM expected_type THEN
        RAISE EXCEPTION 'Pipeline % has entity type %, expected %',
            NEW.pipeline_id, actual_type, expected_type;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_leads_pipeline_entity_type
BEFORE INSERT OR UPDATE OF pipeline_id ON leads
FOR EACH ROW EXECUTE FUNCTION validate_pipeline_entity_type();

CREATE TRIGGER trg_student_profiles_pipeline_entity_type
BEFORE INSERT OR UPDATE OF pipeline_id ON student_profiles
FOR EACH ROW EXECUTE FUNCTION validate_pipeline_entity_type();

CREATE TRIGGER trg_stage_history_pipeline_entity_type
BEFORE INSERT OR UPDATE OF pipeline_id, lead_id, student_profile_id ON stage_history
FOR EACH ROW EXECUTE FUNCTION validate_pipeline_entity_type();

CREATE FUNCTION validate_document_target_type() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    expected_target varchar(30);
BEGIN
    SELECT applies_to INTO expected_target
    FROM document_types
    WHERE id = NEW.document_type_id;

    IF expected_target = 'STUDENT_PROFILE' AND NEW.student_profile_id IS NULL THEN
        RAISE EXCEPTION 'Document type % must be attached to a student profile', NEW.document_type_id;
    ELSIF expected_target = 'APPLICATION' AND NEW.application_id IS NULL THEN
        RAISE EXCEPTION 'Document type % must be attached to an application', NEW.document_type_id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_documents_target_type
BEFORE INSERT OR UPDATE OF document_type_id, student_profile_id, application_id ON documents
FOR EACH ROW EXECUTE FUNCTION validate_document_target_type();

CREATE FUNCTION validate_customer_profile_pair() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.student_profile_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM student_profiles sp
        WHERE sp.id = NEW.student_profile_id
          AND sp.customer_id = NEW.customer_id
    ) THEN
        RAISE EXCEPTION 'Student profile % does not belong to customer %',
            NEW.student_profile_id, NEW.customer_id;
    END IF;

    IF TG_TABLE_NAME = 'invoices' THEN
        IF NEW.contract_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM contracts c
            WHERE c.id = NEW.contract_id
              AND c.customer_id = NEW.customer_id
        ) THEN
            RAISE EXCEPTION 'Contract % does not belong to customer %',
                NEW.contract_id, NEW.customer_id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_contract_customer_profile
BEFORE INSERT OR UPDATE OF customer_id, student_profile_id ON contracts
FOR EACH ROW EXECUTE FUNCTION validate_customer_profile_pair();

CREATE TRIGGER trg_invoice_customer_profile_contract
BEFORE INSERT OR UPDATE OF customer_id, student_profile_id, contract_id ON invoices
FOR EACH ROW EXECUTE FUNCTION validate_customer_profile_pair();

CREATE FUNCTION validate_payment_allocation() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    payment_customer uuid;
    payment_currency char(3);
    invoice_customer uuid;
    invoice_currency char(3);
BEGIN
    SELECT pt.customer_id, pt.currency_code
      INTO payment_customer, payment_currency
    FROM payment_transactions pt
    WHERE pt.id = NEW.payment_transaction_id;

    SELECT i.customer_id, i.currency_code
      INTO invoice_customer, invoice_currency
    FROM installments ins
    JOIN invoices i ON i.id = ins.invoice_id
    WHERE ins.id = NEW.installment_id;

    IF payment_customer IS DISTINCT FROM invoice_customer THEN
        RAISE EXCEPTION 'Payment and installment must belong to the same customer';
    END IF;
    IF payment_currency IS DISTINCT FROM invoice_currency THEN
        RAISE EXCEPTION 'Payment and invoice currencies must match';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_payment_allocation_consistency
BEFORE INSERT OR UPDATE OF payment_transaction_id, installment_id ON payment_allocations
FOR EACH ROW EXECUTE FUNCTION validate_payment_allocation();

-- ============================================================
-- UPDATED_AT MAINTENANCE
-- ============================================================

CREATE FUNCTION set_updated_at() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

DO $$
DECLARE
    table_name text;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'roles', 'users', 'persons', 'person_contacts', 'pipelines',
        'leads', 'customers', 'student_profiles', 'schools', 'applications',
        'activities', 'tasks', 'document_types', 'documents', 'contracts',
        'invoices', 'installments', 'payment_transactions'
    ] LOOP
        EXECUTE format(
            'CREATE TRIGGER %I BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION set_updated_at()',
            'trg_' || table_name || '_updated_at',
            table_name
        );
    END LOOP;
END;
$$;

-- ============================================================
-- BASE REFERENCE DATA
-- No initial user is created here; never ship a default password.
-- ============================================================

INSERT INTO roles (code, name, description, is_system) VALUES
    ('SYSTEM_ADMIN', 'System administrator', 'Full access to CRM configuration and records.', true),
    ('MANAGER', 'Manager', 'Manage counselors, leads, student profiles, and reports.', true),
    ('COUNSELOR', 'Counselor', 'Work with assigned leads and student profiles.', true),
    ('ACCOUNTANT', 'Accountant', 'Manage contracts, invoices, installments, and payments.', true);

INSERT INTO permissions (code, name, module) VALUES
    ('USERS.READ', 'View users', 'USERS'),
    ('USERS.MANAGE', 'Manage users and roles', 'USERS'),
    ('LEADS.READ', 'View leads', 'LEADS'),
    ('LEADS.CREATE', 'Create leads', 'LEADS'),
    ('LEADS.UPDATE', 'Update leads', 'LEADS'),
    ('LEADS.ASSIGN', 'Assign leads', 'LEADS'),
    ('LEADS.CONVERT', 'Convert leads to customers', 'LEADS'),
    ('CUSTOMERS.READ', 'View customers', 'CUSTOMERS'),
    ('CUSTOMERS.UPDATE', 'Update customers', 'CUSTOMERS'),
    ('PROFILES.READ', 'View student profiles', 'PROFILES'),
    ('PROFILES.CREATE', 'Create student profiles', 'PROFILES'),
    ('PROFILES.UPDATE', 'Update student profiles', 'PROFILES'),
    ('APPLICATIONS.MANAGE', 'Manage school applications', 'APPLICATIONS'),
    ('DOCUMENTS.READ', 'View documents', 'DOCUMENTS'),
    ('DOCUMENTS.MANAGE', 'Upload and review documents', 'DOCUMENTS'),
    ('ACTIVITIES.READ', 'View activity history', 'ACTIVITIES'),
    ('ACTIVITIES.CREATE', 'Record activities', 'ACTIVITIES'),
    ('TASKS.MANAGE', 'Manage follow-up tasks', 'TASKS'),
    ('CONTRACTS.MANAGE', 'Manage contracts', 'FINANCE'),
    ('INVOICES.MANAGE', 'Manage invoices and installments', 'FINANCE'),
    ('PAYMENTS.READ', 'View payments', 'FINANCE'),
    ('PAYMENTS.MANAGE', 'Record payments and refunds', 'FINANCE'),
    ('REPORTS.READ', 'View reports', 'REPORTS'),
    ('CONFIGURATION.MANAGE', 'Manage CRM configuration', 'CONFIGURATION'),
    ('AUDIT.READ', 'View audit history', 'AUDIT');

-- Admin gets every seeded permission.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SYSTEM_ADMIN';

-- Manager permissions.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'USERS.READ', 'LEADS.READ', 'LEADS.CREATE', 'LEADS.UPDATE', 'LEADS.ASSIGN',
    'LEADS.CONVERT', 'CUSTOMERS.READ', 'CUSTOMERS.UPDATE', 'PROFILES.READ',
    'PROFILES.CREATE', 'PROFILES.UPDATE', 'APPLICATIONS.MANAGE', 'DOCUMENTS.READ',
    'DOCUMENTS.MANAGE', 'ACTIVITIES.READ', 'ACTIVITIES.CREATE', 'TASKS.MANAGE',
    'CONTRACTS.MANAGE', 'INVOICES.MANAGE', 'PAYMENTS.READ', 'PAYMENTS.MANAGE',
    'REPORTS.READ', 'AUDIT.READ'
)
WHERE r.code = 'MANAGER';

-- Counselor permissions.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'LEADS.READ', 'LEADS.CREATE', 'LEADS.UPDATE', 'LEADS.CONVERT',
    'CUSTOMERS.READ', 'CUSTOMERS.UPDATE', 'PROFILES.READ', 'PROFILES.CREATE',
    'PROFILES.UPDATE', 'APPLICATIONS.MANAGE', 'DOCUMENTS.READ', 'DOCUMENTS.MANAGE',
    'ACTIVITIES.READ', 'ACTIVITIES.CREATE', 'TASKS.MANAGE'
)
WHERE r.code = 'COUNSELOR';

-- Accountant permissions.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'CUSTOMERS.READ', 'PROFILES.READ', 'CONTRACTS.MANAGE', 'INVOICES.MANAGE',
    'PAYMENTS.READ', 'PAYMENTS.MANAGE', 'REPORTS.READ'
)
WHERE r.code = 'ACCOUNTANT';

INSERT INTO pipelines (code, name, entity_type, is_default) VALUES
    ('LEAD_DEFAULT', 'Lead consultation', 'LEAD', true),
    ('STUDENT_DEFAULT', 'Student application process', 'STUDENT_PROFILE', true);

INSERT INTO pipeline_stages
    (pipeline_id, code, name, order_index, category, is_initial, is_terminal)
SELECT p.id, s.code, s.name, s.order_index, s.category, s.is_initial, s.is_terminal
FROM pipelines p
JOIN (VALUES
    ('LEAD_DEFAULT', 'NEW', 'New', 10, 'OPEN', true, false),
    ('LEAD_DEFAULT', 'CONTACTED', 'Contacted', 20, 'OPEN', false, false),
    ('LEAD_DEFAULT', 'CONSULTATION', 'Consultation', 30, 'OPEN', false, false),
    ('LEAD_DEFAULT', 'QUALIFIED', 'Qualified', 40, 'OPEN', false, false),
    ('LEAD_DEFAULT', 'WON', 'Converted', 50, 'WON', false, true),
    ('LEAD_DEFAULT', 'LOST', 'Lost', 60, 'LOST', false, true),
    ('STUDENT_DEFAULT', 'DOCUMENT_PREPARATION', 'Document preparation', 10, 'OPEN', true, false),
    ('STUDENT_DEFAULT', 'TRANSLATION', 'Translation', 20, 'OPEN', false, false),
    ('STUDENT_DEFAULT', 'SCHOOL_SUBMISSION', 'School submission', 30, 'OPEN', false, false),
    ('STUDENT_DEFAULT', 'ADMISSION', 'Admission result', 40, 'OPEN', false, false),
    ('STUDENT_DEFAULT', 'VISA', 'Visa process', 50, 'OPEN', false, false),
    ('STUDENT_DEFAULT', 'DEPARTED', 'Departed', 60, 'WON', false, true),
    ('STUDENT_DEFAULT', 'CANCELLED', 'Cancelled', 70, 'LOST', false, true)
) AS s(pipeline_code, code, name, order_index, category, is_initial, is_terminal)
    ON p.code = s.pipeline_code;

INSERT INTO lead_sources (code, name) VALUES
    ('FACEBOOK', 'Facebook'),
    ('ZALO', 'Zalo'),
    ('WEBSITE', 'Website'),
    ('REFERRAL', 'Referral'),
    ('ORGANIC', 'Organic'),
    ('OTHER', 'Other');

INSERT INTO visa_types (code, name) VALUES
    ('D-4-1', 'Korean language training'),
    ('D-2-1', 'Associate degree'),
    ('D-2-2', 'Bachelor degree'),
    ('D-2-3', 'Master degree'),
    ('D-2-4', 'Doctoral degree');

INSERT INTO document_types (code, name, applies_to, is_required) VALUES
    ('PASSPORT', 'Passport', 'STUDENT_PROFILE', true),
    ('HOUSEHOLD_RECORD', 'Household record', 'STUDENT_PROFILE', false),
    ('TRANSCRIPT', 'Academic transcript', 'APPLICATION', true),
    ('GRADUATION_CERTIFICATE', 'Graduation certificate', 'APPLICATION', true),
    ('BANK_STATEMENT', 'Bank statement', 'APPLICATION', false),
    ('LANGUAGE_CERTIFICATE', 'Language certificate', 'APPLICATION', false),
    ('OTHER', 'Other document', 'STUDENT_PROFILE', false);
