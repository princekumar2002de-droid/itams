# ITAMS backend

Spring Boot 3.3, Java 21, PostgreSQL 16, Flyway, Spring Security with JWT, Testcontainers.

## Getting started

### With Docker

```bash
cd ../docker
cp .env.example .env        # set JWT_SECRET
docker compose up --build -d
```

The API runs on http://localhost:8080. The full stack is described in the main [README](../README.md#13-deployment).

### Local development

Needs Java 21 and Maven. The database runs in Docker, the backend from Maven:

```bash
cd ../docker
cp .env.example .env
docker compose up -d postgres

set -a && source .env && set +a     # export the variables into the shell

cd ../backend
mvn spring-boot:run
```

On first start Flyway runs the migrations and three demo users are created:

```
[DevBootstrap] Seeded admin (role: ADMIN)
[DevBootstrap] Seeded itmanager (role: IT_MANAGER)
[DevBootstrap] Seeded employee (role: EMPLOYEE)
```

## Demo users

Created by `DevBootstrap` in every profile except `prod`. The password comes from `BOOTSTRAP_DEMO_PASSWORD` (default `changeme`), so change it before showing the app to anyone.

| User | Role |
|---|---|
| `admin` | ADMIN |
| `itmanager` | IT_MANAGER |
| `employee` | EMPLOYEE |

## Trying the API

```bash
BASE=http://localhost:8080/api/v1

# Log in
LOGIN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"changeme"}')
ACCESS=$(echo "$LOGIN" | jq -r '.accessToken')
REFRESH=$(echo "$LOGIN" | jq -r '.refreshToken')

# Current user
curl -s -H "Authorization: Bearer $ACCESS" $BASE/auth/me | jq

# Dashboard: all KPIs and breakdowns in one call
curl -s -H "Authorization: Bearer $ACCESS" $BASE/stats/dashboard | jq

# Create a licence (the product is created if it doesn't exist) and assign a seat
curl -s -X POST $BASE/licenses -H "Authorization: Bearer $ACCESS" -H 'Content-Type: application/json' \
  -d '{"vendor":"Microsoft","productName":"Office 365","licenseReference":"MS-O365-2026",
       "licenseType":"SUBSCRIPTION","seatsTotal":25,"purchaseDate":"2026-01-15",
       "expiresOn":"2027-01-15","cost":3000.00}' | jq
curl -s -X POST $BASE/licenses/1/assignments -H "Authorization: Bearer $ACCESS" -H 'Content-Type: application/json' \
  -d '{"personId":2}' | jq

# Raise a ticket and move it to IN_PROGRESS
curl -s -X POST $BASE/tickets -H "Authorization: Bearer $ACCESS" -H 'Content-Type: application/json' \
  -d '{"reporterPersonId":2,"subject":"Laptop overheating","description":"Fans loud under load","priority":"MEDIUM"}' | jq
curl -s -X POST $BASE/tickets/1/status -H "Authorization: Bearer $ACCESS" -H 'Content-Type: application/json' \
  -d '{"to":"IN_PROGRESS"}' | jq

# Rotate the refresh token, then log out
curl -s -X POST $BASE/auth/refresh -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$REFRESH\"}" | jq
```

Swagger UI with all endpoints: http://localhost:8080/swagger-ui.html (use **Authorize** and paste the access token).

Some behaviour worth knowing:

- Ticket numbers look like `TCK-2026-000001`. An illegal status change (for example `OPEN` to `RESOLVED`) returns `409 ticket.illegal_transition`; the allowed transitions are defined in `Ticket.java`.
- Seat allocation locks the licence row, so two parallel requests cannot both take the last seat.
- Using a refresh token a second time revokes the whole login session it belongs to.

## Permissions

