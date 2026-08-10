# CLAUDE.md

This file provides reusable engineering guidance to Claude Code. Apply it to
the current repository after discovering that repository's actual structure,
package root, build commands, ports, and existing product decisions.

## Project Overview

The default supported architecture is a Spring Boot backend and a React
frontend. Preserve an existing project's established stack and layout unless
the user explicitly requests a migration.

Common top-level areas:

- `backend/` — Java 21, Spring Boot 3.4, multi-module Maven backend
- `frontend/` — React + TypeScript + Vite frontend
- `.claude/` — self-contained Claude rules, docs, skills, and task artifacts for this repository

## Start Here

1. Read `.claude/agent_docs/index.md`.
2. When `docs/architecture-overview.md` exists, read it before cross-cutting
   implementation, integration, data, caching, deployment, or handoff work.
   Keep it synchronized with the implemented repository; it describes project
   facts while `.claude/` contains reusable engineering rules.
3. For backend work, read the relevant docs before changing code:
   - `project_structure.md`
   - `building_the_project.md`
   - `running_tests.md`
   - `code_conventions.md`
   - `database_schema.md`
   - `service_architecture.md`
   - `performance_engineering.md` for request flows, transactions, queries,
     external calls, logging, caching, pools, or payload work
4. For frontend work, read the relevant docs before changing code:
   - `project_structure.md`
   - `building_the_project.md`
   - `running_tests.md`
   - `frontend_architecture.md`
   - `frontend_style.md`
   - `frontend_testing.md`
   - `performance_engineering.md` for data fetching, request counts, payloads,
     rendering, or bundle work
5. Respect `.claude/rules/*.md`.
6. Read `.claude/agent_docs/skill-selection.md` before choosing between GSD,
   `task-workflow`, and a focused skill.
7. Read `.claude/agent_docs/context7.md` before using external library or
   framework documentation. Use the configured Context7 MCP for
   version-sensitive APIs; fall back to official vendor docs when unavailable.
8. Before applying a path, package, port, module, navigation, or test command
   from these docs, verify it against the current repository.

## Decision Ownership

The user owns business goals, priorities, and acceptance—not technical design.
Do not ask them to choose frameworks, architecture, persistence, API, caching,
or test mechanisms. Resolve technical ambiguity with the strongest available
reasoning role and record the rationale. Ask only for missing business behavior,
credentials/access, or an irreversible product decision, phrased in business
terms. Follow `.claude/agent_docs/agent-operating-model.md` for delegation and
the fresh final-review loop. Keep user-visible work locally demonstrable; when
the user asks to see it, invoke `local-preview` and supply safe local fixtures
when the flow otherwise has no useful data.

## Enterprise Hard Constraints

- Keep the rule set self-contained when it is installed into a project; do not
  depend on another local checkout or a machine-specific absolute path.
- Read `.claude/agent_docs/project_shape_decision.md` before deciding
  frontend-only vs full-stack work.
- Do not hand-edit generated backend OpenAPI sources or generated frontend OpenAPI types.
- Do not add dependencies casually. Use the existing stack and local patterns first.
- Keep secrets out of frontend code and public environment variables.
- Never request an MCP/API key in chat or commit one. Authentication belongs in
  the user's OAuth session, shell environment, or secret manager.
- Never replace an established product flow, navigation model, or visual system
  with a template default unless the user explicitly asks for that change.

## Backend Hard Constraints

- Backend stack is fixed: Java 21, Spring Boot 3.x, Maven multi-module, PostgreSQL, Liquibase.
- Every Liquibase `changeSet` must declare direct `preConditions`; the generated verify gate rejects changelogs without them.
- Discover and preserve the repository's single production package root; do not
  introduce a second root or placeholder packages.
- Backend controllers implement generated OpenAPI interfaces and stay thin.
- Backend JPA repositories are accessed only through their paired entity service.
- Reusable outbound metrics live in `backend/observability`; it owns
  `ExternalClientMetricsInterceptor` and `ExternalCallTimer`. Application-owned
  Logbook configuration, correlation filtering, logging, Actuator, and
  Prometheus remain in `backend/application`.
- Backend tests follow the project style from `.claude/rules/20-tests.md`.

## HTML-Only Source Projects

