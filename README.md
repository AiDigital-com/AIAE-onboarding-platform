# AI Onboarding Platform

An internal platform for onboarding and continuous learning. Admins and team
leads build **roadmaps**, **lessons**, and **materials**, then assign them to
**teams/groups**; employees work through the content and their **progress** is
tracked on dashboards. Lessons and quizzes can be **AI-generated** from source
materials, and each lesson has an AI assistant learners can ask questions.
Access is role-based (Admin / Team Lead / User) behind SSO.

## Integrations

| Service | Purpose |
| --- | --- |
| **OpenAI** (Responses API, default `gpt-4o`) | AI generation of structured lessons and quizzes, and the multi-turn lesson assistant. Source files are uploaded to OpenAI for grounding. |
| **AWS S3** | Object storage for uploaded materials and lesson assets. Uploads go direct-to-S3 via presigned `PUT`; the app never buffers files. |
| **AWS CloudFront** | CDN in front of S3 for delivering/previewing stored files via signed URLs (optional; falls back to S3 presigned `GET` when unconfigured). |
| **YouTube** (oEmbed + transcript) | Video metadata for lesson embeds and caption transcripts used as lesson-generation input. |
| **Link fetch** | SSRF-resistant fetch of user-submitted URLs for material/link previews. |
| **Clerk** | Authentication / SSO. The backend validates Clerk JWTs; access is restricted to an allowed email domain. |

Outbound integrations are isolated in `backend/external-services` and are disabled
by default locally — enable each explicitly only for a workflow that needs it.

## Stack

**Backend** — Java 21, Spring Boot 3.4 (multi-module Maven), PostgreSQL,
Liquibase, JPA/Hibernate with Ehcache L2, MapStruct, contract-first OpenAPI
(controllers implement generated interfaces), Actuator + Micrometer/Prometheus,
Logbook structured logging.

**Frontend** — React 18 + TypeScript, Vite, TanStack Query, Clerk, a typed
client generated from the OpenAPI spec via `openapi-fetch`, Tiptap editor, plain
CSS with BEM.

## Layout

- `backend/` — multi-module Maven backend (`domain`, `db`, `service`, `application`, `external-services`, `event-logging-to-db-feature`).
- `frontend/` — Vite React app; the API client is generated from `backend/application/src/main/resources/api/v1/specs/openapi.yaml`.
- `scripts/` — local and Replit build/run/dev wrappers.

## Run locally

The `local` Spring profile uses a loopback-only PostgreSQL container, real Clerk
JWT validation, and paid external integrations disabled by default.

```bash
cp .env.local.example .env.local
# set CLERK_PUBLISHABLE_KEY and VITE_CLERK_PUBLISHABLE_KEY in .env.local

docker compose --env-file .env.local -f docker-compose.yaml up -d postgres
bash scripts/local-dev-backend.sh    # backend on :5000, Liquibase applies the schema
bash scripts/local-dev-frontend.sh   # Vite on :5173
bash scripts/local-verify.sh         # build + test gates
```

The frontend runs `npm run generate:api` before typecheck/build, so calls stay
aligned with the OpenAPI contract. Enable OpenAI, storage, or YouTube explicitly
only when a workflow needs the real integration.

Stop the database without deleting data:

```bash
docker compose --env-file .env.local -f docker-compose.yaml stop postgres
```

Reset the database and volume for a clean migration run:

```bash
docker compose --env-file .env.local -f docker-compose.yaml down -v
```
