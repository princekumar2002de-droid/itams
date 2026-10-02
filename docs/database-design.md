# Database design: IT Asset Management System

**Document status:** v0.1 · Milestone 1 · 2026-09-21
**Target DBMS:** PostgreSQL 16
**Migration tool:** Flyway (schema is owned by SQL migrations, not by JPA annotations)

This document explains the schema **before** the DDL is written. The DDL itself lives in [`../database/schema.sql`](../database/schema.sql), and the visual ER model lives in [`diagrams/er-diagram.mmd`](diagrams/er-diagram.mmd). If these three ever disagree, this document is the intent and the SQL is the truth.

---

## 1. Design principles

1. **Third normal form as default.** Denormalise only for a demonstrated read-side pain, which for this scale we don't have.
2. **Business rules in the database wherever they can be.** Uniqueness, referential integrity, non-negative values and mutually-exclusive columns are enforced by constraints. The service layer is a second line of defence, not the only one.
3. **Soft-delete personal data, hard-delete master data.** People (persons, employees, users) get `deleted_at` to preserve historical assignments and audit trails; static master data (asset categories, models) can be deleted directly.
4. **Every domain table carries `id BIGSERIAL PK`, `created_at`, `updated_at`.** Consistency wins over saving a few bytes.
5. **Money in `NUMERIC(12,2)` in EUR.** No floats for currency, ever. All amounts are stored in EUR since the project is single-currency; a future multi-currency extension would add a `currency` column.
6. **Enumerations as `TEXT` columns with `CHECK` constraints**, not PostgreSQL `ENUM` types. PG enums are painful to migrate (adding/renaming a value requires rewriting the type); a `CHECK (status IN ('IN_STOCK', …))` is trivial to change with an `ALTER TABLE`.
7. **Timestamps are `TIMESTAMPTZ`.** No plain `TIMESTAMP`, because timezone mix-ups are a common source of real bugs.
8. **Naming:** `snake_case` for tables and columns, singular table names (`asset`, not `assets`). This is a matter of taste, but I wanted one rule and applied it everywhere.

## 2. Entity list (15 domain tables + audit)

| # | Table | Purpose |
|---|---|---|
| 1 | `person` | A human (name, email, phone). Base identity for employees and users. |
| 2 | `employee` | The employment record attached to a person (department, employee number, hire date). |
| 3 | `department` | Organisational unit. Self-referencing for hierarchy. |
| 4 | `user_account` | A login. Linked to a person. |
| 5 | `role` | One of `ADMIN`, `IT_MANAGER`, `EMPLOYEE`. Seeded. |
| 6 | `user_role` | Many-to-many between `user_account` and `role`. |
| 7 | `refresh_token` | Server-side record of issued refresh tokens (for revocation). |
| 8 | `asset_category` | Broad type: laptop, monitor, phone, etc. Seeded, editable. |
| 9 | `asset_model` | A commercial model (e.g. "ThinkPad T14 Gen 4"). |
| 10 | `asset` | A specific, individually-tracked physical item. |
| 11 | `asset_assignment` | The link between an asset and a person for a period of time. |
| 12 | `software_product` | A commercial software product. |
| 13 | `software_license` | A specific purchased entitlement (seats, expiry, cost). |
| 14 | `license_assignment` | Allocation of one license seat to a person or an asset. |
| 15 | `maintenance_record` | A maintenance event against an asset. |
| 16 | `ticket` | A support ticket. |
| 17 | `ticket_comment` | A comment on a ticket. |
| 18 | `audit_log` | Append-only history of state changes. |

## 3. Relationships (narrative)

- A **person** may have exactly one **employee** record (a `1:0..1` relationship). A person may or may not have a `user_account` (also `1:0..1`): external contractors have a person record for asset assignment but no login.
- A **user_account** has one or more **roles** through **user_role**.
- A **department** may have a **person** as its manager, and can be the child of a parent **department**.
- An **asset_model** belongs to one **asset_category**. An **asset** is one instance of an **asset_model**.
- An **asset** has zero or more **asset_assignment** rows over its life, but at any moment at most one where `actual_return_at IS NULL`. This is enforced by a partial unique index (see §5.1).
- A **software_license** belongs to one **software_product**. A **license_assignment** links a license to *either* a **person** *or* an **asset** (never both), enforced by a `CHECK` constraint.
- A **ticket** is raised by a **person** (reporter), optionally references an **asset**, and may be assigned to a **user_account** (the IT_MANAGER handling it). Comments belong to the ticket.
- The **audit_log** references the acting **user_account** (nullable, so system-generated events can be represented).

