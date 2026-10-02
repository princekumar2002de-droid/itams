-- =============================================================================
-- V1 · Baseline schema for ITAMS
-- Owned by Flyway. Do NOT edit an applied migration; add V2, V3, ... instead.
-- Sourced from /database/schema.sql (Milestone 1).
-- =============================================================================


-- Utility: touch updated_at on UPDATE.
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;


-- ── 1. PERSON ────────────────────────────────────────────────────────────────
CREATE TABLE person (
    id           BIGSERIAL PRIMARY KEY,
    first_name   VARCHAR(80)  NOT NULL,
    last_name    VARCHAR(80)  NOT NULL,
    email        VARCHAR(160) NOT NULL,
    phone        VARCHAR(40),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_person_email_live
    ON person (LOWER(email)) WHERE deleted_at IS NULL;
CREATE TRIGGER trg_person_updated
    BEFORE UPDATE ON person
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ── 2. DEPARTMENT ────────────────────────────────────────────────────────────
CREATE TABLE department (
    id                    BIGSERIAL PRIMARY KEY,
    code                  VARCHAR(30)  NOT NULL,
    name                  VARCHAR(160) NOT NULL,
    parent_department_id  BIGINT REFERENCES department(id) ON DELETE SET NULL,
    manager_person_id     BIGINT REFERENCES person(id)     ON DELETE SET NULL,
    deleted_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_department_code_live
    ON department (code) WHERE deleted_at IS NULL;
CREATE INDEX ix_department_parent ON department (parent_department_id);
CREATE TRIGGER trg_department_updated
    BEFORE UPDATE ON department
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ── 3. EMPLOYEE ──────────────────────────────────────────────────────────────
CREATE TABLE employee (
    id                 BIGSERIAL PRIMARY KEY,
    person_id          BIGINT      NOT NULL UNIQUE REFERENCES person(id)     ON DELETE RESTRICT,
    employee_number    VARCHAR(30) NOT NULL UNIQUE,
    department_id      BIGINT      NOT NULL REFERENCES department(id)        ON DELETE RESTRICT,
    job_title          VARCHAR(120),
    hire_date          DATE        NOT NULL,
    end_date           DATE,
    employment_status  TEXT        NOT NULL DEFAULT 'ACTIVE'
        CHECK (employment_status IN ('ACTIVE','ON_LEAVE','LEFT')),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_employee_dates CHECK (end_date IS NULL OR end_date >= hire_date)
);
CREATE INDEX ix_employee_department ON employee (department_id);
CREATE TRIGGER trg_employee_updated
    BEFORE UPDATE ON employee
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ── 4. USER_ACCOUNT ──────────────────────────────────────────────────────────
CREATE TABLE user_account (
    id             BIGSERIAL PRIMARY KEY,
    person_id      BIGINT       NOT NULL UNIQUE REFERENCES person(id) ON DELETE RESTRICT,
    username       VARCHAR(80)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_user_account_updated
    BEFORE UPDATE ON user_account
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ── 5. ROLE + USER_ROLE ──────────────────────────────────────────────────────
CREATE TABLE role (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE user_role (
    user_account_id BIGINT NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    role_id         BIGINT NOT NULL REFERENCES role(id)          ON DELETE RESTRICT,
    PRIMARY KEY (user_account_id, role_id)
);
CREATE INDEX ix_user_role_role ON user_role (role_id);


-- ── 6. REFRESH_TOKEN ─────────────────────────────────────────────────────────
CREATE TABLE refresh_token (
    id                        BIGSERIAL PRIMARY KEY,
    user_account_id           BIGINT       NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    token_hash                VARCHAR(255) NOT NULL UNIQUE,
    issued_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at                TIMESTAMPTZ  NOT NULL,
    revoked_at                TIMESTAMPTZ,
    replaced_by_token_hash    VARCHAR(255),
    CONSTRAINT ck_refresh_token_expiry CHECK (expires_at > issued_at)
);
CREATE INDEX ix_refresh_token_user ON refresh_token (user_account_id);


-- ── 7. ASSET_CATEGORY ────────────────────────────────────────────────────────
CREATE TABLE asset_category (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_asset_category_updated
    BEFORE UPDATE ON asset_category
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ── 8. ASSET_MODEL ───────────────────────────────────────────────────────────
CREATE TABLE asset_model (
    id           BIGSERIAL PRIMARY KEY,
    category_id  BIGINT       NOT NULL REFERENCES asset_category(id) ON DELETE RESTRICT,
    manufacturer VARCHAR(80)  NOT NULL,
    model_name   VARCHAR(160) NOT NULL,
    specs        JSONB,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_asset_model_manuf_model UNIQUE (manufacturer, model_name)
);
CREATE INDEX ix_asset_model_category ON asset_model (category_id);
CREATE TRIGGER trg_asset_model_updated
    BEFORE UPDATE ON asset_model
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ── 9. ASSET ─────────────────────────────────────────────────────────────────
CREATE TABLE asset (
    id               BIGSERIAL PRIMARY KEY,
    asset_tag        VARCHAR(30)  NOT NULL UNIQUE,
    model_id         BIGINT       NOT NULL REFERENCES asset_model(id) ON DELETE RESTRICT,
    serial_number    VARCHAR(80)  NOT NULL,
    status           TEXT         NOT NULL DEFAULT 'IN_STOCK'
        CHECK (status IN ('IN_STOCK','ASSIGNED','UNDER_MAINTENANCE','RETIRED','LOST')),
    purchase_date    DATE         NOT NULL,
    purchase_price   NUMERIC(12,2) NOT NULL CHECK (purchase_price >= 0),
    warranty_ends_on DATE,
    notes            TEXT,
    retired_at       TIMESTAMPTZ,
    retired_reason   VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_asset_model_serial UNIQUE (model_id, serial_number),
    CONSTRAINT ck_asset_retired_fields CHECK (
        (status = 'RETIRED' AND retired_at IS NOT NULL)
        OR (status <> 'RETIRED' AND retired_at IS NULL)
    )
);
CREATE INDEX ix_asset_status         ON asset (status);
CREATE INDEX ix_asset_model          ON asset (model_id);
CREATE INDEX ix_asset_warranty_end   ON asset (warranty_ends_on);
CREATE TRIGGER trg_asset_updated
    BEFORE UPDATE ON asset
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ── 10. ASSET_ASSIGNMENT ─────────────────────────────────────────────────────
CREATE TABLE asset_assignment (
    id                    BIGSERIAL PRIMARY KEY,
    asset_id              BIGINT       NOT NULL REFERENCES asset(id)         ON DELETE RESTRICT,
    assignee_person_id    BIGINT       NOT NULL REFERENCES person(id)        ON DELETE RESTRICT,
    assigned_by_user_id   BIGINT       NOT NULL REFERENCES user_account(id)  ON DELETE RESTRICT,
    assigned_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expected_return_on    DATE,
    actual_return_at      TIMESTAMPTZ,
    returned_by_user_id   BIGINT       REFERENCES user_account(id)           ON DELETE RESTRICT,
    out_condition         TEXT         NOT NULL
        CHECK (out_condition IN ('NEW','GOOD','FAIR','POOR')),
    in_condition          TEXT
        CHECK (in_condition IN ('NEW','GOOD','FAIR','POOR')),
    notes                 TEXT,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_assignment_return_pair CHECK (
        (actual_return_at IS NULL     AND returned_by_user_id IS NULL     AND in_condition IS NULL)
     OR (actual_return_at IS NOT NULL AND returned_by_user_id IS NOT NULL AND in_condition IS NOT NULL)
    ),
    CONSTRAINT ck_assignment_return_after_assign
        CHECK (actual_return_at IS NULL OR actual_return_at >= assigned_at)
);
CREATE UNIQUE INDEX ux_asset_assignment_one_open
    ON asset_assignment (asset_id) WHERE actual_return_at IS NULL;
CREATE INDEX ix_asset_assignment_assignee_open
    ON asset_assignment (assignee_person_id, actual_return_at);
CREATE INDEX ix_asset_assignment_assigned_by ON asset_assignment (assigned_by_user_id);
CREATE TRIGGER trg_asset_assignment_updated
    BEFORE UPDATE ON asset_assignment
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ── 11. SOFTWARE_PRODUCT ─────────────────────────────────────────────────────
CREATE TABLE software_product (
    id         BIGSERIAL PRIMARY KEY,
    vendor     VARCHAR(120) NOT NULL,
    name       VARCHAR(160) NOT NULL,
    version    VARCHAR(60),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_software_product UNIQUE (vendor, name, version)
);
CREATE TRIGGER trg_software_product_updated
    BEFORE UPDATE ON software_product
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ── 12. SOFTWARE_LICENSE ─────────────────────────────────────────────────────
CREATE TABLE software_license (
    id                BIGSERIAL PRIMARY KEY,
    product_id        BIGINT       NOT NULL REFERENCES software_product(id) ON DELETE RESTRICT,
    license_reference VARCHAR(120) NOT NULL,
    license_type      TEXT         NOT NULL
        CHECK (license_type IN ('PER_SEAT','PER_DEVICE','SITE','SUBSCRIPTION')),
    seats_total       INTEGER      NOT NULL CHECK (seats_total >= 1),
    purchase_date     DATE         NOT NULL,
    expires_on        DATE,
    cost              NUMERIC(12,2) NOT NULL CHECK (cost >= 0),
    procurement_ref   VARCHAR(120),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_software_license UNIQUE (product_id, license_reference),
    CONSTRAINT ck_license_expiry CHECK (expires_on IS NULL OR expires_on >= purchase_date)
);
CREATE INDEX ix_software_license_expires ON software_license (expires_on);
CREATE TRIGGER trg_software_license_updated
    BEFORE UPDATE ON software_license
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ── 13. LICENSE_ASSIGNMENT ───────────────────────────────────────────────────
CREATE TABLE license_assignment (
    id           BIGSERIAL PRIMARY KEY,
    license_id   BIGINT      NOT NULL REFERENCES software_license(id) ON DELETE RESTRICT,
    person_id    BIGINT      REFERENCES person(id) ON DELETE RESTRICT,
    asset_id     BIGINT      REFERENCES asset(id)  ON DELETE RESTRICT,
    assigned_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    released_at  TIMESTAMPTZ,
    notes        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_license_assignment_target
        CHECK ((person_id IS NOT NULL)::int + (asset_id IS NOT NULL)::int = 1),
    CONSTRAINT ck_license_assignment_released_after
        CHECK (released_at IS NULL OR released_at >= assigned_at)
);
CREATE INDEX ix_license_assignment_open
    ON license_assignment (license_id) WHERE released_at IS NULL;
CREATE INDEX ix_license_assignment_person ON license_assignment (person_id);
CREATE INDEX ix_license_assignment_asset  ON license_assignment (asset_id);
CREATE TRIGGER trg_license_assignment_updated
    BEFORE UPDATE ON license_assignment
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ── 14. MAINTENANCE_RECORD ───────────────────────────────────────────────────
CREATE TABLE maintenance_record (
    id                    BIGSERIAL PRIMARY KEY,
    asset_id              BIGINT      NOT NULL REFERENCES asset(id)        ON DELETE RESTRICT,
    performed_on          DATE        NOT NULL,
    performed_by_user_id  BIGINT      REFERENCES user_account(id)          ON DELETE SET NULL,
    provider_name         VARCHAR(160),
    description           TEXT        NOT NULL,
    cost                  NUMERIC(12,2) CHECK (cost IS NULL OR cost >= 0),
    next_scheduled_on     DATE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_maintenance_performer CHECK (
        performed_by_user_id IS NOT NULL OR provider_name IS NOT NULL
    )
);
CREATE INDEX ix_maintenance_asset ON maintenance_record (asset_id, performed_on DESC);
CREATE TRIGGER trg_maintenance_record_updated
    BEFORE UPDATE ON maintenance_record
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ── 15. TICKET ───────────────────────────────────────────────────────────────
CREATE TABLE ticket (
    id                   BIGSERIAL PRIMARY KEY,
    ticket_number        VARCHAR(30)  NOT NULL UNIQUE,
    subject              VARCHAR(200) NOT NULL,
    description          TEXT         NOT NULL,
    priority             TEXT         NOT NULL
        CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    status               TEXT         NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN','IN_PROGRESS','WAITING','RESOLVED','CLOSED')),
    reporter_person_id   BIGINT       NOT NULL REFERENCES person(id)       ON DELETE RESTRICT,
    related_asset_id     BIGINT       REFERENCES asset(id)                 ON DELETE SET NULL,
    assigned_to_user_id  BIGINT       REFERENCES user_account(id)          ON DELETE SET NULL,
    resolved_at          TIMESTAMPTZ,
    closed_at            TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_ticket_close_after_resolve
        CHECK (closed_at IS NULL OR resolved_at IS NULL OR closed_at >= resolved_at)
);
CREATE INDEX ix_ticket_status_priority ON ticket (status, priority);
CREATE INDEX ix_ticket_assignee_status ON ticket (assigned_to_user_id, status);
CREATE INDEX ix_ticket_reporter_status ON ticket (reporter_person_id, status);
CREATE INDEX ix_ticket_related_asset   ON ticket (related_asset_id);
CREATE TRIGGER trg_ticket_updated
    BEFORE UPDATE ON ticket
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ── 16. TICKET_COMMENT ───────────────────────────────────────────────────────
CREATE TABLE ticket_comment (
    id              BIGSERIAL PRIMARY KEY,
    ticket_id       BIGINT      NOT NULL REFERENCES ticket(id)       ON DELETE CASCADE,
    author_user_id  BIGINT      NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT,
    body            TEXT        NOT NULL,
    is_internal     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_ticket_comment_ticket ON ticket_comment (ticket_id, created_at);


-- ── 17. AUDIT_LOG ────────────────────────────────────────────────────────────
CREATE TABLE audit_log (
    id             BIGSERIAL PRIMARY KEY,
    occurred_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_user_id  BIGINT      REFERENCES user_account(id) ON DELETE SET NULL,
    entity_type    VARCHAR(60) NOT NULL,
    entity_id      BIGINT      NOT NULL,
    action         TEXT        NOT NULL
        CHECK (action IN (
            'CREATE','UPDATE','DELETE',
            'ASSIGN','RETURN','STATUS_CHANGE',
            'LOGIN','LOGOUT','TOKEN_REFRESH','TOKEN_REVOKE'
        )),
    changes        JSONB,
    request_id     VARCHAR(60)
);
CREATE INDEX ix_audit_entity  ON audit_log (entity_type, entity_id, occurred_at DESC);
CREATE INDEX ix_audit_actor   ON audit_log (actor_user_id, occurred_at DESC);
CREATE INDEX ix_audit_time    ON audit_log (occurred_at DESC);