| Endpoints | ADMIN | IT_MANAGER | EMPLOYEE | Anonymous |
|---|:-:|:-:|:-:|:-:|
| `POST /auth/login`, `/auth/refresh`, `/auth/logout` | ✓ | ✓ | ✓ | ✓ |
| `GET /auth/me` | ✓ | ✓ | ✓ | |
| `/actuator/health`, Swagger UI | ✓ | ✓ | ✓ | ✓ |
| Read departments, people, employees | ✓ | ✓ | | |
| Write departments, people, employees | ✓ | | | |
| Read asset categories, models, assets, assignments | ✓ | ✓ | ✓ | |
| Write models, assets (incl. retire, maintenance done), assignments | ✓ | ✓ | | |
| Dashboard | ✓ | ✓ | ✓ | |
| Read licences and seat assignments | ✓ | ✓ | ✓ | |
| Create licences, assign and release seats | ✓ | ✓ | | |
| Read maintenance | ✓ | ✓ | ✓ | |
| Record and delete maintenance | ✓ | ✓ | | |
| Read tickets and comments | ✓ | ✓ | own only | |
| Raise tickets, add comments | ✓ | ✓ | own only | |
| Change ticket status, priority, assignee | ✓ | ✓ | | |

Anonymous calls get `401 auth.unauthenticated`, wrong roles get `403 auth.forbidden`. Employees get 404 for tickets that are not theirs, so they cannot tell whether a ticket exists, and they never see internal comments. When an employee raises a ticket, the reporter is always set to the employee, whatever the request body says.

## Security notes

- JWT (jjwt 0.12, HS256) signed with `JWT_SECRET`; the app does not start without it. Access tokens live 15 minutes, refresh tokens 7 days.
- Refresh tokens are 512-bit random values, stored as SHA-256 hashes, single-use and grouped in families for reuse detection.
- Passwords use BCrypt through `DelegatingPasswordEncoder` (`{bcrypt}` prefix).
- Failed logins are limited to 10 per IP per 60 seconds; the filter runs before authentication so blocked requests cost no BCrypt work.
- The API is stateless and uses no cookies, so CSRF protection is not needed. CORS is not configured because nginx serves frontend and API from one origin.
- `@PreAuthorize` on every controller method, plus a default-deny rule in `SecurityConfig`.
- Changes through annotated service methods are written to `audit_log` by `AuditAspect`. The audit row is written after the business transaction (see the Javadoc for why and what that means).

More in [SECURITY.md](../SECURITY.md).

## Tests

```bash
mvn test      # 70 unit tests, no Docker needed
mvn verify    # plus 42 integration tests on PostgreSQL 16 via Testcontainers
```

What the tests cover is described in [docs/testing.md](../docs/testing.md).

On macOS with colima, point Testcontainers at the colima socket first:

```bash
export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

With Docker Desktop, enable "Allow the default Docker socket to be used" in the advanced settings instead.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/itams` | JDBC URL |
| `DB_USERNAME` | `itams` | Database user |
| `DB_PASSWORD` | `itams` | Database password |
| `SERVER_PORT` | `8080` | HTTP port |
| `JWT_SECRET` | none, required | At least 32 characters |
| `JWT_ACCESS_TTL_MINUTES` | `15` | Access token lifetime |
| `JWT_REFRESH_TTL_DAYS` | `7` | Refresh token lifetime |
| `JWT_ISSUER` | `itams` | `iss` claim |
| `BOOTSTRAP_DEMO_PASSWORD` | `changeme` | Password of the demo users |
| `AUTH_RATE_LIMIT_MAX` | `10` | Failed logins per IP and window |
| `AUTH_RATE_LIMIT_WINDOW_SEC` | `60` | Rate-limit window in seconds |

In the `prod` profile the database settings have no defaults either. `docker/.env` is ignored by Git; `docker/.env.example` shows the expected variables.

## Production image

The `Dockerfile` builds with the JDK and runs on a JRE image as a non-root user, with a health check on `/actuator/health`, the `prod` profile and `-XX:MaxRAMPercentage=75` so the JVM respects container memory limits.
