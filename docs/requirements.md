# Requirements: IT Asset Management System (ITAMS)

**Document status:** v0.1 · Milestone 1 · 2026-09-21
**Author:** Prince Kumar

---

## 1. Purpose of this document

Establish, before any code is written, *what* the IT Asset Management System must do and *what qualities* it must have. Every later technical decision (schema shape, API surface, security model, test coverage) is traceable back to a requirement here.

## 2. Business context

**Problem being solved.** In a mid-sized company (approx. 50–500 employees) the IT operations team is responsible for procuring, distributing, tracking and eventually retiring hardware assets (laptops, monitors, phones, docks) and managing the software licenses that go with them. Today, most such teams manage this in spreadsheets and shared mailboxes. The result is data that is inconsistent (multiple truths), out-of-date, and unauditable.

**Users of the system.** Three distinct user populations, one system:

| Role | Approx. share of users | Main need |
|---|---|---|
| `ADMIN` | 1–3 people | Manage the whole system, users, roles, master data |
| `IT_MANAGER` | 3–10 people | Do the day-to-day operational work (assign, return, maintain, resolve tickets) |
| `EMPLOYEE` | Everyone else (50–500) | See "my assets", raise a ticket, acknowledge a return |

**Scope of Project 1.** A single-tenant internal application. Not a multi-tenant SaaS. Not integrated with any external identity provider or procurement system in this project (those are listed as future improvements).

## 3. Stakeholders

| Stakeholder | Interest in the system |
|---|---|
| IT operations lead (`IT_MANAGER`) | Fast, accurate day-to-day workflows |
| Company IT admin (`ADMIN`) | Master data control, user administration, auditability |
| End employees (`EMPLOYEE`) | Self-service view of their assets; easy ticket raising |
| Finance / controlling | Warranty expiry visibility, license spend visibility (read-only KPIs) |
| Auditors (external, occasional) | Historical "who had what when" reconstruction |

## 4. Glossary

| Term | Meaning in this system |
|---|---|
| **Asset** | A physical, individually identifiable IT item. A specific laptop with a specific serial number. |
| **Asset model** | A commercial model of asset (e.g. *"ThinkPad T14 Gen 4"*). Many assets share one model. |
| **Asset category** | A broad type: *Laptop, Monitor, Phone, Dock, Peripheral, Other.* |
| **Assignment** | The link between one asset and one person for a period of time. |
| **Software product** | A commercial software product (e.g. *"Microsoft 365 Business Standard"*). |
| **Software license** | A specific purchased entitlement to use a software product, with a seat count. |
| **License assignment** | The allocation of one seat of a license to one person or one asset. |
| **Ticket** | A support request raised by an employee, optionally about a specific asset. |
| **Retire** | Formally end an asset's operational life. It cannot be assigned again. |

## 5. Assumptions

- One legal entity, one currency (EUR), one primary language (English UI first, German later).
- All employees have an email address; email doubles as the login identifier candidate.
- The company operates in Germany; personal data handling follows GDPR principles  (soft-delete, audit log, minimum-necessary fields).
- Fewer than ~10 000 assets and ~500 concurrent users, which a single PostgreSQL instance handles easily.

## 6. Constraints

| Constraint | Origin |
|---|---|
| Stack: Java, Spring Boot, React, PostgreSQL, Docker | Widely used in German enterprise IT and the technologies I want to work with |
| The whole system must run locally with one command | Easy to demonstrate and review |
| No secrets in source code; `.env` is never committed | Basic security practice |
| Built in milestones that each produce a runnable result | Keeps the project testable at every step |

---

## 7. Functional requirements

Numbered `FR-nn`. Each row identifies which role(s) can invoke it.

### 7.1 Authentication & authorization

