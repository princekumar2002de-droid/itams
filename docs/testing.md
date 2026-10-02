# Testing

## Overview

| Level | Tools | Count | What it covers |
|---|---|---|---|
| Backend unit | JUnit 5, Mockito, AssertJ | 70 | Business rules and state machines, without Spring |
| Backend integration | Spring Boot Test, MockMvc, Testcontainers (PostgreSQL 16) | 42 | The real application against a real database, through the full security chain |
| Frontend | Vitest, Testing Library, jsdom | 31 | API client, auth context, route guards, pages and dialogs |
| Manual | Browser against the Docker Compose stack | per role | Complete workflows as ADMIN, IT_MANAGER and EMPLOYEE |

```bash
cd backend && mvn verify        # unit + integration tests (needs Docker)
cd frontend && npm test         # frontend tests
```

## Backend unit tests

These run in about a second and need nothing but the JDK.

- **State machines:** asset (`IN_STOCK → ASSIGNED → …`, retirement rules, maintenance) and ticket transitions
- **Tickets:** numbering, and the employee scoping rules (own tickets only, 404 otherwise, no internal comments)
- **Licences:** a seat goes to either a person or a device, no allocation beyond the seat count, release and re-release
- **Maintenance:** a record needs a performer (internal user or external provider)
- **Authentication:** the same error for unknown user, wrong password and disabled account; refresh rotation within a token family; reuse of a rotated token revokes the family, while a logged-out token does not
- **Rate limiting:** sliding window per IP, other endpoints unaffected
- **Audit aspect:** entity id extraction and the shape of the audit row

## Backend integration tests

Each test class starts a PostgreSQL 16 container, runs the Flyway migrations and calls the API through MockMvc with the real security configuration.

- `ItamsApplicationIT`: the context starts and Hibernate validates the schema against the migrations
- `DepartmentControllerIT`: create, read, update, delete, validation and conflicts; audit rows are written for create and update
- `AssetAssignmentFlowIT`: assign, return and retire an asset; the maintenance cycle (return into maintenance, assignment blocked, maintenance finished, back in stock)
- `AuthControllerIT`: login, refresh rotation, reuse of an old refresh token revoking the newer one, other sessions surviving, logout with an expired access token, `/me`
- `AuthorizationIT`: 18 role × endpoint cases, expecting 401 for anonymous calls and 403 for wrong roles

## Frontend tests

- API client: bearer header, refresh on 401 followed by a retry, a single refresh for parallel requests, clearing the session when the refresh fails, mapping error bodies
- `AuthProvider`: restoring the session, role checks, clearing cached data on logout
- Route guard and sidebar: pages and menu entries per role
- Ticket detail: only allowed status transitions are offered, employees get no workflow controls
- Dialogs: raise ticket, record maintenance (internal or external performer), onboard employee (a failed second step does not create the person twice)

To check that the tests can actually fail, I broke some of the behaviour on purpose (removing the single-flight refresh, the cache clear on logout, the role filter in the sidebar) and confirmed that the matching test went red.

## Bugs the tests found

Running the integration tests against a real database, and testing every role by hand, found problems that the unit tests could not see:

| Problem | Cause | Fix |
|---|---|---|
| Wrong role got 500 instead of 403 | `@PreAuthorize` throws inside the DispatcherServlet, so the generic `@ExceptionHandler(Exception.class)` caught it first | Separate handler for `AccessDeniedException` |
| Audit entries were never stored | `String` field mapped to a `jsonb` column was sent as `varchar`; the aspect only logged the error | `@JdbcTypeCode(SqlTypes.JSON)` and a test that counts audit rows |
| `LazyInitializationException` on several endpoints | Open-session-in-view is off, and the controllers mapped lazy relations after the transaction | `@EntityGraph` on the repository methods |
| Creating a licence without product version failed | A null JPQL parameter was sent untyped, so PostgreSQL saw `lower(bytea)` | Two derived queries, chosen by whether a version is given |
| Logout failed after the access token expired | The endpoint required authentication | Logout is authorised by the refresh token only |
| A repaired asset could never go back to stock | The domain method existed but no endpoint called it | New `maintenance-complete` endpoint |
| A second user in the same tab briefly saw the first user's data | The frontend cache was not cleared on logout (the API correctly returned 404) | Clear the cache on login, logout and session expiry |

## Environment notes

- Docker Engine 29 rejects the API version used by Testcontainers 1.21.3; 1.21.4 or newer is required.
- With colima on macOS, set `DOCKER_HOST=unix://$HOME/.colima/default/docker.sock` and `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` before `mvn verify`.
- Node 25 ships its own experimental `localStorage`, which hides the jsdom one; the Vitest config starts workers with `--no-experimental-webstorage`.
