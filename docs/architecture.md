# Architecture

This document describes how ITAMS is put together and why. Where I chose one technology over another, the alternative and the reason are listed in section 8.

## 1. Context

```
        ┌───────────────┐                      ┌────────────────────────────┐
        │  ADMIN        │      HTTP · JSON      │                            │
        │  IT_MANAGER   │ ◄──────────────────► │   ITAMS                    │
        │  EMPLOYEE     │      Bearer JWT       │                            │
        └───────────────┘                      └─────────────┬──────────────┘
                                                             ▼
                                                        PostgreSQL
```

ITAMS has no integrations with other systems. Three roles use one application.

## 2. Containers

```
┌──────────────────────────────────────────────────────────────┐
│ Browser: React SPA (React 18, TypeScript, Tailwind,          │
│          React Router, TanStack Query)                       │
└───────────────────────────┬──────────────────────────────────┘
                            │ /api/v1/... · Bearer JWT
┌───────────────────────────▼──────────────────────────────────┐
│ nginx: serves the built SPA, proxies /api to the backend     │
└───────────────────────────┬──────────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────────┐
│ Spring Boot 3.3 on Java 21: REST, Spring Security + JWT,     │
│ Spring Data JPA, Bean Validation, Flyway, springdoc          │
└───────────────────────────┬──────────────────────────────────┘
                            │ JDBC
┌───────────────────────────▼──────────────────────────────────┐
│ PostgreSQL 16: one database, schema owned by Flyway          │
└──────────────────────────────────────────────────────────────┘
```

Because nginx serves the frontend and the API under one origin, the browser never makes cross-origin requests and no CORS configuration is needed. In development the Vite dev server does the same job.

## 3. Backend

### Layers

| Layer | Responsibility | Does not |
|---|---|---|
| Controller (`@RestController`) | HTTP: request/response DTOs, status codes, validation, `@PreAuthorize` | contain business logic |
| Service (`@Service`, `@Transactional`) | business rules, state transitions, ownership checks, audit annotations | know about HTTP |
| Repository (Spring Data JPA) | queries, fetch plans (`@EntityGraph`) | contain business logic |
| Entities | domain state and state machines (e.g. `Asset`, `Ticket`) | define the schema (Flyway does) |

- Controllers stay thin so a rule is not implemented twice, once in the HTTP layer and once somewhere else.
- The service method is the transaction: "assign an asset" touches several rows and must succeed or fail as a whole.
- The API returns DTOs, never entities, so the database can change without breaking clients.
- Open-session-in-view is switched off. Each repository method states which relations it loads with `@EntityGraph`, which keeps the queries predictable.

### Packages

Organised by feature, so everything about one topic is in one folder:

```
com.princekumar.itams
├── auth/          login, refresh, logout, JWT, rate limiting, current user
├── user/          user accounts, roles, refresh tokens
├── person/        people (with or without a login)
├── department/
├── employee/
├── asset/         assets, models, categories
├── assignment/    assignment and return of assets
├── license/       software products, licences, seat assignments
├── maintenance/
├── ticket/        tickets and comments
├── stats/         dashboard
├── common/        base entity, error handling, DTO helpers, audit aspect
└── config/        security, OpenAPI, demo data
```

## 4. Frontend

```
src/
├── api/           typed functions per backend resource, DTO types
├── auth/          AuthProvider, ProtectedRoute, usePermissions
├── components/    layout, small UI components, form building blocks, dialogs
├── pages/         one component per route
└── lib/           fetch wrapper, query client, formatters
```

- **Server data:** TanStack Query handles caching, loading states and invalidation after changes. The cache is cleared on login and logout.
- **UI state:** local `useState`; no global store is needed at this size.
- **Auth state:** a React context with the current user and a `hasRole` helper.
- **API client:** a small wrapper around `fetch` that adds the token, refreshes it once on a 401 (one refresh shared by parallel requests) and converts error bodies into a typed `ApiError`.
- **Errors:** pages and dialogs show the server's error message through a shared `ErrorState` component.

## 5. Cross-cutting concerns

### 5.1 Security

- **Passwords:** BCrypt through `DelegatingPasswordEncoder`; the `{bcrypt}` prefix allows a later switch to Argon2.
- **Tokens:** access JWT (HS256, 15 minutes) in the `Authorization` header; refresh token of 512 random bits, stored only as a SHA-256 hash, valid once and rotated on every refresh. Tokens of one login form a family; reusing an old token revokes the whole family.
- **Login:** same error for every kind of failure (no username enumeration), rate-limited per IP.
- **Authorisation:** `@PreAuthorize` on every controller method and a default-deny rule in `SecurityConfig`. Employees only reach their own tickets; others return 404.
- **Input:** Bean Validation on every request DTO; all queries are parameterised.
- **Client:** tokens are kept in `localStorage`, which is simple but readable by page scripts. nginx sends a strict Content-Security-Policy to reduce the XSS risk. An `HttpOnly` cookie with CSRF protection would be the stronger option.
- **Transport:** plain HTTP inside the Compose network. A real deployment needs a TLS-terminating proxy in front.
- **Secrets:** environment variables only; the `prod` profile has no defaults for the JWT secret or database credentials.

