# Development roadmap

I built ITAMS in milestones. Each one ended with something that could be run and checked, and later milestones built on the earlier ones. The detailed list of changes is in the [changelog](../CHANGELOG.md).

| Milestone | Content | Version |
|---|---|---|
| M1 | Requirements, use cases, architecture, database design | 0.1.0 |
| M2 | Spring Boot backend, Flyway migrations, first feature slices | 0.2.0 |
| M3 | Authentication and role-based authorisation | 0.3.0 |
| M4 | React and TypeScript frontend | 0.4.0 |
| M5 | Dashboard, licences, maintenance, tickets | 0.5.0 |
| M6 | Docker images, Compose stack, production profile | 0.6.0 |
| M7 | Row-level access for employees, audit log | 0.7.0 |
| M8 | Integration tests on a real database, security hardening, CI | 0.8.0 |
| M9 | Role-based UI for all workflows | 1.0.0 |

## M1: Design first

Before writing code I wrote down what the system should do and why, so later technical decisions could be traced back to a requirement: [requirements](requirements.md), [use cases](use-cases.md), [architecture](architecture.md), [database design](database-design.md) and the [ER diagram](diagrams/er-diagram.mmd).

## M2: Backend foundation

The goal was to prove the whole chain (database, migrations, JPA, HTTP, validation, tests, Docker) with one entity before copying the pattern.

- Spring Boot 3.3, Java 21, Maven, PostgreSQL 16 in Docker, Flyway migrations
- Feature packages: `department`, `person`, `employee`, `asset` (with categories and models), `assignment`
- "One open assignment per asset" enforced by a partial unique index plus a row lock
- A common error format for all failures

## M3: Authentication and authorisation

- Login, refresh, logout and `/me` with JWT access tokens and hashed, rotating refresh tokens
- BCrypt passwords, `@PreAuthorize` on every endpoint
- Demo users for local development; the application refuses to start without `JWT_SECRET`

## M4: Frontend

- React 18, TypeScript, Vite, Tailwind, TanStack Query
- Login, role-based route guards and a typed API client that refreshes the token automatically

## M5: Operations features

- Dashboard endpoint with KPIs and breakdowns in one call
- Licences with seat allocation that is safe under concurrent requests
- Maintenance records and support tickets with a status workflow

## M6: Deployment

- Multi-stage Dockerfiles, non-root backend image, health checks
- Compose stack that starts database, backend and frontend in the right order
- `prod` profile that fails fast and hides internal details; rate-limited login

## M7: Data access and audit

- Employees only see and create their own tickets
- Audit log written by an AOP aspect on the service layer

## M8: Testing and hardening

- Integration tests running against PostgreSQL with Testcontainers; they found and fixed several bugs (see [testing](testing.md))
- Refresh-token reuse detection, Content-Security-Policy, frontend tests
- GitHub Actions workflow and Dependabot

## M9: Complete UI

- Every business workflow available in the UI, with buttons and menus per role
- New pages for ticket details, licence details and departments
- Endpoint to finish maintenance and return an asset to stock

## Not planned for this project

Caching layers, message queues, multi-tenancy, a mobile app and cloud hosting. They would add complexity the scale of this project does not need. Some of these topics come back in my larger ERP project.

## Possible next steps

- Audit entry in the same transaction, with before/after values
- Refresh token in an `HttpOnly` cookie
- MFA for admins and TLS behind a reverse proxy
- Playwright end-to-end tests
- Major frontend dependency upgrades
