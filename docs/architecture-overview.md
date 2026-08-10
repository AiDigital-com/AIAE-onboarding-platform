# Architecture Overview

Project facts for this repository. `.claude/` holds the reusable AIAE
engineering contract; this file holds what is specific to **AI Onboarding
Platform** and must not be copied into the rule tree.

## Identity

- Product: **AI Onboarding Platform** — Spring Boot backend + React frontend.
- Backend production package root: **`com.aidigital.aionboarding.*`**. There is
  exactly one root; do not introduce a second.

## Backend modules

| Module | Role |
|---|---|
| `backend/domain` | JPA entities and Spring Data repositories |
| `backend/migrations` | Liquibase changelogs (renamed from `backend/db` to match the AIAE standard module name) |
| `backend/service` | business orchestration, entity services, validators |
| `backend/application` | Spring Boot runtime, security, controllers, OpenAPI implementations |
| `backend/external-services` | outbound integrations |
| `backend/event-logging-to-db-feature` | MVP usage-event logging to PostgreSQL |
| `backend/config` | shared configuration module |

Liquibase changelogs live at
`backend/migrations/src/main/resources/db/changelog/`. The Spring property is
`classpath:db/changelog/db.root-master.xml` — a classpath resource, so it is
unaffected by the module directory name. All 14 changeSets in
`1.0.0/db.version-master.xml` declare `preConditions` with `onFail="MARK_RAN"`.

## Deployment / runtime constraints

- Browser-history URLs are a public contract. `frontend/src/app/AppRoot.tsx`
  mounts `BrowserRouter`; deep links are served by
  `backend/application/.../web/SpaFallbackController.java` on the Spring side
  and `try_files ... /index.html` on nginx. Do not switch to `HashRouter`.
- BigQuery is an optional seam, not an installed integration: there is no
  BigQuery SDK dependency. `RoutingUsageEventSink` accepts an optional
  `@Qualifier("bigqueryUsageEventSink")` bean and falls back to PostgreSQL.

## Adopted standards the code has not caught up to yet

These are **not** exceptions to the rules. The AIAE contract in `.claude/` is
authoritative; the items below record where the implementation still lags, so
the gap is visible rather than mistaken for compliance.

- **Observability module.** `ExternalClientMetricsInterceptor` and
  `ExternalCallTimer` currently live in
  `backend/external-services/.../external/common/http/` alongside
  `PooledRestClientFactory`. The standard places them in a leaf
  `backend/observability` module that `application` can attach without
  depending on `external-services`. Nothing outside `external-services`
  consumes them today, so the extraction is low-risk.
- **Distributed cache.** No `ApplicationCacheNamesByClassRegistry` and no
  `CacheInvalidationEventService` exist. The rules in `12-database.md`,
  `14-performance.md`, and `agent_docs/distributed_cache.md` describe the
  target design; the AIAE template provides a `backend/cache-management`
  module for it.
- **Coverage phase tooling.** `20-tests.md` references `.template-phase`, the
  `-Pmvp` profile, `scripts/lib/check-coverage-integrity.sh`, and
  `prepare-engineering-handoff.sh`; `database_schema.md` references
  `scripts/verify-gates.sh`. None of these exist here — `scripts/` has no
  `lib/`. Jacoco itself *is* configured in `backend/pom.xml`.
- **Import ordering.** `40-frontend-rules.md` states that the ESLint rule
  `project-rules/import-section-order` fails the build. That rule is not wired
  into `frontend/eslint.config.*`, so the ordering is convention-only here.
- **CSS tokens and units.** `frontend_style.md` and `40-frontend-rules.md`
  require semantic tokens and `rem`, forbidding raw `px`. The tokens
  `--radius-card` / `--radius-control` / `--radius-pill` are not defined
  anywhere under `frontend/src`, and 59 CSS files use raw `px` against 39 using
  `rem`.

## Known upstream defect

`.claude/tasks/README.md` documents the task artifacts as `review-report.md`
and `test-report.md`, but the `task-workflow` skill writes `review.md`,
`verification.md`, and `final-review-<pass>.md`. Both files ship from AIAE, so
the inconsistency is upstream — report it there rather than patching locally.