The visual version is in [`diagrams/er-diagram.mmd`](diagrams/er-diagram.mmd).

## 4. Table specs (columns, types, constraints)

The DDL lives in the Flyway migrations (`backend/src/main/resources/db/migration`). This section summarises what is in there and why.

### 4.1 `person`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `first_name` | VARCHAR(80) NOT NULL | |
| `last_name` | VARCHAR(80) NOT NULL | |
| `email` | VARCHAR(160) NOT NULL | Unique among non-deleted rows (partial unique index) |
| `phone` | VARCHAR(40) | |
| `is_active` | BOOLEAN NOT NULL DEFAULT TRUE | |
| `deleted_at` | TIMESTAMPTZ | Soft-delete marker |
| `created_at`, `updated_at` | TIMESTAMPTZ NOT NULL | |

**Design note:** `person` is separated from `employee` and `user_account` because *one human* may play different roles (employee, external contractor with an asset, generic user of the system). Merging them would either add many nullable columns or force every login to be an employee.

### 4.2 `employee`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `person_id` | BIGINT NOT NULL, UNIQUE, FK → `person(id)` | 1:1 |
| `employee_number` | VARCHAR(30) NOT NULL, UNIQUE | Human-friendly ID |
| `department_id` | BIGINT NOT NULL, FK → `department(id)` | |
| `job_title` | VARCHAR(120) | |
| `hire_date` | DATE NOT NULL | |
| `end_date` | DATE | NULL while employed |
| `employment_status` | TEXT NOT NULL DEFAULT 'ACTIVE' CHECK IN ('ACTIVE','ON_LEAVE','LEFT') | |
| `created_at`, `updated_at` | TIMESTAMPTZ NOT NULL | |

### 4.3 `department`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `code` | VARCHAR(30) NOT NULL UNIQUE | e.g. `IT`, `FIN`, `MKT-EU` |
| `name` | VARCHAR(160) NOT NULL | |
| `parent_department_id` | BIGINT, FK → `department(id)` ON DELETE SET NULL | Self-reference |
| `manager_person_id` | BIGINT, FK → `person(id)` ON DELETE SET NULL | |
| `deleted_at` | TIMESTAMPTZ | Soft-delete |
| `created_at`, `updated_at` | TIMESTAMPTZ NOT NULL | |

### 4.4 `user_account`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `person_id` | BIGINT NOT NULL UNIQUE, FK → `person(id)` | One login per person |
| `username` | VARCHAR(80) NOT NULL UNIQUE | |
| `password_hash` | VARCHAR(255) NOT NULL | BCrypt |
| `enabled` | BOOLEAN NOT NULL DEFAULT TRUE | |
| `last_login_at` | TIMESTAMPTZ | |
| `created_at`, `updated_at` | TIMESTAMPTZ NOT NULL | |

### 4.5 `role` + `user_role`

```
role(id BIGSERIAL PK, code VARCHAR(30) UNIQUE, description VARCHAR(255))
user_role(user_account_id BIGINT, role_id BIGINT, PRIMARY KEY (user_account_id, role_id))
```

`role` is seeded with `ADMIN`, `IT_MANAGER`, `EMPLOYEE`.

### 4.6 `refresh_token`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `user_account_id` | BIGINT NOT NULL, FK → `user_account(id)` | |
| `token_hash` | VARCHAR(255) NOT NULL UNIQUE | We store a hash of the token, not the token itself |
| `issued_at` | TIMESTAMPTZ NOT NULL | |
| `expires_at` | TIMESTAMPTZ NOT NULL | |
| `revoked_at` | TIMESTAMPTZ | NULL means still valid |
| `replaced_by_token_hash` | VARCHAR(255) | Set when rotated; supports revocation chains |
| `family_id` | UUID NOT NULL | Shared by all tokens of one login; reusing an old token revokes the whole family (V4) |

### 4.7 `asset_category`
| `id` PK | `code` UNIQUE | `name` NOT NULL | `description` | timestamps |

Seeded with `LAPTOP`, `DESKTOP`, `MONITOR`, `PHONE`, `TABLET`, `DOCK`, `PERIPHERAL`, `OTHER`. Editable by ADMIN.