| # | Requirement | Roles |
|---|---|---|
| FR-01 | Users authenticate with username + password and receive a short-lived JWT access token | all |
| FR-02 | Refresh tokens rotate on use; a refresh token can be revoked | all |
| FR-03 | Passwords are hashed with BCrypt (cost ≥ 10); plaintext passwords never stored | ADMIN (policy) |
| FR-04 | Users have one or more roles; roles determine what endpoints they can call | ADMIN |
| FR-05 | A user account can be disabled without being deleted (soft-disable) | ADMIN |

### 7.2 People & departments

| # | Requirement | Roles |
|---|---|---|
| FR-10 | Create / update / soft-delete a Department, with optional parent department | ADMIN |
| FR-11 | Assign a manager (a person) to a department | ADMIN |
| FR-12 | Create / update / soft-delete a Person (name, email, phone) | ADMIN |
| FR-13 | Attach an Employee record to a Person (employee number, department, hire date, job title, employment status) | ADMIN |
| FR-14 | Link a Person to a User account so they can log in | ADMIN |
| FR-15 | List / search / filter departments and employees with pagination | ADMIN, IT_MANAGER |
| FR-16 | Every employee can see and edit their own contact phone number | EMPLOYEE (self) |

### 7.3 Asset catalogue

| # | Requirement | Roles |
|---|---|---|
| FR-20 | Manage the closed list of asset categories (seeded, editable by ADMIN) | ADMIN |
| FR-21 | Create / update asset models (manufacturer, model name, category) | ADMIN, IT_MANAGER |
| FR-22 | Create a new asset (asset tag, model, serial number, purchase date, purchase price EUR, warranty end, initial status = `IN_STOCK`) | IT_MANAGER, ADMIN |
| FR-23 | Update non-immutable attributes of an asset (notes, warranty end, status transitions) | IT_MANAGER, ADMIN |
| FR-24 | Retire an asset (status → `RETIRED`); once retired, cannot be assigned again | IT_MANAGER, ADMIN |
| FR-25 | List / search / filter assets by category, status, assignee, department, warranty expiry range | IT_MANAGER, ADMIN |
| FR-26 | An asset tag is unique across the company | (system-enforced) |

### 7.4 Assignment & return

| # | Requirement | Roles |
|---|---|---|
| FR-30 | Assign an available asset (`IN_STOCK`) to a Person; captures assigned-by user, assigned-at, expected return date (nullable), out-condition | IT_MANAGER, ADMIN |
| FR-31 | An asset can have **at most one** open assignment at any time (business rule enforced in DB and service) | (system-enforced) |
| FR-32 | Cannot assign a `RETIRED` or `LOST` asset | (system-enforced) |
| FR-33 | Return an assigned asset; captures returned-by user, actual return date, in-condition, notes | IT_MANAGER, ADMIN |
| FR-34 | On return, asset status transitions back to `IN_STOCK` (unless flagged for maintenance, then `UNDER_MAINTENANCE`) | (system-enforced) |
| FR-35 | An employee can see the list of assets currently assigned to them | EMPLOYEE (self) |
| FR-36 | Assignment history for an asset (all past assignments) is visible | IT_MANAGER, ADMIN |

### 7.5 Software licenses

| # | Requirement | Roles |
|---|---|---|
| FR-40 | Create / update a Software Product (vendor, name, version) | ADMIN, IT_MANAGER |
| FR-41 | Create a Software License (product, license key or reference, license type ∈ {PER_SEAT, PER_DEVICE, SITE, SUBSCRIPTION}, seat count, purchase date, expiry date, cost EUR) | ADMIN, IT_MANAGER |
| FR-42 | Assign one seat of a license to either a Person **or** an Asset (mutually exclusive) | IT_MANAGER, ADMIN |
| FR-43 | Cannot exceed the license seat count with active assignments (system-enforced) | (system-enforced) |
| FR-44 | Release a license assignment; that seat becomes available again | IT_MANAGER, ADMIN |
| FR-45 | Alert / KPI: licenses expiring within the next 60 days | IT_MANAGER, ADMIN |

