# Frontend Dockerfile — local-dev / handoff only. Not used on Replit.
#
# Build context is the REPO ROOT (set in docker-compose.yml as `context: .`).

FROM node:22-bookworm-slim AS build
WORKDIR /workspace/frontend
ARG VITE_CLERK_PUBLISHABLE_KEY
ENV VITE_CLERK_PUBLISHABLE_KEY=${VITE_CLERK_PUBLISHABLE_KEY}
ENV VITE_CLERK_JWT_TEMPLATE=aidigital-api
COPY frontend/package*.json ./
# npm runs the `prepare` lifecycle during install, and it calls this script —
# it must exist before `npm ci` or the install exits 1. The script itself
# no-ops without a .git directory, which is the case inside the image.
COPY frontend/scripts/prepare-husky.mjs ./scripts/prepare-husky.mjs
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi
COPY frontend/ ./
COPY backend/application/src/main/resources/api/v1/specs/openapi.yaml \
     /workspace/backend/application/src/main/resources/api/v1/specs/openapi.yaml
RUN test -n "${VITE_CLERK_PUBLISHABLE_KEY}" \
  || (echo "VITE_CLERK_PUBLISHABLE_KEY build arg is required for Docker frontend build" >&2 && exit 1)
RUN npm run generate:api && npm run build

FROM nginx:1.27-alpine
ENV APP_CONTEXT_PATH=
# vite.config.ts sets build.outDir to the backend static directory (Replit
# packages the SPA into the jar), so the build output is there, not in dist/.
COPY --from=build /workspace/backend/application/src/main/resources/static /usr/share/nginx/html
COPY frontend/nginx.conf.template /etc/nginx/templates/default.conf.template
EXPOSE 80