Why JWT and not server sessions: the backend stays stateless, and the server-side refresh-token table still allows logout and revocation without a session store. The cost is that an access token stays valid until it expires (at most 15 minutes).

### 5.2 Auditing

Service methods that change data carry an `@AuditWrite(entity, action)` annotation. `AuditAspect` writes one `audit_log` row after the method returns: user, entity type and id, action, timestamp and request id.

The row is written after the business transaction has committed. If the audit insert fails, the change stays and the failure is logged as an error. Writing the audit row inside the same transaction would make both succeed or fail together; that is on the list of next steps.

Besides the audit log, the domain tables keep their own history fields: assignment and return dates and users, ticket resolution and closing times, `created_at` / `updated_at` on every table (kept current by a database trigger).

### 5.3 Testing

| Level | Tools | Examples |
|---|---|---|
| Domain and services | JUnit 5, Mockito, AssertJ | asset and ticket state machines, licence seat rules, auth rules |
| API with database | `@SpringBootTest`, MockMvc, Testcontainers | assignment flow, auth flow, role matrix, audit rows |
| Frontend | Vitest, Testing Library | API client, role-based UI, dialogs |

Details in [testing.md](testing.md). Simple getters, setters and DTO mappers are not tested on their own; the integration tests cover them indirectly.

### 5.4 Logging and monitoring

- Plain text logs; `DEBUG` for the application package in development, `INFO` in production.
- Actuator exposes only `health` and `info`; in `prod` the health details are hidden.
- Each audit row stores the `X-Request-Id` header if the client sends one.

Not included: JSON logs, generated correlation ids for every request and a metrics endpoint for Prometheus. A production system should have them.

### 5.5 Error handling

One `@RestControllerAdvice` turns every exception into the same `ErrorResponse` (`code`, `message`, `status`, `path`, `timestamp`, field errors):

| Exception | Status |
|---|---|
| validation errors, malformed JSON, wrong parameter types, `IllegalArgumentException` | 400 |
| authentication errors | 401 |
| `AccessDeniedException` | 403 |
| `ResourceNotFoundException` | 404 |
| `BusinessRuleViolationException`, `IllegalStateException`, database constraint violations | 409 |
| anything else | 500 with a generic message; details only in the log |

## 6. Deployment

```
docker compose
┌────────────────────┐   ┌────────────────────┐   ┌────────────────────┐
│ postgres :5432     │◀──│ backend :8080      │◀──│ frontend :3000     │
│ postgres:16-alpine │   │ Spring Boot (JRE)  │   │ nginx + SPA        │
└────────────────────┘   └────────────────────┘   └────────────────────┘
        healthy  ──────────▶  healthy  ──────────▶  starts
```

Each service has a health check, and each one waits for the previous one to be healthy. Both application images are multi-stage builds; the backend runs as a non-root user with the `prod` profile.

## 7. Not in scope

- SSO; only local username and password
- Email notifications
- File uploads (photos, invoices)
- Translations; the UI is English only

## 8. Decisions and alternatives

| Decision | Chosen | Alternatives | Reason |
|---|---|---|---|
| Language | Java 21 | Kotlin | Java is what most German enterprise teams use; 21 is the current LTS with records and pattern matching |
| Framework | Spring Boot 3.3 | Quarkus, Micronaut | The most common choice in German enterprise IT and the one covered in my studies |
| Migrations | Flyway | Liquibase | Plain SQL files are easier to review than XML changelogs |
| Frontend build | Vite | Create React App, Next.js | CRA is no longer maintained; Next.js adds server rendering this app doesn't need |
| Styling | Tailwind CSS | CSS Modules, styled-components | Fast to work with, consistent, no runtime cost |
| Server state | TanStack Query | Redux Toolkit Query, plain `useEffect` | Built for exactly this; caching and invalidation come for free |
| Authentication | JWT with refresh tokens | Sessions in Redis | Stateless backend, revocation through the refresh-token table |
| Data access | Spring Data JPA (Hibernate) | jOOQ, MyBatis, JDBC | Standard in Spring Boot and enough for this domain |
| Runtime | Docker Compose | Kubernetes | Kubernetes would be overkill for three containers on one machine |

## 9. Risks

| Risk | How I handled it |
|---|---|
| First larger Spring Boot project, so the effort was hard to estimate | Built one complete feature (departments) end to end before adding the others |
| Integration tests with real databases slow down the feedback loop | Unit tests run on their own with `mvn test`; integration tests run with `mvn verify` |
| Scope creep towards a full ERP system | The out-of-scope list in the requirements; ERP topics belong to a separate project |
| Security details are easy to get wrong | Established libraries (jjwt, Spring Security), integration tests for the security rules, documented gaps |