If the input project is standalone HTML/CSS/JS but the user needs usage
logging, analytics, action review, persistence, auth, or multi-user visibility,
do not keep it static-only. Migrate the UI into `frontend/`, add the fixed Java
backend under `backend/`, and serve the built Vite app from Spring Boot for
Replit deployment. During the MVP feedback phase, when
`backend/event-logging-to-db-feature/` is present, persist usage events to its
PostgreSQL `usage_events` table. After engineering handoff the module is
intentionally absent; do not recreate it unless the project explicitly chooses
a production analytics design. Read
`.claude/agent_docs/html_only_project_migration.md` before implementing.

## Frontend Hard Constraints

- Frontend stack is fixed: React, TypeScript, Vite, TanStack Query, Clerk, `openapi-fetch`, plain CSS with BEM.
- Frontend API calls go through the generated OpenAPI client boundary under `frontend/src/shared/api`.
- Preserve the product's established navigation model. For a new project with
  no explicit design, follow the installed frontend design guidance.
- Frontend styles follow BEM and semantic CSS tokens; do not introduce Tailwind, CSS Modules, styled-components, Emotion, or CSS-in-JS.
- Frontend tests follow the project style from `.claude/rules/50-frontend-tests.md`.

## Migration Guardrails

An AIAE convergence migration is in progress. `docs/aiae-migration-plan.md` is the single
source of truth for it; `docs/aiae-migration-log.md` records what has actually been measured.
Read the phase you are given before editing anything, and obey its **Scope** and
**Do not touch** blocks.

Each rule below is a trap that has already been hit or verified in this repository.

- **`1.0.0` is production.** Never check it out, never merge into it. All work lands on
  `migration` via one `mig/pNN-*` branch per phase. Nothing is pushed and nothing is deployed.
- **Never run `materialize-project.sh`.** It copies unconditionally; its own comment calls a
  second run "the worst shape of that failure."
- **Never normalise line endings under `.claude/`.** Those files are checksummed in
  `.claude/.aiae-fixtures-manifest`, the manifest is deliberately mixed (some entries CRLF,
  some LF), and `install-managed-claude-fixtures.py` aborts the whole install on any
  mismatch — reporting "managed fixture was edited locally", which misnames the cause. Do not
  run `git add --renormalize` across the tree and do not remove `.claude/** -text` from
  `.gitattributes`.
- **`python3` on this machine is the Windows Store stub.** It prints `Python` and exits 0.
  Ten checkers and `verify-gates.sh` invoke it internally, so a gate run without a real
  `python3` on `PATH` reports success having read nothing. Verify with
  `python3 -c "import sys; print(sys.version)"` before trusting any gate result.
- **Never rename `backend/migrations/src/main/resources/db/changelog/`.** Liquibase records
  the classpath path; renaming that directory re-runs all 14 changesets against a populated
  schema. The Maven module name is *not* part of that path and may change freely.
- **Never add `@EnableCaching`** without setting `spring.cache.jcache.config: ehcache.xml` in
  the same change. `spring.cache.type: jcache` is already declared, so the annotation alone
  creates a second, empty `javax.cache.CacheManager` beside Hibernate's.
- **Never refactor `backend/external-services/.../external/link/support/`** beyond a
  mechanical static-factory conversion. Those five classes are the SSRF guard.
- **Never touch the two presigned-upload `fetch()` call sites** in
  `frontend/src/features/library/api/useLessonMutations.ts` and `useMaterialMutations.ts`.
  They fail a gate on purpose; the failure is tracked as CR-1.
- **Never turn a gate green by renaming.** `Sidebar` → `NavRail` and the like change nothing
  real and destroy the signal for everyone after. A documented red gate is worth more than a
  fake green one. Four assertions are red by decision — see the log.
- **Do not remove `backend/event-logging-to-db-feature`,** and do not run
  `prepare-engineering-handoff.sh`: it deletes that module and then fails if it survives.
  Usage telemetry is kept by explicit decision.
- **Task artifacts are `review.md` and `verification.md`.** `.claude/tasks/README.md` claims
  `review-report.md` and `test-report.md`; that file is wrong and is an upstream defect
  (CR-7). It is a managed fixture — do not edit it to correct this.
