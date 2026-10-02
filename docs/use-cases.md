# Use cases: IT Asset Management System

**Document status:** v0.1 · Milestone 1 · 2026-09-21

Each use case follows the same short shape: **actor · precondition · main flow · alternate flows · postcondition**. This is shorter than the classic Cockburn format: enough to drive the API and UI design without becoming hard to maintain. Use cases marked *not implemented* were planned but are not part of the current version.

## Actors

- **ADMIN**: system administrator, full access.
- **IT_MANAGER**: IT operations, does the day-to-day work.
- **EMPLOYEE**: normal user in the company.
- **System**: automatic behaviour (audit log writer, token issuing).

---

## UC-01 · Log in
**Actor:** any authenticated role
**Precondition:** user account exists and is enabled
**Main flow:**
1. User submits username + password to `POST /auth/login`.
2. Backend verifies the BCrypt hash.
3. Backend returns an access JWT (~15 min) and a refresh token (~7 days).
4. Client stores tokens in memory + HTTP-only cookie for refresh.
**Alternates:**
- Wrong password → HTTP 401 with a generic message, so usernames cannot be guessed.
- Account disabled → HTTP 403 "account disabled".
**Postcondition:** user session established; login event written to `audit_log`.

## UC-02 · Refresh access token
**Actor:** authenticated user
**Main flow:**
1. Client sends the refresh token to `POST /auth/refresh`.
2. Server rotates it (issues new access + new refresh, invalidates old refresh).
**Alternates:** revoked / expired refresh → 401, force re-login.

## UC-03 · Log out
**Actor:** authenticated user
**Main flow:** `POST /auth/logout` marks the current refresh token as revoked.

---

## UC-10 · ADMIN creates a department
**Actor:** ADMIN
**Precondition:** parent department (if given) exists and is not soft-deleted
**Main flow:**
1. ADMIN sends `POST /departments` with `{ code, name, parentDepartmentId?, managerPersonId? }`.
2. `code` uniqueness is checked.
3. Department is created; audit logged.

## UC-11 · ADMIN creates an employee
**Actor:** ADMIN
**Precondition:** referenced department exists
**Main flow:**
1. ADMIN creates a Person (name, email, phone).
2. ADMIN attaches an Employee record (employee number, department, job title, hire date).
3. Optionally creates a User account linked to the Person and assigns one or more roles.
**Alternates:** duplicate employee number → 409 Conflict.

## UC-12 · EMPLOYEE updates own phone *(not implemented)*
**Actor:** EMPLOYEE
**Main flow:** the employee changes their own phone number. Today an ADMIN changes contact details through `PATCH /people/{id}`.

---

## UC-20 · IT_MANAGER adds a new asset to the catalogue
**Actor:** IT_MANAGER
**Precondition:** the asset model exists (create it first if not)
**Main flow:**
1. `POST /assets` with `{ assetTag, modelId, serialNumber, purchaseDate, purchasePriceCents, warrantyEndsOn }`.
2. Backend validates: unique `assetTag`, unique `serialNumber` per model, `purchaseDate` ≤ today, price ≥ 0.
3. Asset is created with status `IN_STOCK`. Audit logged.

## UC-21 · IT_MANAGER edits asset notes / warranty
**Actor:** IT_MANAGER
**Main flow:** `PATCH /assets/{id}` with allowed fields only. Immutable fields (`assetTag`, `serialNumber`) rejected.

## UC-22 · IT_MANAGER retires an asset
**Actor:** IT_MANAGER
**Precondition:** asset has no open assignment
**Main flow:**
1. `POST /assets/{id}/retire` with `{ reason }`.
2. Asset moves to status `RETIRED`; asset cannot be assigned again (system-enforced).
**Alternates:** asset still assigned → 409, must be returned first.

---

## UC-30 · IT_MANAGER assigns an asset to an employee
**Actor:** IT_MANAGER
**Precondition:**
- Asset exists and status is `IN_STOCK`
- Assignee Person exists and is active
**Main flow:**
1. `POST /assignments` with `{ assetId, assigneePersonId, expectedReturnOn?, outCondition, notes? }`.
2. Backend obtains a transactional row lock on the asset.
3. Backend verifies no open assignment exists for this asset (partial-unique DB index also enforces this).
4. Assignment is created; asset status transitions `IN_STOCK → ASSIGNED`.
5. Audit logged.
**Alternates:**
- Asset already assigned → 409.
- Asset status is `RETIRED`, `LOST`, or `UNDER_MAINTENANCE` → 409.