### 7.6 Maintenance

| # | Requirement | Roles |
|---|---|---|
| FR-50 | Record a maintenance event against an asset (performed-at, provider, description, cost EUR, next-scheduled-at) | IT_MANAGER, ADMIN |
| FR-51 | While an asset is `UNDER_MAINTENANCE` it cannot be assigned | (system-enforced) |
| FR-52 | Maintenance history is visible per asset | IT_MANAGER, ADMIN |

### 7.7 Support tickets

| # | Requirement | Roles |
|---|---|---|
| FR-60 | Create a ticket (subject, description, priority ∈ {LOW, MEDIUM, HIGH, CRITICAL}, optionally linked to an asset) | EMPLOYEE, IT_MANAGER, ADMIN |
| FR-61 | Auto-assign a human-readable ticket number, e.g. `TCK-2026-000123` | (system-enforced) |
| FR-62 | Update ticket status through the lifecycle `OPEN → IN_PROGRESS → WAITING → RESOLVED → CLOSED` (state-machine rules apply) | IT_MANAGER, ADMIN |
| FR-63 | Assign a ticket to an IT_MANAGER | IT_MANAGER, ADMIN |
| FR-64 | Add comments to a ticket, with an `is_internal` flag hiding comments from the reporting employee | IT_MANAGER, ADMIN (any comment); EMPLOYEE (non-internal only) |
| FR-65 | Reporter can see all tickets they raised and any comment that is not internal | EMPLOYEE (self) |
| FR-66 | List / filter tickets by status, priority, assignee, related asset | IT_MANAGER, ADMIN |

### 7.8 Dashboards

| # | Requirement | Roles |
|---|---|---|
| FR-70 | ADMIN dashboard: total assets by category and status, active users, ticket volume trend | ADMIN |
| FR-71 | IT_MANAGER dashboard: my open tickets, assets under maintenance, licenses expiring in 60 days, unassigned employees | IT_MANAGER |
| FR-72 | EMPLOYEE dashboard: my assets, my open tickets, my assigned software | EMPLOYEE |

### 7.9 Audit

| # | Requirement | Roles |
|---|---|---|
| FR-80 | Every create / update / delete / assign / return / status change / login is written to an append-only `audit_log` | (system-enforced) |
| FR-81 | ADMIN can query the audit log by actor, entity type, entity id, date range | ADMIN |

---

## 8. Non-functional requirements

Numbered `NFR-nn`. Sized for one company with up to about 500 users.

### 8.1 Performance

| # | Requirement |
|---|---|
| NFR-01 | Any list endpoint returns page 1 (20 rows) in < 300 ms on a laptop, with 10 000 assets seeded |
| NFR-02 | The frontend renders its shell (Time to Interactive on the dashboard) in < 2 s on a modern laptop over localhost |

### 8.2 Scalability

| # | Requirement |
|---|---|
| NFR-10 | Single PostgreSQL instance is sufficient for the target scale (≤ 10 000 assets, ≤ 500 users). No horizontal DB sharding planned. |
| NFR-11 | Stateless backend: sessions live in the JWT, so horizontal scaling of the API is possible without sticky sessions |

### 8.3 Security

| # | Requirement |
|---|---|
| NFR-20 | All API endpoints except `/auth/login` and `/auth/refresh` require authentication |
| NFR-21 | Authorization is enforced at the controller / service layer using `@PreAuthorize`, and additionally by row-level ownership checks for EMPLOYEE requests |
| NFR-22 | Passwords are hashed with BCrypt (cost 10 or higher); no reversible encryption |
| NFR-23 | Secrets (`JWT_SECRET`, DB credentials) live only in `.env` files and environment variables, never in Git |
| NFR-24 | All state-changing endpoints validate input server-side (Bean Validation), regardless of any client validation |
| NFR-25 | SQL access is exclusively through parameterised JPA / JPQL queries, no string-concatenated SQL |

