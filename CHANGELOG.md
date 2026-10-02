# Changelog

All notable changes to ITAMS. The format is based on [Keep a Changelog](https://keepachangelog.com/). Versions follow the project milestones.

## [1.0.0] – 2026-10-02

### Added
- UI for all business workflows, shown per role: register assets and models, edit and retire assets, record maintenance and finish it, create licences and assign or release seats, ticket detail page with status changes, priority, assignment and internal notes, onboarding and offboarding of employees, a departments page.
- `POST /api/v1/assets/{id}/maintenance-complete` to bring a repaired asset back into stock.
- Role-based navigation and route guards in the frontend.
- Shared form components (`FormField`, `FormDialog`, `ConfirmDialog`) with labels linked to their inputs.
- Frontend tests for role visibility, the ticket workflow, form rules and the logout cache clear (31 frontend tests in total).

### Fixed
- An asset sent to maintenance could not be returned to stock; the domain method existed but was not exposed.
- The frontend data cache survived a logout, so the next user in the same browser tab could briefly see cached data of the previous user. The cache is now cleared on login, logout and session expiry. (The API itself was not affected.)
- Employees saw menu entries for pages they are not allowed to open.

## [0.8.0] – 2026-10-02

### Added
- Refresh-token reuse detection using token families (migration `V4__refresh_token_family.sql`).
- Strict Content-Security-Policy for the frontend; security headers moved to `security-headers.conf`.
- Frontend tests with Vitest and Testing Library.
- GitHub Actions workflow (backend tests, frontend checks, Docker image builds) and Dependabot configuration.
- `LICENSE`, `SECURITY.md` and this changelog.

### Changed
- Testcontainers 1.20.2 → 1.21.4, required for Docker Engine 29.

### Fixed
- Requests with the wrong role returned 500 instead of 403, because `@PreAuthorize` denials reached the generic exception handler.
- Audit entries were not stored: `audit_log.changes` is `jsonb` but was bound as `varchar`.
- `LazyInitializationException` when returning assets, models, employees, tickets, licences, assignments and maintenance records (open-session-in-view is disabled); fixed with `@EntityGraph` on the repositories.
- Creating a licence for a product without a version failed (`lower(bytea)` on a null query parameter).
- Logout required a valid access token; it now only needs the refresh token.
- A test helper annotated with `@Entity` was picked up by the entity scan and broke the integration tests.
- Frontend type errors that stopped `npm run build`; the frontend Dockerfile did not copy `index.html`.
- nginx did not send security headers for files under `/assets/`.
- The frontend health check used `localhost` (resolves to IPv6) while nginx listens on IPv4.

## [0.7.0] – 2026-10-01

### Added
- Row-level access for employees on tickets: they can only create tickets for themselves, see their own tickets, get 404 for others and never see internal comments.
- Audit log: `@AuditWrite` annotation and an AOP aspect that writes an `audit_log` row after each annotated service method (20 methods across 8 modules).
- `CurrentUser` helpers `personId()`, `roles()` and `hasOnlyRole()`.

### Fixed
- `TicketServiceTest` passed constructor arguments in the wrong order and did not compile.
- The login rate-limit filter could not serialise its 429 response (missing `JavaTimeModule`) and would have answered with 500.

## [0.6.0] – 2026-09-24

### Added
- Multi-stage Dockerfiles for backend (JDK build, JRE runtime, non-root) and frontend (Node build, nginx runtime with `/api` proxy).
- Full Docker Compose stack with health-check ordering; pgAdmin as an optional profile.
- `prod` profile that fails fast on missing configuration and hides internal details.
- Login rate limiting per IP.
- Unit tests for the services added in 0.5.0 and for login without username enumeration.

### Changed
- Exception handler maps `IllegalStateException`, `IllegalArgumentException` and request parsing errors to proper 4xx responses.

## [0.5.0] – 2026-09-23

### Added
- Dashboard endpoint `GET /api/v1/stats/dashboard` with 8 KPIs and 4 breakdowns.
- Software licences with seat allocation (`SELECT … FOR UPDATE` on the licence row).
- Maintenance records, performed internally or by an external provider.
- Support tickets with a status workflow and numbers like `TCK-2026-000123`.
- Frontend pages for dashboard, licences, maintenance and tickets; assign, return and raise-ticket dialogs.

## [0.4.0]

### Added
- React and TypeScript frontend with 10 routes, login, role-based route guards and a typed `fetch` wrapper that refreshes the token on 401.
- TanStack Query for server state; small set of Tailwind components.

## [0.3.0]

### Added
- JWT authentication (15-minute access token, 7-day refresh token stored as a hash, rotated on use), BCrypt passwords.
- `@PreAuthorize` rules on every controller method.
- Demo users for local development.
- Integration tests for the login flow and the role matrix.

## [0.2.0]

### Added
- Spring Boot backend with Flyway migrations and reference data.
- Departments, people, employees, assets (with categories and models) and assignments.
- One open assignment per asset, enforced by a partial unique index and a row lock.

## [0.1.0]

### Added
- Requirements, use cases, architecture, database design, ER diagram and roadmap.