## UC-31 · IT_MANAGER records the return of an asset
**Actor:** IT_MANAGER
**Precondition:** an open assignment exists for the asset
**Main flow:**
1. `POST /assignments/{id}/return` with `{ actualReturnOn, inCondition, sendForMaintenance?: bool, notes? }`.
2. Open assignment is closed (`actualReturnOn`, `inCondition` set).
3. Asset status transitions to `IN_STOCK` (or `UNDER_MAINTENANCE` if flagged).
4. Audit logged.

## UC-32 · EMPLOYEE views own assigned assets *(not implemented as a separate view)*
**Actor:** EMPLOYEE
**Main flow:** a "my devices" page listing only the employee's open assignments. Today employees see the general asset and assignment lists.

---

## UC-40 · IT_MANAGER records a software license
**Actor:** IT_MANAGER
**Main flow:**
1. Create the SoftwareProduct if new.
2. `POST /licenses` with `{ productId, licenseKey/reference, licenseType, seatsTotal, purchaseDate, expiresOn, costCents }`.

## UC-41 · IT_MANAGER assigns a license seat
**Actor:** IT_MANAGER
**Main flow:**
1. `POST /licenses/{id}/assignments` with either `{ personId }` or `{ assetId }`, never both.
2. Backend obtains a transactional lock on the license row.
3. Backend counts open assignments; if `count < seatsTotal`, allocate. Else 409.
4. Audit logged.

## UC-42 · IT_MANAGER releases a license seat
**Actor:** IT_MANAGER
**Main flow:** `POST /licenses/assignments/{id}/release`. The release time is recorded and the seat becomes available.

---

## UC-50 · IT_MANAGER records a maintenance event
**Actor:** IT_MANAGER
**Precondition:** asset is not `RETIRED`
**Main flow:**
1. `POST /maintenance` with `{ assetId, performedOn, performedByUserId? | providerName?, description, cost?, nextScheduledOn? }`. Exactly one performer is required: an internal user or an external provider.
2. An asset reaches `UNDER_MAINTENANCE` when it is returned with the maintenance flag; `POST /assets/{id}/maintenance-complete` puts it back to `IN_STOCK`.
3. Audit logged.

---

## UC-60 · EMPLOYEE raises a support ticket
**Actor:** EMPLOYEE
**Main flow:**
1. `POST /tickets` with `{ subject, description, priority, relatedAssetId? }`.
2. For an EMPLOYEE the reporter is always set to the employee, whatever the request says.
3. Backend generates `ticketNumber` (e.g. `TCK-2026-000123`), sets status `OPEN`.
4. Audit logged.

## UC-61 · IT_MANAGER picks up and works on a ticket
**Actor:** IT_MANAGER
**Main flow:**
1. `PATCH /tickets/{id}` with `{ assignedToUserId }` to take the ticket, and `{ priority }` to change its priority.
2. `POST /tickets/{id}/status` with `{ to: 'IN_PROGRESS' }`; it may go to `WAITING` and back while waiting for the employee.
3. `{ to: 'RESOLVED' }` when solved, then `{ to: 'CLOSED' }`. Closing is manual; automatic closing after a few days without reply is not implemented.

## UC-62 · IT_MANAGER comments internally on a ticket
**Main flow:** `POST /tickets/{id}/comments` with `{ body, internal: true }`. Internal comments never appear in the EMPLOYEE view.

## UC-63 · EMPLOYEE comments on their own ticket
**Main flow:** `POST /tickets/{id}/comments` with `{ body }`. The server always stores employee comments as not internal, whatever the client sends.

---

## UC-70 · Any authenticated user opens their dashboard
**Actor:** any
**Main flow:** `GET /stats/dashboard` returns the same KPIs and breakdowns for every role. Role-specific dashboards (for example personal KPIs for employees) are not implemented.

## UC-80 · ADMIN queries the audit log *(not implemented)*
**Actor:** ADMIN
**Main flow:** search the audit log by user, entity and time range. The audit rows are written today, but there is no endpoint or page to search them yet.

---

## State machines summarised

**Asset status:**

```
IN_STOCK  ──assign──►  ASSIGNED  ──return──►  IN_STOCK
   │                       │  ──return+flag──►  UNDER_MAINTENANCE
   │                       │                       │
   │  ◄────maintenance complete────────────────────┘
   │
   ├──retire──►  RETIRED         (terminal)
   └──mark lost──►  LOST         (terminal)
```

**Ticket status:**

```
OPEN ──pick up──► IN_PROGRESS ──need info──► WAITING ──reply──► IN_PROGRESS
                       │                                            │
                       └───────── resolve ──────────► RESOLVED ─────┘
                                                        │
                                                     close ──► CLOSED
```

Both state machines live in the entities (`Asset`, `Ticket`) and are called from the services. `LOST` exists as a status but there is no action that sets it yet.
