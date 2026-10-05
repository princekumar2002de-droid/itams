# =============================================================================
#  ITAMS frontend — production Docker image
#  Multi-stage: Vite build in a Node image, then serve dist/ with nginx.
#  The nginx server proxies /api/ /actuator/ /v3/ /swagger-ui.html to the
#  backend container, so the browser only ever talks to one origin.
# =============================================================================

# ---------- Stage 1: build ---------------------------------------------------
FROM node:22-alpine AS build

WORKDIR /workspace

# Reproducible install — package-lock.json is committed.
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

# index.html is Vite's build entry point — without it `vite build` fails.
COPY index.html tsconfig.json tsconfig.node.json vite.config.ts postcss.config.js tailwind.config.js ./
COPY src ./src

# Vite reads VITE_BACKEND_URL at build time only for the dev proxy — the
# production bundle uses same-origin /api/*, so we don't set it here.
RUN npm run build

# ---------- Stage 2: nginx ---------------------------------------------------
FROM nginx:1.31-alpine

# Small runtime footprint; nginx already runs as an unprivileged user inside.
COPY --from=build /workspace/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY security-headers.conf /etc/nginx/snippets/security-headers.conf

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=5 \
  CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1   # not localhost: resolves to ::1, nginx listens on IPv4
