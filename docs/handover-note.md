# Handover note — for whoever deploys this

Written at the close of the AIAE convergence migration (P15, 2026-08-14). This
migration ran **entirely locally**: nothing was pushed to a remote, nothing
was deployed, and no CI service ran a single check. Every number in
`docs/aiae-migration-log.md` was produced by a command run on this machine.
That means the deploy path itself — Replit, or wherever this actually
ships — is someone else's first contact with this converged state. This note
exists so that contact is informed, not a surprise.

## What is verified, and what is not

**Verified, repeatedly, on this machine:** the backend builds and tests green
end to end (`mvn -f backend/pom.xml clean verify`, no flags — strict
0.80 LINE / 0.70 BRANCH coverage in every module, `.template-phase =
engineering`); `bash scripts/local-verify.sh` runs every step (gates, backend
build, frontend lint/test/build, compose-file syntax) and exits with a real,
non-hardcoded status; the frontend builds and its own test suite passes.

**Never verified, by design, because this migration never deploys:**

- **`.github/workflows/ci.yml` has never executed.** It was converged to
  match the standard's shape (five jobs, `continue-on-error` removed from
  `static-checks`) so that a future remote gets a correct file on day one,
  but no GitHub Actions run, no runner, nothing has ever exercised it. The
  first real run of this file is a genuine unknown — treat it as unverified
  configuration, not a green pipeline.
- **The Replit deploy path is unverified.** P3 converged `.replit` to the
  standard's inline-workflow contract (`source scripts/replit-env.sh`,
  `npm run generate:api` before the Vite build, the `application/pom.xml`
  reference, the `onBoot` hook) and confirmed by inspection that
  `VITE_CLERK_JWT_TEMPLATE = "aidigital-api"` survived the rewrite, but
  whether the app actually **boots** from that contract on Replit has not
  been checked. Whoever deploys next should treat the first Replit boot as a
  real test, not a formality, and watch specifically for the `onBoot` hook
  and the frontend build step inside the Maven reactor.
- **The required S3 bucket CORS policy lives in AWS, outside this
  repository**, and nothing here can see whether it is actually configured.
  `docs/architecture-overview.md`'s *Runtime and deployment* section records
  the exact origins each environment's bucket needs in `AllowedOrigins`; a
  mismatch breaks the direct-to-S3 presigned upload with an error the browser
  deliberately makes unreadable. Check this explicitly before the first real
  upload in a new environment.

## The five carried gate failures — permanent, not a regression to chase

`bash scripts/verify-gates.sh` and `bash scripts/structure-lint.sh` report
exactly five failures, every time, by design:

| # | Script | Assertion | Why |
|---|---|---|---|
| 1 | `verify-gates` | Frontend must not use a left side menu/sidebar | Product decision: the UI and navigation model are carried over unchanged from before this migration. |
| 2 | `verify-gates` | `check-frontend-ui-rules.sh` (~2222 findings: raw `px`, hex colors, form a11y) | Same decision — fixing this means rewriting the visual layer, out of scope by explicit product call. |
| 3 | `verify-gates` | Logbook `DefaultSink` must be built with formatter + writer (literal-string match) | `LogbookConfig` builds the same objects through a configurable `resolveFormatter`/`resolveStrategy` path whose default is *stricter* than what the gate's literal string demands. |
| 4 | `verify-gates` | Production/Replit Logbook must use metadata-only `WithoutBodyStrategy` (literal-string match) | Same reason as #3 — real behavior is correct, the gate wants specific source text. |
| 5 | `structure-lint` | present event-logging module requires the usage-events migration at a specific path | False negative: the checker looks for `changes/0001-usage-events.xml`; this project's changeset is `1.0.0/db.version-master.xml` + `sql/usage_events.sql`. The migration exists and is applied. |

**Do not "fix" any of these by editing source to match a gate's literal
string, and do not silence them.** Each is filed as a change request to the
template maintainer (CR-1 through CR-3, plus one covering the UI-rules gate)
so the conflict is visible upstream, not just locally suppressed. If a future
gate run reports a **sixth** failure, or any of these five text strings
change, that is real signal — investigate it, don't assume it is more of the
same.

Frontend lint (`npm run lint`, 339 errors) is a related but separate,
deliberately-not-gated backlog — see the next section.

## The frontend lint backlog — 339 errors, permanent by product decision

