# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

AI Onboarding Platform is a Spring Boot backend plus a React frontend.

Current top-level areas:

- `backend/` — Java 21, Spring Boot, multi-module Maven backend
- `frontend/` — React + TypeScript + Vite frontend
- `.claude/` — self-contained Claude rules, docs, skills, and task artifacts for this repository

## Start Here

1. Read `.claude/agent_docs/index.md`.
2. For backend work, read the relevant docs before changing code:
   - `project_structure.md`
   - `building_the_project.md`
   - `running_tests.md`
   - `code_conventions.md`
   - `database_schema.md`
   - `service_architecture.md`
   - `performance_engineering.md` for request flows, transactions, queries,
     external calls, logging, caching, pools, or payload work
3. For frontend work, read the relevant docs before changing code:
   - `project_structure.md`
   - `building_the_project.md`
   - `running_tests.md`
   - `frontend_architecture.md`
   - `frontend_style.md`
   - `frontend_testing.md`
   - `performance_engineering.md` for data fetching, request counts, payloads,
     rendering, or bundle work
4. Respect `.claude/rules/*.md`. These rules are repository-local and must not depend on external files.
5. Read `.claude/agent_docs/skill-selection.md` before choosing between GSD,
   `task-workflow`, and a focused skill.
6. Before applying a path, package, port, module, or test command from these
   docs, verify it against the current repository — docs can drift from code.

## Enterprise Hard Constraints

- Do not replace these repository-local rules with references to another local path or external project.
- Do not hand-edit generated backend OpenAPI sources or generated frontend OpenAPI types.
- Do not add dependencies casually. Use the existing stack and local patterns first.
- Keep secrets out of frontend code and public environment variables.
- Never replace an established product flow, navigation model, or visual system with a template default unless the user explicitly asks for that change.

## Backend Hard Constraints

- Backend stack is fixed: Java 21, Spring Boot 3.x, Maven multi-module, PostgreSQL, Liquibase.
- Backend production code stays under `com.aidigital.aionboarding.*`.
- Backend controllers implement generated OpenAPI interfaces and stay thin.
- Backend JPA repositories are accessed only through their paired entity service.
- Outbound integrations, `ExternalClientMetricsInterceptor`, and `ExternalCallTimer` live in `backend/external-services`; Logbook configuration, correlation, logging, Actuator, and Prometheus stay application-owned.
- Backend tests follow the project style from `.claude/rules/20-tests.md`.

## Frontend Hard Constraints

- Frontend stack is fixed: React, TypeScript, Vite, TanStack Query, Clerk, `openapi-fetch`, plain CSS with BEM.
- Frontend API calls go through the generated OpenAPI client boundary under `frontend/src/shared/api`.
- Preserve the product's established navigation model.
- Frontend styles follow BEM and semantic CSS tokens; do not introduce Tailwind, CSS Modules, styled-components, Emotion, or CSS-in-JS.
- Frontend tests follow the project style from `.claude/rules/50-frontend-tests.md`.
