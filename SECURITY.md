# Security

ITAMS is a portfolio project, not a production service, but I tried to follow normal security practice throughout.

## Reporting a problem

If you find a security issue, or a statement in the documentation that does not match the code, please email princekumar2002.de@gmail.com rather than opening a public issue. Include what you found, how to reproduce it and what you think the impact is. I will reply within a week.

## What is implemented

**Authentication**
- Passwords are hashed with BCrypt through Spring's `DelegatingPasswordEncoder`, so the algorithm can be changed later without rehashing everyone at once.
- Access tokens are JWTs (HS256, 15 minutes). The application does not start without a `JWT_SECRET` of at least 32 bytes.
- Refresh tokens are 512-bit random values. Only their SHA-256 hash is stored, each one can be used once and is replaced on every refresh.
- All refresh tokens from one login share a family id. If an already used token is presented again, the whole family is revoked, which logs out that session for both the attacker and the real user. Other sessions of the user keep working. The revocation is committed even though the request fails (`noRollbackFor`).
- Login returns the same error for an unknown user, a wrong password and a disabled account, so usernames cannot be guessed. Failed logins are rate-limited per IP.

**Authorisation**
- Every controller method has a `@PreAuthorize` rule; `SecurityConfig` has a second, coarser list as a safety net.
- Employees can only create tickets for themselves and only see their own tickets. Other tickets return 404 instead of 403, so their existence is not revealed, and internal comments are hidden.
- Access-denied errors return 403 with the standard error body.

**Data and auditing**
- Changes made through the services are recorded in `audit_log` (who, what, when, request id).
- Constraints and row locks in PostgreSQL protect the important business rules.

**Web**
- The API is stateless and uses bearer tokens only (no cookies, so no CSRF surface).
- All errors use one JSON format and never contain stack traces.
- nginx sends a strict Content-Security-Policy (`default-src 'self'`, no inline scripts, no framing) plus `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` and `Permissions-Policy`.
- The frontend clears its data cache on login, logout and session expiry.

**Operations**
- The `prod` profile fails fast on missing configuration and hides actuator details.
- Dependabot is configured for Maven, npm, Docker images and GitHub Actions; CI fails on high or critical npm advisories in runtime dependencies.

## Known gaps

1. **Audit timing.** The audit row is written after the business transaction commits, so a failed audit insert does not roll back the change (it is logged as an error). Writing it in the same transaction would fix this.
2. **No before/after data** in `audit_log.changes` yet.
3. **Tokens in `localStorage`.** Any script on the page could read them, so an XSS bug would leak them. The CSP reduces the risk; an `HttpOnly` refresh cookie with CSRF protection would be the proper fix.
4. **Two tabs refreshing at the same moment** look like token reuse and end the session. Refreshes within one tab are already combined into one.
5. **No MFA** for admin accounts.
6. **No HTTPS** inside the Compose setup. A real deployment needs a TLS-terminating reverse proxy with HSTS.
7. **Java dependencies** are covered by Dependabot alerts, but CI does not fail on a vulnerable Maven dependency.
8. **npm advisories in development tools.** As of October 2026, `npm audit` reports issues in Vitest (UI server) and Vite (dev server on Windows), which are not part of the built app, and two moderate issues in React Router 6. Fixing them needs major upgrades (Vite 8, Vitest 5, React Router 7).
9. **Swagger UI** is not covered by the CSP and is enabled in `prod` for internal use; it should be switched off for anything internet-facing.

## Out of scope

Multi-tenancy, SSO (SAML/OIDC), encryption at rest beyond PostgreSQL defaults, and a formal threat model.