### 4.8 `asset_model`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `category_id` | BIGINT NOT NULL, FK → `asset_category(id)` | |
| `manufacturer` | VARCHAR(80) NOT NULL | |
| `model_name` | VARCHAR(160) NOT NULL | |
| `specs` | JSONB | Free-form: RAM, CPU, storage, screen size, etc. Not queried, just displayed. |
| `created_at`, `updated_at` | TIMESTAMPTZ NOT NULL | |
| UNIQUE (`manufacturer`, `model_name`) | | Prevent accidental duplicates |

### 4.9 `asset`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `asset_tag` | VARCHAR(30) NOT NULL UNIQUE | Company-internal ID printed on a sticker |
| `model_id` | BIGINT NOT NULL, FK → `asset_model(id)` | |
| `serial_number` | VARCHAR(80) NOT NULL | Manufacturer serial |
| `status` | TEXT NOT NULL CHECK IN ('IN_STOCK','ASSIGNED','UNDER_MAINTENANCE','RETIRED','LOST') | Default `IN_STOCK` |
| `purchase_date` | DATE NOT NULL | |
| `purchase_price` | NUMERIC(12,2) NOT NULL CHECK ≥ 0 | EUR |
| `warranty_ends_on` | DATE | Nullable |
| `notes` | TEXT | |
| `retired_at` | TIMESTAMPTZ | Set when status → RETIRED |
| `retired_reason` | VARCHAR(255) | |
| `created_at`, `updated_at` | TIMESTAMPTZ NOT NULL | |
| UNIQUE (`model_id`, `serial_number`) | | Same serial can't repeat for the same model |

### 4.10 `asset_assignment`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `asset_id` | BIGINT NOT NULL, FK → `asset(id)` | |
| `assignee_person_id` | BIGINT NOT NULL, FK → `person(id)` | |
| `assigned_by_user_id` | BIGINT NOT NULL, FK → `user_account(id)` | |
| `assigned_at` | TIMESTAMPTZ NOT NULL DEFAULT now() | |
| `expected_return_on` | DATE | Nullable |
| `actual_return_at` | TIMESTAMPTZ | NULL = still assigned |
| `returned_by_user_id` | BIGINT, FK → `user_account(id)` | Set on return |
| `out_condition` | TEXT NOT NULL CHECK IN ('NEW','GOOD','FAIR','POOR') | |
| `in_condition` | TEXT CHECK IN ('NEW','GOOD','FAIR','POOR') | Set on return |
| `notes` | TEXT | |
| `created_at`, `updated_at` | TIMESTAMPTZ NOT NULL | |
| PARTIAL UNIQUE INDEX on (`asset_id`) WHERE `actual_return_at IS NULL` | | **Enforces "at most one open assignment per asset"** |

### 4.11 `software_product`
| `id` PK | `vendor` NOT NULL | `name` NOT NULL | `version` | timestamps | UNIQUE (`vendor`, `name`, `version`) |

### 4.12 `software_license`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `product_id` | BIGINT NOT NULL, FK → `software_product(id)` | |
| `license_reference` | VARCHAR(120) NOT NULL | Key or order/contract number |
| `license_type` | TEXT NOT NULL CHECK IN ('PER_SEAT','PER_DEVICE','SITE','SUBSCRIPTION') | |
| `seats_total` | INTEGER NOT NULL CHECK ≥ 1 | |
| `purchase_date` | DATE NOT NULL | |
| `expires_on` | DATE | NULL = perpetual |
| `cost` | NUMERIC(12,2) NOT NULL CHECK ≥ 0 | EUR |
| `procurement_ref` | VARCHAR(120) | PO number, invoice, etc. |
| `created_at`, `updated_at` | TIMESTAMPTZ NOT NULL | |
| UNIQUE (`product_id`, `license_reference`) | | |

### 4.13 `license_assignment`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `license_id` | BIGINT NOT NULL, FK → `software_license(id)` | |
| `person_id` | BIGINT, FK → `person(id)` | |
| `asset_id` | BIGINT, FK → `asset(id)` | |
| `assigned_at` | TIMESTAMPTZ NOT NULL DEFAULT now() | |
| `released_at` | TIMESTAMPTZ | NULL = still allocated |
| `notes` | TEXT | |
| CHECK ((`person_id` IS NOT NULL) <> (`asset_id` IS NOT NULL)) | | XOR: assigned to person xor to asset, never both, never neither |
| INDEX on (`license_id`) WHERE `released_at IS NULL` | | Fast "how many seats used?" query |