`npm run lint` reports 339 errors. 184 of them require moving constants and
interfaces out of 145 UI component files into dedicated `model`/`constants`
modules — a real restructuring of the UI layer that this migration was
explicitly scoped **not** to do (the product's existing UI and navigation
stay as they are). `.husky/pre-commit` runs lint on every commit and
**reports but never blocks** — this is intentional, not a misconfiguration,
and `scripts/local-verify.sh` mirrors the same choice: lint runs and is
printed on every invocation, but does not affect the script's exit code.
Do not flip either of those to blocking without first clearing the backlog
(or getting an explicit, separately-approved decision to accept a red hook).

## Usage telemetry — live, and `prepare-engineering-handoff.sh` must never run

`backend/event-logging-to-db-feature` is present and active: `@LogUsage` is
an optional per-method override, not the trigger — the aspect intercepts
**every public method of all 42 `*ServiceImpl` classes** automatically, and
`app.usage-logging.enabled` defaults to `true`. This is kept by explicit
product decision (D-D in `docs/aiae-migration-plan.md`), for MVP feedback
visibility.

**`bash scripts/prepare-engineering-handoff.sh` must never be run against
this project while that module is kept.** It is a destructive script wearing
a checker's name: it deletes the module directory and the BigQuery sink
directories unconditionally when the module is present, and then hard-fails
if the module still exists — there is no dry-run and no way out once
started. Beyond the code deletion, removing an **already-applied** Liquibase
changeset entry desynchronizes `DATABASECHANGELOG` from what the running
schema actually reflects — the same hazard class as renaming the changelog
directory (see the next section). If usage telemetry is ever formally
retired, that is a deliberate, separately-scoped removal, not a byproduct of
running this script.

## Liquibase — one rule that protects a live database

`backend/migrations/src/main/resources/db/changelog/` is the classpath root
Liquibase resolves against (`spring.liquibase.change-log`), and that
classpath path — not the Maven module directory name, not the folder name on
disk — is what Liquibase records per changeset in `DATABASECHANGELOG`.
**Never rename `db/changelog/` itself.** Doing so changes the recorded
filename and Liquibase will treat all 14 (now 15, after P6's
cache-invalidation changeset) existing changesets as new and attempt to
re-run them against a schema that already has them applied. This was
confirmed empirically against production during this migration (P0/P5): the
recorded filename is the classpath form, confirming the module rename
(`backend/db` → `backend/migrations`) was always safe and this directory
rename is the actual risk, not a hypothetical one.

**Before any future phase that touches `backend/migrations` reaches a live
database, take a `DATABASECHANGELOG` backup first.** This migration never
reached that point itself — P5 was cleared by a read-only query, not a
write — so no such backup has ever been taken here, and the first person to
run new migrations against production should be the first to take one.

## What was never proven, and should not be read as proven

- **Multi-node deployment (D-E in the plan) is prepared, not exercised.**
  `backend/cache-management`'s outbox/registry mechanism and the
  `SELECT ... FOR UPDATE SKIP LOCKED` claim pattern in the two scheduled jobs
  and the teacher-video refresh path are all designed for more than one node,
  but only one node has ever run this code. Standing up a second node is the
  first real test of that design, not a formality.
- **`ApplicationCacheNamesByClassRegistry` is intentionally empty.** All 8
  cached dictionary sources are `READ_ONLY`/`@Immutable`, changed only by
  Liquibase at deploy time, so nothing ever publishes an invalidation event
  today. The registry becomes load-bearing the moment any mutable source is
  added to the Hibernate L2 cache — populate it in the same change that adds
  that source, not after.
- **The SVG upload vector is accepted, not fixed** (see
  `docs/architecture-overview.md`'s *Accepted risk* section for the full
  reasoning) and depends on `CLOUDFRONT_ENABLED`. It defaults to `false` in
  code; production is confirmed `true`. Any new environment that stands up
  without CloudFront enabled silently reactivates the vector — check this
  explicitly when provisioning a new environment, not just production.

## Where to look for more detail

- `docs/aiae-migration-plan.md` — the source of truth for every decision
  referenced above, with full reasoning.
- `docs/aiae-migration-log.md` — the phase-by-phase evidence trail (Build /
  Test / Review / Verification / Rollback) for every change this migration
  made.
- `docs/architecture-overview.md` — current, implementation-verified project
  facts (kept in `Lifecycle phase: Engineering` as of this note).
- `docs/migration-guardrails.md` — environment traps specific to this
  repository and this Windows machine; several are unrelated to the product
  and will not reproduce on a different machine (the `python3` Windows Store
  stub, the CRLF-on-Windows-native-python3 bug fixed in
  `scripts/lib/check-architecture-overview.sh`, and the Testcontainers/Docker
  Desktop named-pipe incompatibility recorded in `backend/domain/pom.xml`).
