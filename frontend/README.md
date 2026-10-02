# ITAMS frontend

React 18, TypeScript 5, Vite 5, Tailwind CSS 3, React Router 6, TanStack Query 5.

## Getting started

### With Docker

```bash
cd ../docker
cp .env.example .env        # set JWT_SECRET
docker compose up --build -d
```

The app runs on http://localhost:3000. nginx serves the built frontend and forwards `/api` to the backend, so the browser only talks to one origin.

### Local development

Needs Node 20 or newer and a backend on http://localhost:8080. For another backend URL, copy `.env.example` to `.env.local` and set `VITE_BACKEND_URL`.

```bash
npm install
npm run dev
```

Open http://localhost:5173. The Vite dev server forwards `/api`, `/actuator` and `/v3/api-docs` to the backend, so no CORS configuration is needed.

Demo users: `admin`, `itmanager` and `employee`, password `changeme` (local development only).

## Commands

| Command | |
|---|---|
| `npm run dev` | dev server with hot reload |
| `npm test` | Vitest test run |
| `npm run typecheck` | TypeScript check only |
| `npm run build` | type check and production build into `dist/` |
| `npm run preview` | serve the production build locally |

## Pages

| Route | Visible to | Main actions |
|---|---|---|
| `/login` | everyone | log in |
| `/dashboard` | all roles | KPIs and breakdowns |
| `/assets`, `/assets/:id` | all roles | register, edit, assign, return, record maintenance, finish maintenance, retire (ADMIN, IT_MANAGER) |
| `/assignments` | all roles | assignment history |
| `/licenses`, `/licenses/:id` | all roles | create licence, assign and release seats (ADMIN, IT_MANAGER) |
| `/maintenance` | all roles | record and delete maintenance (ADMIN, IT_MANAGER) |
| `/tickets`, `/tickets/:id` | all roles | raise and comment; status, priority, assignment and internal notes for ADMIN and IT_MANAGER |
| `/employees` | ADMIN, IT_MANAGER | onboard, edit, offboard (ADMIN) |
| `/departments` | ADMIN, IT_MANAGER | create, edit, delete (ADMIN) |
| `/profile` | all roles | own account and roles |

Hiding buttons and menu entries is only for usability. The backend checks every permission again.

## Structure

```
src/
├── main.tsx              entry point: query client, router, auth provider
├── App.tsx               routes and route guards
├── lib/                  API client, query client, formatters
├── auth/                 AuthProvider, ProtectedRoute, usePermissions
├── api/                  typed functions per backend resource, DTO types
├── components/
│   ├── layout/           shell, sidebar, top bar
│   ├── ui/               small Tailwind components (button, input, modal, badges, ...)
│   ├── form/             FormField, FormDialog, ConfirmDialog
│   └── dialogs/          one dialog per action (assign asset, create licence, ...)
├── pages/                one component per route
└── test/                 test setup and helpers
```

## Design decisions

- **Plain `fetch` instead of axios.** `src/lib/api.ts` adds the bearer token, refreshes once on a 401 and retries, and turns error responses into a typed `ApiError`. Parallel requests that fail at the same time share one refresh call, which matters because the backend treats a reused refresh token as a theft signal.
- **TanStack Query for server data.** Caching, loading and error states and invalidation after a change, without hand-written effects. The cache is cleared on login and logout so data never carries over between users.
- **Tokens in `localStorage`.** Simple, but readable by scripts on the page. The strict Content-Security-Policy from nginx reduces that risk; an `HttpOnly` cookie would be the better long-term solution.
- **Own small components instead of a UI library.** A dozen Tailwind components are enough and keep the bundle small. Dashboard bars are plain divs, so there is no chart library either.
- **Shared form building blocks.** Each dialog only contains its fields and its API call; the layout, error display and buttons come from `FormDialog`, and `FormField` links every label to its input.

## Production build

The `Dockerfile` builds with Node 20 and serves `dist/` with `nginx:1.27-alpine`. `nginx.conf` handles client-side routing, proxies the API, caches hashed assets for 30 days and includes `security-headers.conf` (CSP, `nosniff`, frame and referrer policies).