**The "seats not exceeded" rule** is enforced in the service layer (`SELECT … FOR UPDATE` on the license row, then count open assignments, then insert). It's *not* enforceable purely declaratively in PostgreSQL without a trigger, and I'd rather have that logic visible in Java where it can be unit-tested.

### 4.14 `maintenance_record`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `asset_id` | BIGINT NOT NULL, FK → `asset(id)` | |
| `performed_on` | DATE NOT NULL | |
| `performed_by_user_id` | BIGINT, FK → `user_account(id)` | NULL if external vendor |
| `provider_name` | VARCHAR(160) | Non-null when `performed_by_user_id IS NULL` (checked in service) |
| `description` | TEXT NOT NULL | |
| `cost` | NUMERIC(12,2) CHECK ≥ 0 | EUR |
| `next_scheduled_on` | DATE | |
| `created_at`, `updated_at` | TIMESTAMPTZ NOT NULL | |

### 4.15 `ticket`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `ticket_number` | VARCHAR(30) NOT NULL UNIQUE | e.g. `TCK-2026-000123`. Generated at insert. |
| `subject` | VARCHAR(200) NOT NULL | |
| `description` | TEXT NOT NULL | |
| `priority` | TEXT NOT NULL CHECK IN ('LOW','MEDIUM','HIGH','CRITICAL') | |
| `status` | TEXT NOT NULL DEFAULT 'OPEN' CHECK IN ('OPEN','IN_PROGRESS','WAITING','RESOLVED','CLOSED') | |
| `reporter_person_id` | BIGINT NOT NULL, FK → `person(id)` | |
| `related_asset_id` | BIGINT, FK → `asset(id)` | |
| `assigned_to_user_id` | BIGINT, FK → `user_account(id)` | |
| `resolved_at` | TIMESTAMPTZ | |
| `closed_at` | TIMESTAMPTZ | |
| `created_at`, `updated_at` | TIMESTAMPTZ NOT NULL | |

### 4.16 `ticket_comment`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `ticket_id` | BIGINT NOT NULL, FK → `ticket(id)` ON DELETE CASCADE | |
| `author_user_id` | BIGINT NOT NULL, FK → `user_account(id)` | |
| `body` | TEXT NOT NULL | |
| `is_internal` | BOOLEAN NOT NULL DEFAULT FALSE | Hidden from reporter |
| `created_at` | TIMESTAMPTZ NOT NULL DEFAULT now() | |

### 4.17 `audit_log`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL PK | |
| `occurred_at` | TIMESTAMPTZ NOT NULL DEFAULT now() | |
| `actor_user_id` | BIGINT, FK → `user_account(id)` | NULL for system-generated events |
| `entity_type` | VARCHAR(60) NOT NULL | e.g. `asset`, `ticket` |
| `entity_id` | BIGINT NOT NULL | |
| `action` | TEXT NOT NULL CHECK IN ('CREATE','UPDATE','DELETE','ASSIGN','RETURN','STATUS_CHANGE','LOGIN','LOGOUT','TOKEN_REFRESH') | |
| `changes` | JSONB | Reserved for old / new values; not filled yet |
| `request_id` | VARCHAR(60) | Correlation ID from the request that caused this |

**Append-only:** the application never updates or deletes audit rows, and the table has no `updated_at` column.

---

## 5. Business rules

### 5.1 "An asset has at most one open assignment at a time"

Enforced by:

```sql
CREATE UNIQUE INDEX idx_asset_assignment_one_open
  ON asset_assignment(asset_id)
  WHERE actual_return_at IS NULL;
```

A partial unique index: PostgreSQL enforces the uniqueness only over rows matching the `WHERE`. Attempting to insert a second row for the same `asset_id` with `actual_return_at IS NULL` fails at the DB level. The service layer also checks this to give a clean 409 response instead of an ugly integrity-violation exception, but the database is the safety net.

### 5.2 "A license is assigned to a person XOR an asset, never both, never neither"

```sql
CHECK ((person_id IS NOT NULL) <> (asset_id IS NOT NULL))
```

### 5.3 "License seats not exceeded"

Not enforceable purely declaratively without a trigger. Enforced in the service:

```java
@Transactional
public LicenseAssignment assign(...) {
    SoftwareLicense lic = licenseRepo.findByIdForUpdate(licenseId);  // SELECT ... FOR UPDATE
    long used = licenseAssignmentRepo.countOpenByLicenseId(licenseId);
    if (used >= lic.getSeatsTotal()) {
        throw new BusinessRuleViolationException("license.out_of_seats");
    }
    // ...insert...
}
```

The `SELECT ... FOR UPDATE` row-locks the license, so concurrent assignment attempts serialise and cannot both see the same `used` count.

### 5.4 "Cannot assign a RETIRED or LOST asset"

Enforced in the service; DB constraint would need a trigger.

### 5.5 "Return only what's assigned"

The service verifies that the assignment referenced by `POST /assignments/{id}/return` has `actual_return_at IS NULL`.

### 5.6 "Ticket state machine"

Enforced in the service. Illegal transitions (e.g. `CLOSED → OPEN`) throw `BusinessRuleViolationException`.

### 5.7 "Employees see only their own"

For endpoints returning lists (assets, tickets, licenses) called with role `EMPLOYEE`, the service adds `WHERE person_id = :currentPersonId`. For single-row `GET`s, the service verifies ownership before returning; otherwise `404 Not Found` (not 403, so as not to leak existence).

## 6. Indexes

Beyond what primary keys and unique constraints implicitly create:

| Table | Index | Purpose |
|---|---|---|
| `person` | Partial UNIQUE on `(LOWER(email))` WHERE `deleted_at IS NULL` | Case-insensitive email uniqueness among live rows |
| `employee` | `(department_id)` | List "employees in department X" |
| `department` | `(parent_department_id)` | Traverse hierarchy |
| `asset` | `(status)` | Common filter in dashboards |
| `asset` | `(model_id)` | Join |
| `asset` | `(warranty_ends_on)` | Expiry KPIs |
| `asset_assignment` | PARTIAL UNIQUE `(asset_id) WHERE actual_return_at IS NULL` | Business rule §5.1 |
| `asset_assignment` | `(assignee_person_id, actual_return_at)` | "My assets" query |
| `software_license` | `(expires_on)` | Expiry KPIs |
| `license_assignment` | PARTIAL `(license_id) WHERE released_at IS NULL` | Seats-used count |
| `ticket` | `(status, priority)` | Dashboards |
| `ticket` | `(assigned_to_user_id, status)` | "My tickets" |
| `ticket` | `(reporter_person_id, status)` | EMPLOYEE view |
| `audit_log` | `(entity_type, entity_id, occurred_at DESC)` | Audit lookup |
| `audit_log` | `(actor_user_id, occurred_at DESC)` | "Who did what" |

## 7. Migration strategy

- `V1__baseline.sql`: the complete schema.
- `V2__seed_reference.sql`: reference data for `role` (ADMIN, IT_MANAGER, EMPLOYEE) and `asset_category`.
- `V3__seed_bootstrap_user.sql`: a disabled "system" user that cannot log in, used as the actor for system actions.
- `V4__refresh_token_family.sql`: adds `family_id` to `refresh_token` for reuse detection.

The three demo users are not part of the migrations. `DevBootstrap` creates them at startup in every profile except `prod`.
- Every subsequent change to the schema is a new `Vn__…sql` file. **No editing of an already-applied migration.**

## 8. What is deliberately *not* modelled here

- Cost centres for chargeback accounting. Would live on `department` or a separate `cost_centre` table; not needed for Project 1.
- Asset photos, invoice attachments. Would need object storage; punted.
- Approval workflows on assignments (line manager approval before an asset is issued). Not needed here; purchase approvals are part of my separate ERP project.
- Multi-tenancy. Explicitly out of scope.

## 9. Data-model risks and how they are handled

| Risk | Containment |
|---|---|
| Someone tries to hard-delete a person who has historical assignments → orphan | Soft-delete only; FK from `asset_assignment.assignee_person_id` uses `ON DELETE RESTRICT` so accidental hard-delete fails loudly. |
| Duplicate assignments due to a race condition | Partial unique index (§5.1) is the DB-level guarantee even if the service check races. |
| Timezone bugs in reporting | All timestamps `TIMESTAMPTZ`; UI converts on display; date-only fields (`hire_date`, `purchase_date`) are `DATE` and interpreted as company-local. |
| Schema drift between environments | Flyway is the only way to change the schema. `spring.jpa.hibernate.ddl-auto=validate` in every profile ensures Hibernate refuses to boot against a schema that doesn't match its entities. |