### 8.4 Reliability & data integrity

| # | Requirement |
|---|---|
| NFR-30 | Referential integrity is enforced by foreign keys, not left to the application |
| NFR-31 | The "one active assignment per asset" rule is enforced by a partial-unique index in the database |
| NFR-32 | The "seats available ≥ 0" rule is enforced in the service layer under a transactional lock on the license row |
| NFR-33 | Every domain table has `created_at` and `updated_at` maintained by JPA lifecycle callbacks |

### 8.5 Maintainability

| # | Requirement |
|---|---|
| NFR-40 | Layered architecture (controller → service → repository); controllers contain no business logic |
| NFR-41 | Data-transfer objects (DTOs) separate the API surface from JPA entities; no JPA entity is serialised directly to the client |
| NFR-42 | Schema changes go through Flyway migrations; the entity model is *not* the source of truth for the schema |
| NFR-43 | Every service method with non-trivial branching has a unit test; every important endpoint has an integration test with Testcontainers |

### 8.6 Usability & accessibility

| # | Requirement |
|---|---|
| NFR-50 | UI reachable by keyboard; interactive elements have visible focus |
| NFR-51 | Colour contrast on primary text meets WCAG AA (4.5:1) |
| NFR-52 | Every list view supports search + filter + pagination |
| NFR-53 | Destructive actions require a confirmation dialogue |

### 8.7 Observability

| # | Requirement |
|---|---|
| NFR-60 | Structured JSON logs from the backend; every request logged with a correlation ID |
| NFR-61 | Spring Boot Actuator `/health`, `/info` and `/metrics` endpoints exposed (restricted to ADMIN in non-dev profiles) |

### 8.8 Deployment

| # | Requirement |
|---|---|
| NFR-70 | The full stack (Postgres + backend + frontend + nginx) starts with `docker compose up` on any machine with Docker |
| NFR-71 | Backend and frontend images build reproducibly from the repository, no local state required |

### 8.9 Legal / compliance (GDPR-lite for a student project)

| # | Requirement |
|---|---|
| NFR-80 | Personal data is limited to what's operationally necessary (name, work email, work phone, department, role) |
| NFR-81 | Delete of a Person is a soft-delete (`deleted_at`) so historical audit trail is preserved; hard-delete is a separate ADMIN operation |
| NFR-82 | The audit log records the actor for every state change |

---

## 9. In scope for Project 1

- Everything in §7 and §8 above.
- Local Docker-based deployment.
- English UI only.
- Manual user creation by ADMIN.

## 10. Explicitly out of scope for Project 1

*(Kept for future projects or explicit "future improvements" section.)*

- Multi-tenancy / multi-company support.
- SSO / SAML / OIDC integration (would replace `NFR-20`).
- Mobile app.
- QR-code / barcode label printing and scanning.
- Automatic asset discovery via network agents.
- Slack / Teams / email notifications on ticket state changes.
- Financial reporting integration with SAP or DATEV.
- Full ERP procure-to-pay for asset purchasing (part of my separate ERP project).

## 11. Acceptance criteria for Milestone 1 (this milestone)

Milestone 1 is documentation and design only. It is done when:

- [ ] All five docs (`requirements.md`, `use-cases.md`, `architecture.md`, `database-design.md`, `development-roadmap.md`) exist and are internally consistent.
- [ ] The ER diagram (`docs/diagrams/er-diagram.mmd`) covers every entity in the database design.
- [ ] The DDL script (`database/schema.sql`) creates every table listed in the database design, with primary keys, foreign keys, uniqueness constraints, check constraints and indexes.
- [ ] The DDL script runs against a fresh PostgreSQL 16 database with zero errors *(will be validated in Milestone 2 when Postgres is available)*.

Every subsequent milestone will have its own acceptance criteria in `development-roadmap.md`.
