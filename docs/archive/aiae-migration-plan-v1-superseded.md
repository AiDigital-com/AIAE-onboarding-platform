# AIAE Convergence — Executable Migration Plan

**Status: awaiting technical-owner approval. No phase may start before sign-off.**

| | |
|---|---|
| Project | `AIAE-onboarding-platform` |
| Branch / base commit | `1.0.0` @ `9f6e93e` |
| Standard | `C:\Users\Admin\Desktop\migrationv2\AIAE-replit-llm-aux` @ `cc64e49` |
| Skills pin (`llm-aux.lock`) | `690a9748657adf81d01702dafa2c7ecc8afcf5c5` (v0.2.0) |
| Source audit | [`docs/aiae-audit.md`](./aiae-audit.md) |
| Upstream requests | [`docs/aiae-template-change-requests.md`](./aiae-template-change-requests.md) |
| Plan date | 2026-08-09 |

This plan turns the audit's §9 outline into executable phases. It supersedes that
outline where the two differ, and every difference is justified in §2 with evidence
measured against the working tree today, not at audit time.

---

## 1. Where the repository actually is right now

The tree has moved since the audit was written. 72 paths are dirty and **none of it
is committed**. Phase-1 work landed partially, and one Phase-3 step landed early.

| Item | Audit (at `9f6e93e`) | Working tree today | Consequence |
|---|---|---|---|
| `.claude/rules`, `agent_docs`, 11 topic dirs, skills | Forked / missing | **Installed**, `.aiae-fixtures-manifest` present | C6 substantially closed |
| `CLAUDE.md` | Hand-edited fork | **Replaced with AIAE text** | Decision Ownership, Context7, HTML-only sections restored |
| `.mcp.json` | Missing | **Present** — Context7 over HTTP OAuth | No key needed, only a session auth |
| `agent-payload.skills` | Missing | **Present** | — |
| `docs/architecture-overview.md` | Missing | **Present** (content unverified) | C-check may already pass |
| `backend/db` → `backend/migrations` | Not started | **Rename staged in the index** | ⚠ see §2.1 |
| `AGENTS.md`, `replit.md`, `.agents/`, `llm-aux.lock`, `.template-phase`, `.template-version` | Missing | **Still missing** | Phase 1 outstanding |
| `.gitignore` C5 | Ignores the agent surface | **Unchanged** — still ignores `.agents/`, `AGENTS.md`, `replit.md` | Phase 1 outstanding |
| `scripts/lib/` gate harness | Missing | **Still missing** | No enforcement of anything |
| `backend/observability`, `backend/cache-management` | Missing | **Still missing** | C2, D2 outstanding |
| ESLint, Husky, `docker-compose.yml`, `DEPENDENCY-ANALYSIS.md`, `checkstyle-test-fields.xml` | Missing | **Still missing** | — |
| CI | 2 jobs | **Still 2 jobs** | Standard ships 5 |
| JaCoCo | Inverted + neutered | **Unchanged**: hardcoded `0.8` LINE, no BRANCH, 7 hand-written excludes, `-Phandoff` | C3 outstanding |

One piece of good luck worth recording: the fixtures were installed *before* the
`.gitignore` fix, which is the exact ordering C5 warns about — but the damage is
contained, because `.claude/` is not ignored and the three genuinely-ignored surfaces
(`AGENTS.md`, `replit.md`, `.agents/`) have not been created yet. Fixing `.gitignore`
before creating them, as P1 does, is still sufficient.

### Confirmed source locations for everything this plan installs

Verified by direct listing, so no phase has to go hunting:

| What | Where in the standard | Count |
|---|---|---|
| Runtime scripts | `templates/generated-project/scaffold/scripts/` | 22 |
| Gate checkers | `templates/generated-project/scaffold/scripts/lib/` | 28 |
| CI workflow (5 jobs) | `templates/generated-project/.github/workflows/ci.yml` | `static-checks`, `unit-tests`, `integration-tests`, `frontend-checks`, `local-dev-dry-run` |
| Backend module references | `templates/generated-project/scaffold/backend/` | includes `observability`, `cache-management`, `migrations` |
| `.replit`, `.gitignore`, `.env.example`, `.template-phase`, `.husky/` | `templates/generated-project/scaffold/` | `.template-phase` = `mvp` |
| Fixture installer | `scripts/install-claude-fixtures.sh` | + `scripts/lib/install-managed-claude-fixtures.py` |

---

## 2. Three corrections to the audit's phase plan

### 2.1 The staged migrations rename is the most dangerous thing in the tree — **P0 reverts it**

`backend/db → backend/migrations` is staged in the index right now with:

- **no `logicalFilePath`** anywhere under `backend/migrations/` (grep returns nothing),
- **no Lombok dependency** in `backend/migrations/pom.xml` (which structure-lint requires),
- **no harness installed** to verify either of those,
- **no rehearsal** against a production restore.

This is R1 — the one finding in the audit that can damage production data — staged
without its mitigation, ahead of the phases that would catch the omission. Starting a
migration from this state means the riskiest change is also the least verified.

**P0 unstages it and P5 re-lands it properly.** The work is not lost: it is fifteen
`git mv`s and a POM edit, and re-doing it after the harness exists costs an hour and
buys the R1 mitigation, the Lombok fix and a mechanical gate check.

### 2.2 R4 is roughly a quarter the size the audit assumed — evidence

The audit rates removing MUI/Emotion as "a UI rewrite… touches visual output on every
screen." Measured against `frontend/src` today:

| Measure | Count |
|---|---|
| Files importing `@mui/` or `@emotion/` | **40** of 403 source files |
| Largest cluster | `features/library` — **14** files (not 54) |
| `.scss` files in the repo | **0** — `sass` is an unused dependency |
| `@base-ui/react` usage | 1 file |
| `@radix-ui/*` usage | 2 files |
| `class-variance-authority` usage | 1 file |

Two consequences. `sass` removal is a one-line `package.json` deletion with zero code
impact — it does not belong in a risky phase at all. And the MUI/Emotion removal is a
bounded 40-file job, while the CSS-token migration (2103 findings) is the larger and
*separate* half. The plan splits them: dependency removal in P12, token migration in
P13. The audit's advice to combine them per surface still holds *within* P13.

The three unreviewed libraries are each used in 1–2 files. That is small enough to be a
decision made with evidence in P12 rather than an open question carried through the
migration.

### 2.3 The standard has moved since the audit — re-baseline before trusting any count

The audit measured against the template as it stood on 2026-08-09; the checkout is now
at `cc64e49` ("PDI_046: Improve llm templates"), which postdates it. Every number in
audit §1 is therefore a *reference* baseline, not necessarily today's. P0 re-runs the
suite against `cc64e49` and records the actual numbers; those become the tracking
baseline, and `.template-version` records `cc64e49` as the revision adopted.

---

## 3. Ground rules for every phase

**Reversibility.** One phase = one branch = one PR = one squashed commit on `1.0.0`, so
rollback is exactly `git revert <sha>`. Where a phase cannot be reverted by git alone
(P5 touches a live database, P15 flips CI to blocking), the phase states its
out-of-repo rollback explicitly. No phase may be merged unless its rollback has been
written down and read by the reviewer.

**Evidence contract.** Every phase produces five things, appended to
`docs/aiae-migration-log.md` in the same PR:

| Field | Requirement |
|---|---|
| **Build** | The exact command and its exit status |
| **Test** | The exact command, pass/fail counts, and coverage where the phase touches it |
| **Review** | The named skill that reviewed it, and its verdict |
| **Verification** | The named gate(s), with a before → after count |
| **Rollback** | The revert command, plus any out-of-repo action |

A phase whose gate count did not move in the predicted direction does not exit, even if
the build is green. "Green because the check does not run" is the failure mode C3
already produced once in this repository.

**Three things that must not happen.** Each was reasoned through in the audit and each
will look tempting under schedule pressure:

1. **No re-materialization.** `materialize-project.sh` copies unconditionally and its own
   comment calls a second run "the worst shape of that failure." Every project fact in
   audit §7 would be silently overwritten. Install by explicit copy, never by regenerating.
2. **No gate-gaming.** Renaming `Sidebar` to `NavRail`, or relocating the presigned-upload
   helper out of `frontend/src`, turns a gate green while changing nothing real. A
   documented red gate is worth more than a fake green one.
3. **No routing uploads through the backend.** That trades one gate failure (§6.2, CR-1)
   for a `14-performance.md` violation and a real regression on large materials.

**Carried failure.** Throughout the migration, `verify-gates.sh` fails on exactly one
assertion — the two presigned direct-to-S3 `fetch()` call sites — knowingly, with the
reasoning in audit §6.2 and CR-1. Every phase's evidence must show that count as **1**,
not 0. If it ever reads 0, someone gamed it.

**Where the technical owner is asked again.** Nowhere. All product decisions are settled
(D1, D2, §7.1). Approving this plan approves the sequence; individual phases need only
code review.

---

## 4. Phases

Fifteen phases. P0–P3 carry no runtime risk and can run back-to-back. P4–P8 are backend
topology and want their own release. P9–P11 are the backend contract. P12–P14 are the
frontend. P15 locks it in.

Skills referenced are the installed ones: `backend-rule-review`, `frontend-style-review`,
`production-code-review`, `verification-gate`, `aiae-rule-compliance-audit`,
`openapi-contract-first`, `finalize-coverage`, `ui-designer`, `local-preview`.

---

### P0 — Stabilize the tree and re-baseline · *no change to shipped code*

**Goal.** Get to a clean, honest starting line and re-measure against `cc64e49`.

**Steps.**
1. Unstage the premature rename: `git restore --staged backend/` then `git stash push -m "premature-migrations-rename" backend/`. Confirm `backend/db/` is restored and the build is green. The rename returns in P5.
2. Keep the installed `.claude/` fixtures — they are correct and P1 completes them.
3. Copy the gate suite to a scratch directory and run it against the tree unmodified. Record all counts in `docs/aiae-migration-log.md` as the **tracking baseline**, superseding audit §1. Use an explicit interpreter for the Python gates; `python3` here resolves to the Windows Store stub and silently no-ops.
4. Record the "before" product state: `mvn -f backend/pom.xml -Phandoff verify` and `npm test`, with counts.
5. Send `docs/aiae-template-change-requests.md` to the template maintainer. CR-1 is the blocker; CR-4 would resolve CR-1..CR-3 together.
6. **R5 spike** — on a throwaway branch, pin `vitest@^3.2.6`, `vite@^5.4`, `@vitejs/plugin-react@^4.3.4`; run install, build, test. The question is whether tiptap 3.22, `@base-ui/react` and `@tsparticles/confetti` survive Vite 5. Record the answer; a failure raises CR-6 and changes P12's shape.
7. **D1 specification** — write down what "reading mode" means in a top-header layout, before any code moves. Today `isLessonReadingRoute` suppresses the sidebar on `/lessons/:id` and restores it on activity routes. That is product behaviour, and deciding it mid-rebuild is how it gets lost. One paragraph in `docs/architecture-overview.md`.

**Build** `mvn -f backend/pom.xml clean install -DskipTests` · **Test** `mvn -f backend/pom.xml -Phandoff verify`, `cd frontend && npm test` · **Review** none (no product code changes) · **Verification** full gate suite run and archived; tree is clean apart from `.claude/` and `docs/` · **Rollback** `git stash pop` restores the staged rename.

> **Exit** — baseline recorded against `cc64e49`; `backend/db` intact and building; CRs sent; R5 answer known; reading-mode behaviour written down.

---

### P1 — Agent surface · *no runtime risk*

**Goal.** Make the dual-agent runtime real and committable. Ordering matters: `.gitignore` first, or the tree looks converged while the repository is not.

**Steps.**
1. `.gitignore` (C5) **first**: un-ignore `.agents/`, `AGENTS.md`, `replit.md`; add `templates/` and `custom_instruction/`; add the `.claude/tasks/*` + `!README.md` negation. Take the shape from `scaffold/.gitignore`.
2. Re-run `bash scripts/install-claude-fixtures.sh <project>` from the standard checkout to confirm the installed fixtures match `cc64e49`, and let it rewrite doc citations. It **overwrites** `CLAUDE.md` — re-apply the project-specific paragraphs afterwards, and do not re-introduce the C2 divergence (metrics belong in `observability`) while doing so.
3. Add `AGENTS.md` and `replit.md` from `scaffold/AGENTS.md.template` / `replit.md.template`.
4. Mirror `.agents/skills/` (11 skills); add `llm-aux.lock` (revision `690a9748…`), `.template-version` = `cc64e49`, `.template-phase` = `mvp`.
5. Confirm `rule-compliance-audit` is gone in favour of `aiae-rule-compliance-audit`, and `.claude/rules/README.md` stays deleted — both already true in the tree, both need to survive step 2.

**Build** n/a · **Test** n/a · **Review** `aiae-rule-compliance-audit` for surface completeness · **Verification** `check-agent-surfaces.sh` = 0; `check-installed-documentation-links.py` 6 → 0; `git ls-files` shows `AGENTS.md`, `replit.md`, `.agents/` tracked · **Rollback** `git revert`.

> **Exit** — both gates zero, and the agent surface is *tracked*, not merely present.

---

### P2 — Install the harness and CI · *measure, do not fix*

**Goal.** Every gate runs and publishes a number. Nothing blocks yet.

**Steps.**
1. Copy the 22 scripts from `scaffold/scripts/` and all 28 checkers from `scaffold/scripts/lib/` **alongside** the project's own ten. Do not replace `backend-jacoco-coverage-targets.sh`, `report-bundle-size.sh`, `local-dev-*.sh`, `replit-dev-*.sh` (audit §7).
2. Replace `.github/workflows/ci.yml` with `templates/generated-project/.github/workflows/ci.yml`, with **`static-checks` marked non-blocking** (`continue-on-error: true`) so the team sees every count without a red pipeline on day one. P15 flips it.
3. Add `backend/config/checkstyle-test-fields.xml`.
4. Leave `local-verify.sh` as the project's own for now; P15 swaps it.

**Build** unchanged · **Test** unchanged · **Review** `production-code-review` on the CI diff · **Verification** a CI run on a PR publishes counts for all 28 checkers; the numbers match P0's local baseline within noise · **Rollback** `git revert` restores the 2-job workflow.

> **Exit** — every gate runs in CI and publishes a count; the pipeline is green because `static-checks` is advisory, and the log says so.

---

### P3 — Documentation accuracy, Replit contract, Context7 · *no runtime risk*

**Goal.** Close the paper findings and the deployment contract in one low-risk phase.

**Steps.**
1. **`docs/architecture-overview.md`** — the file now exists; bring it to the template's shape and verify it against the implemented repository, not the scaffold. It must carry: the five outbound integrations, the Clerk-only auth model with `AUTH_ALLOWED_EMAIL_DOMAIN`, `PermissionEvaluator`, the L2 cache inventory, the two scheduled jobs, the presigned upload path — and **the required S3 bucket CORS policy with exact origins per environment (§7.1.1)**. That CORS block is the highest-value paragraph in this phase: the configuration lives in AWS, CI cannot see it, and it breaks uploads with an error the browser deliberately makes unreadable.
2. **`backend/DEPENDENCY-ANALYSIS.md`** — from the scaffold, filled in for this project's actual dependency set.
3. **README** — add the API / Swagger UI / OpenAPI YAML link block the gate requires. Leave the rest; the audit rates the content as excellent.
4. **`docker-compose.yaml` → `docker-compose.yml`** — `local-verify.sh` and the `local-dev-dry-run` job both look for `.yml`.
5. **`.replit` (C7)** — adopt the scaffold's inline workflow form so the literal contract strings live in `.replit`: `source scripts/replit-env.sh`, `npm run generate:api` before Vite, `backend/application/pom.xml`, and the `onBoot = "bash scripts/setup-project.sh"` hook. **Carry `VITE_CLERK_JWT_TEMPLATE = "aidigital-api"` in `[env]`** — the scaffold does not have it and losing it breaks Clerk token exchange. Keep `replit-dev-*.sh` for local use.
6. **Context7** — `.mcp.json` is already present and points at the HTTP OAuth endpoint, so there is no key to manage. Verify the server resolves in-session and record the check in `context7.md` terms: focused library questions only, never source or secrets, never a key in chat.

**Build** n/a · **Test** `docker compose -f docker-compose.yml config` parses · **Review** `production-code-review` on `.replit` and the docs diff · **Verification** `check-architecture-overview.sh` 1 → 0; `check-maven-dependency-analysis.py` 1 → 0; `verify_replit_file()` assertions pass; a Replit deploy boots and serves the SPA · **Rollback** `git revert`; redeploy.

> **Exit** — three gates zero, Replit boots from the standard workflow contract, Context7 verified reachable.

---

### P4 — `backend/observability` · *low risk, first module move*

**Goal.** Close C2 and prove the module-adding procedure on the safe change before P5's risky one.

**Steps.**
1. Create `backend/observability` with Lombok declared; add to the parent POM module list.
2. Move `ExternalCallTimer` and `ExternalClientMetricsInterceptor` from `external-services/…/external/common/http/` to `…observability.external`.
3. Add the dependency from `external-services` and `application`.
4. `PooledRestClientFactory` must keep both registration expressions **byte-identical** — the gate greps for the literal strings `new ExternalClientMetricsInterceptor(name, meterRegistry)` and `new LogbookClientHttpRequestInterceptor(logbook)`.
5. App-owned observability stays in `application`: `PerformanceMetricsFilter`, `RequestAuthenticationCacheFilter`, `IntegrationHealthIndicator`, `ByteCountingResponseWrapper`, `CountingServletOutputStream`, `MetadataOnlyHttpLogFormatter`. Only the two reusable classes move.

**Build** `mvn -f backend/pom.xml clean install` · **Test** full backend suite; assert the outbound metrics still appear on `/actuator/prometheus` after one real external call · **Review** `backend-rule-review` · **Verification** structure-lint's 4 observability assertions pass; metric names unchanged before → after · **Rollback** `git revert`.

> **Exit** — 4 structure-lint assertions cleared, metrics still emitted under the same names.

---

### P5 — `backend/migrations` rename · *data risk · own release · own day*

**Goal.** Land C1 without Liquibase re-running 14 changesets against a populated schema.

**Preconditions.** P4 merged. A restore of production available. Nothing else in flight.

**Steps.**
1. Re-do the rename: `backend/db` → `backend/migrations`, artifactId `db` → `migrations`, parent POM entry, **and add the Lombok dependency** the staged version lacked. Keep the versioned `1.0.0/` folder and the 14 `sqlFile` changesets — AIAE constrains the module name, not SQL-vs-XML.
2. **R1 mitigation — pin `logicalFilePath` on the master changelog to its current value.** Liquibase keys an applied changeset on *(id, author, filename)*; without the pin, 14 changesets look unknown and re-run. Preferred over `changelogSync` for one decisive reason: **`logicalFilePath` is revertible by `git revert` alone, `changelogSync` is not** — it rewrites `DATABASECHANGELOG` rows in every environment, and undoing that is hand-written SQL under pressure.
3. Restore `.claude/rules/12-database.md` to the AIAE text — the local edit scoped it to `backend/db/**`, making the divergence self-ratifying.
4. **Rehearse against a production restore before merging.** Start the app against the restore and assert `DATABASECHANGELOG` has exactly 14 rows afterwards, unchanged. 28 rows means the pin did not take.
5. Watch `jsonb_array_contains_ci` specifically: it uses an `sqlCheck` precondition rather than table-existence, so it is the one least protected by the `MARK_RAN` safety net.

**Build** `mvn -f backend/pom.xml clean install` · **Test** full suite plus Testcontainers integration tests on an empty schema (proves a green-field apply still works) · **Review** `backend-rule-review` + a second reviewer on the changelog diff specifically · **Verification** `check-liquibase-preconditions` stays 0 (all 14 already comply — preserve this); `DATABASECHANGELOG` row count identical before → after on the restore; structure-lint's 3 `migrations` assertions pass · **Rollback** `git revert` and redeploy. If `changelogSync` was used instead of the pin, rollback also requires restoring the `DATABASECHANGELOG` rows from the pre-release backup — take that backup regardless.

> **Exit** — Liquibase applies cleanly to a restored production database with no new rows, and to an empty schema from scratch.

---

### P6 — `cache-management` and the second cache · *D2, M1, M3*

**Goal.** Give the live L2 cache a cross-node invalidation path, and stop having two caches for the same data.

**Preconditions.** `.claude/agent_docs/distributed_cache.md` installed (done in P1 — it is the protocol being adopted, so P6 cannot precede it).

**Steps.**
1. Install `backend/cache-management` with POM wiring (`service` depends on it, Lombok declared) and the `0003-cache-invalidation` changelog included from the master changelog.
2. Add `JpaCacheInvalidationEventService` and an **initially empty** `ApplicationCacheNamesByClassRegistry`. Empty is deliberate: all 8 cached sources are `READ_ONLY` dictionary entities changed only by Liquibase at deploy time, so registering them would add publish calls no transaction ever makes. The registry becomes load-bearing the moment a mutable source is cached.
3. Move `CacheConfig` to the shared-manager form so Spring and Hibernate hold **one** `javax.cache.CacheManager`.
4. Keep the 8 entity regions, 8 query regions and every `ehcache.xml` heap bound unchanged.
5. Warm-up stays **disabled**. Never with an unbounded `parallelStream()` — structure-lint rejects that shape by name.
6. **M3** — fold `DictionaryLookupService`'s bean-level `ConcurrentHashMap` into the managed cache. `distributed_cache.md` names this exact shape as forbidden. Leaving a second unmanaged cache beside the one being made invalidation-aware defeats the phase.

**Build** `mvn -f backend/pom.xml clean install` · **Test** the standard's cache verification list in full: registry verification resolves every configured region; a mutation commit creates exactly one event and a rollback none; publication outside a transaction fails; polling processes increasing IDs and retries a failed eviction; Spring and Hibernate receive the same manager instance. Plus an M3 test asserting no second cache instance is reachable from `DictionaryLookupService` · **Review** `backend-rule-review` · **Verification** structure-lint's cache-pairing assertion passes; `@EnableScheduling` present; cache hit ratios on `/actuator/prometheus` unchanged before → after · **Rollback** `git revert`; the `0003` changelog needs its rollback exercised in the rehearsal.

> **Exit** — the full cache verification list passes and only one cache manager exists in the context.

---

### P7 — Coordinate node-affine work · *M2, M4*

**Goal.** Make scheduled and on-read work safe on N nodes. `MaterialYoutubeBackfillJob` is the priority — it burns a hard daily YouTube quota once per node every five minutes.

**Steps.**
1. `MaterialYoutubeBackfillJob` — claim batches with `SELECT … FOR UPDATE SKIP LOCKED`. Preferred over an advisory lock because both jobs are already bounded batch sweeps, and claiming parallelises (two nodes take disjoint batches) where a lock wastes the other nodes' tick.
2. `AbandonedUploadCleanupJob` — same treatment. Duplicated S3 deletes are idempotent but wasteful.
3. `TeacherVideoRefreshService` — different shape: read-triggered, so two nodes serving the same lesson both poll HeyGen. `Lesson.@Version` protects correctness but the loser sees a 409 for something they did not do. Add a conditional claim on the lesson's refresh timestamp so only the winner polls.

**Build** standard · **Test** the §8 verification set: a job invoked concurrently from two threads processes disjoint batches; two concurrent teacher-video refreshes produce exactly one HeyGen call · **Review** `backend-rule-review` · **Verification** the two concurrency tests pass; no transaction is held open across the HeyGen or YouTube call (`14-performance.md`) · **Rollback** `git revert`.

> **Exit** — both concurrency tests green; YouTube call volume is independent of node count.

---

### P8 — Boundary splits · *C8, mechanical*

**Goal.** Three small extractions the production scanners flag, batched because each is a few minutes.

**Steps.**
1. Split `GlobalExceptionHandler` into the handler plus `error/mapper/GlobalExceptionResponseHelper` and its `Impl`, so the handler holds no private helpers. The scaffold files are drop-in references.
2. Split `service/common/time/CurrentTime` into an interface plus `CurrentTimeImpl` that owns the `now()` calls.
3. Move the `MultipartFile` import out of `UploadValidator` — the only place a service imports a web API.
4. **§7.1.2** — while the upload path is open, add server-side content verification to `confirmUpload`: read the object's leading bytes and reject on mismatch with the declared type. `confirmUpload` already fetches object metadata, so this composes with the existing per-purpose allowlist rather than replacing it. Today a renamed `evil.html` passes as `application/pdf` and is shown to other staff.

**Build** standard · **Test** existing suite plus a negative test per split and a content-mismatch rejection test · **Review** `backend-rule-review` · **Verification** `check-production-current-time.sh` 2 → 0; the handler contains no private methods; the mismatch test rejects · **Rollback** `git revert`.

> **Exit** — the current-time gate is zero and uploads reject content that does not match its declared type.

---

### P9 — Coverage gate · *CI risk · one commit*

**Goal.** Make `mvn verify` mean something. Today `jacoco.line.coverage` and `jacoco.branch.coverage` are dead configuration, `jacoco-check` hardcodes `0.8` LINE, BRANCH is not gated at all, and seven hand-written package globs are invisible.

**Steps.**
1. **Measure first**, with the excludes removed and the check skipped. Nobody knows the real number, because `**/models/**`, `**/entities/**`, `**/repositories/**` and `**/config/**` have never been counted. Record it.
2. Move `0.80` / `0.70` into `<properties>`; wire both limits to those properties; **add the missing BRANCH limit**.
3. Delete the seven hand-written excludes, keeping only the `**/api/v1/**` generated ones. Delete them from *both* the report and check blocks — the duplication is why the gate reports 15 problems for 7 excludes.
4. Replace the `handoff` profile with `mvp` (0.30/0.25).
5. **Same commit**: commit `.template-phase` = `mvp` and drop `-Phandoff` from CI, so the relaxed floor applies the moment the strict defaults land (R2). Splitting these turns CI red between merges.

**Build** `mvn -f backend/pom.xml clean verify` with no flags · **Test** full suite; record real line and branch coverage against the new denominator · **Review** `production-code-review` on the POM diff · **Verification** `check-coverage-integrity.sh` 15 → 0; a bare `mvn verify` is strict and green under `-Pmvp` · **Rollback** `git revert` restores `handoff`.

> **Exit** — coverage integrity zero, the real coverage number is known and written down, and CI is green on the `mvp` floor.

---

### P10 — API validation · *tests before constraints* · R3

**Goal.** Close the largest hole in the suite, then make the contract honest — in that order.

**Steps.**
1. **First**, write negative MVC tests. 91 constrained operations currently have **zero** `isBadRequest()` assertions. Without them, nothing catches a constraint that is too tight — including one that rejects requests the existing frontend sends today.
2. **Then** add the 82 OpenAPI input constraints. Derive every bound from the actual column width in `1.0.0/sql/*.sql` rather than guessing. Where a field genuinely has no bound, use `x-unconstrained-reason` — the gate accepts it and it is the honest answer.
3. Add the 4 missing schema descriptions.
4. Regenerate both sides through `openapi-contract-first`; never hand-edit generated sources.
5. Bring test style to `20-tests.md` along the way — private fields, package-private classes, targeted Instancio `.set(field(...))` — now enforced by the P2 Checkstyle config.

**Build** standard, including OpenAPI regeneration · **Test** `mvn verify`; `cd frontend && npm run check:api` · **Review** `backend-rule-review` + `openapi-contract-first` · **Verification** `check-api-validation-tests.py` 91 → 0; `check-openapi-input-constraints.py` 82 → 0; `check-openapi-documentation.sh` 4 → 0; `check-api-client-paths.sh` stays 0 · **Rollback** `git revert` — but note a constraint that reached production and started rejecting real traffic needs a forward fix, so stage this behind the negative tests as written.

> **Exit** — three gates zero, and every existing frontend request still succeeds.

---

### P11 — Thin controllers and service shape · *regression risk · one aggregate per PR*

**Goal.** 55 controller violations across 12 controllers, 25 service-shape violations, 10 mappers, 8 static factories.

**Steps.**
1. **Controllers first** — mechanical. Push branching, ternaries and stream transforms down into services or API mappers.
2. **Then services** — split the 5 oversized and 9 over-injected `ServiceImpl`s into collaborators (validator / policy / assembler). `LearningServiceImpl` (357 lines, 16 public methods, 13 fields) and `LessonServiceImpl` (332 lines, 11 fields) are the two that are easy to get subtly wrong. **One aggregate per PR**, and only now that P10's negative tests exist as the safety net.
3. Stop the 10 application mappers hand-constructing `new *V1(...)`.
4. Convert the 8 static factories (3 enum `fromValue`, 5 in `external/link/model`). **Do not refactor the SSRF link guard casually** — its five `link/support` classes are security-critical (audit §7).
5. Resolve the Logbook sink form (C9): either match the gate's literal expressions, or keep the configurable `resolveFormatter()` / `resolveStrategy()` design — which is genuinely *stricter* than the scaffold — and file it as a template exception alongside CR-1.

**Build** standard · **Test** full suite per PR; no coverage regression · **Review** `backend-rule-review` per PR, `production-code-review` on the service splits · **Verification** `check-thin-controllers.py` 55 → 0; `check-service-contract-quality.py` 25 → 0; `check-production-static-methods` 8 → 0; `check-production-manual-mapping.sh` stays 0; structure-lint's 10 mapper findings → 0 · **Rollback** per-PR `git revert`.

> **Exit** — `structure-lint.sh` reports zero and every backend `lib/` checker is zero.

---

### P12 — Frontend tooling and dependency cleanup · *low risk, guards the rest*

**Goal.** Put the guardrails in before touching UI, and clear the dependencies that need no UI work.

**Steps.**
1. ESLint flat config, the `eslint-rules/import-section-order.mjs` rule, a `lint` npm script, `scripts/prepare-husky.mjs`, `.husky/pre-commit`. There is no linting in this project at all today.
2. **Delete `sass`** — zero `.scss` files exist. A one-line removal, no code impact.
3. Align `vitest` → `^3.2.6`, `vite` → `^5.4`, `@vitejs/plugin-react` → `^4.3.4`, per P0's spike result. The downgrade is mandatory, not conditional, since the template is read-only for this team. If the spike failed, this step stops and CR-6 escalates rather than forking the pin locally — a local fork re-creates exactly the drift this migration exists to remove.
4. Decide the three unreviewed libraries with the usage evidence in §2.2: `@base-ui/react` (1 file), `@radix-ui/*` (2), `class-variance-authority` (1). Each is small enough to remove during P13's pass over that file, or to keep with a recorded reason. Do not carry them as an open question.

**Build** `cd frontend && npm run build` · **Test** `npm test`, `npm run typecheck`, `npm run lint` · **Review** `frontend-style-review` · **Verification** `npm run lint` green and firing on pre-commit; `verify-gates.sh` vitest-pin assertion passes; bundle size recorded via `report-bundle-size.sh` before → after · **Rollback** `git revert`; `npm ci` restores the lockfile.

> **Exit** — lint is green and hooked, the pinned toolchain builds and tests, `sass` is gone.

---

### P13 — MUI, Emotion and the CSS token migration · *visual risk · surface by surface*

**Goal.** One styling system. 40 files import MUI/Emotion; 2222 UI findings exist of which 2103 are product code.

**Steps.**
1. Work **one feature per PR**, in ascending size so the pattern is established on small surfaces first: `features/home`, `profile`, `permissions`, `roadmaps`, `groups`, `team-progress`, `activity-player`, `lessons`, `shared/ui`, `lesson-reader`, `library` (14 MUI files, the largest).
2. Per surface, combine the MUI/Emotion removal and the CSS token migration in the **same** PR — they touch the same files, and two full sweeps costs twice.
3. Before/after screenshots on every PR. Use `ui-designer` in **parity mode** — this is a re-implementation, not a redesign, and the visual system must not drift while the dependency does.
4. **Relocate vendored tiptap** from `frontend/src/shared/editor` (153 files) to `frontend/vendor/editor`. The UI gate scans `frontend/src` only, so this removes its 119 findings honestly — vendored dependency code is not project source — rather than by restyling upstream code. Confirmation requested upstream as CR-3.
5. **Leave the two presigned-upload `fetch()` call sites intact** in `useLessonMutations.ts` and `useMaterialMutations.ts`.

**Build** `npm run build` per PR · **Test** `npm test`, `npm run typecheck`; `local-preview` for a visual pass on the changed surface · **Review** `frontend-style-review` + `ui-designer` parity check per PR · **Verification** `check-frontend-ui-rules.sh` decreasing monotonically to 0; `@mui/*`, `@emotion/*` absent from `package.json`; screenshots attached · **Rollback** per-PR `git revert`.

> **Exit** — UI-rules gate zero, no CSS-in-JS dependency remains, every surface has a parity screenshot.

---

### P14 — Navigation rebuild · *D1 · product behaviour*

**Goal.** Replace the left sidebar with the scaffold's top-header `AppShell`, per the sign-off recorded 2026-08-09.

**Why this is compliant, which is subtle.** The rule reads *"Never replace an established
product flow, navigation model, or visual system with a template default **unless the
user explicitly asks for that change**."* The sign-off is not paperwork around the rule —
it is the clause that permits the rebuild. **Cite it in the commit message**, so the
compliance argument survives with the code.

**Steps.**
1. Replace `app/AppShell.tsx`, `shared/ui/Sidebar.tsx`, `shared/ui/sidebar.css`; re-lay-out the 12 pages. `shared/ui/AppHeader.tsx` already exists and is the natural base.
2. Implement the **reading-mode behaviour specified in P0**. This is the one item that is not incidental styling.
3. **Routing is preserved exactly.** All 16 `<Route>` declarations in `app/AppRoot.tsx` keep their paths, params, guards and lazy boundaries. This phase changes chrome, not the route table — a changed URL is a broken bookmark and an unrequested product change.
4. Do not rename `Sidebar` to `NavRail`. Someone will suggest it; it turns the gate green while changing nothing about the UI, and a green gate obtained that way destroys the signal for everyone afterwards.

**Build** `npm run build` · **Test** `npm test`; routing tests asserting all 16 paths resolve to the same components as before; `local-preview` walkthrough of every page including `/lessons/:id` reading mode · **Review** `frontend-style-review` + `ui-designer`; product sign-off re-confirmed on the built UI · **Verification** `verify-gates.sh` sidebar assertion passes; route table diffed before → after and identical; screenshots of all 12 pages · **Rollback** `git revert` — self-contained, since routing is untouched.

> **Exit** — `verify-gates.sh` fails on **exactly one** assertion, the presigned upload (§6.2), and that failure is visible and attributed. Every route resolves as before.

---

### P15 — Make it stick · *lock-in*

**Goal.** Prevent re-divergence.

**Steps.**
1. Flip CI `static-checks` to blocking (remove `continue-on-error`), with the CR-1 presigned-upload assertion explicitly allow-listed and annotated — not silenced.
2. Replace the project's `local-verify.sh` with the AIAE one, so gates, `mvn clean verify`, the frontend suite and the compose syntax check all run before every push.
3. Run the `finalize-coverage` skill; raise `.template-phase` to `engineering` once the real number clears 0.80 / 0.70. That unlocks `prepare-engineering-handoff.sh`.
4. Document the `sync-llm-aux.sh --update-lock` upgrade path in `AGENTS.md`, so the next template revision arrives as a reviewed diff rather than as drift.

**Build** `mvn -f backend/pom.xml clean verify` (no flags, strict) · **Test** `bash scripts/local-verify.sh` end to end with no skipped step · **Review** `aiae-rule-compliance-audit` over the whole repository as the closing check · **Verification** every gate zero except the one carried CR-1 assertion; `.template-phase` = `engineering`; `llm-aux.lock` and `.template-version` current · **Rollback** revert `static-checks` to advisory; `.template-phase` back to `mvp`.

> **Exit** — `local-verify.sh` passes end to end, and a fresh `aiae-rule-compliance-audit` reproduces it.

---

## 5. Coverage of the requested scope

| Requested area | Phase(s) | Note |
|---|---|---|
| Architecture | P4, P6, P8, P11 | Module topology, cache protocol, boundary splits, service shape |
| Module boundaries | P4, P5, P6 | `observability`, `migrations`, `cache-management` |
| API validation | P10 | Negative tests **before** constraints (R3) |
| Thin controllers | P11 | 55 violations across 12 controllers |
| Database migrations | P5 | R1 mitigated by `logicalFilePath`, rehearsed on a restore |
| Cache decisions | P6 | D2 adopted; registry empty by design; M3 folded in |
| Authentication | P3 preserve+document, P0 baseline | Clerk-only, `PermissionEvaluator`, `AUTH_ALLOWED_EMAIL_DOMAIN`, `VITE_CLERK_JWT_TEMPLATE`, CSP `frame-src` — all preserved, none rebuilt |
| Frontend routing | P14 | All 16 routes preserved verbatim; chrome only |
| Tests | P9, P10, P7, P6 | Coverage gate, 91 negative tests, concurrency tests, cache verification list |
| Dependency cleanup | P12, P13 | `sass` (dead), MUI/Emotion (40 files), toolchain pins, 3 libraries decided |
| Documentation accuracy | P1, P3 | Doc-link gate, `architecture-overview.md` incl. the S3 CORS policy, README, `DEPENDENCY-ANALYSIS.md` |
| CI | P2 advisory → P15 blocking | 2 jobs → 5 |
| Context7 | P1, P3 | `.mcp.json` present; verify reachability, no key in chat |
| Replit deployment | P3 | Workflow contract strings, `onBoot`, `VITE_CLERK_JWT_TEMPLATE` carried |
| Multi-node (stated objective) | P6, P7 | M1–M4 |

**Not in scope, deliberately:** routing uploads through the backend (CR-1, §6.2);
raising the vitest pin locally (CR-6); re-materializing the project (audit §7); and any
change to the SSRF link guard beyond the mechanical static-factory conversion.

---

## 6. Sequencing and dependencies

```
P0 ──▶ P1 ──▶ P2 ──▶ P3 ──┬──▶ P4 ──▶ P5 ──▶ P6 ──▶ P7 ──▶ P8 ──▶ P9 ──▶ P10 ──▶ P11 ──┐
                          │                                                              ├──▶ P15
                          └──────────────────────▶ P12 ──▶ P13 ──▶ P14 ────────────────┘
```

- **P0 → P1 → P2 → P3** are strictly ordered and carry no runtime risk.
- **P6 requires P1** (`distributed_cache.md` is the protocol being adopted).
- **P10 must precede P11** (negative tests are the refactor's safety net).
- **P5 is a release of its own** — nothing else in flight, rehearsed on a production restore.
- **The backend chain (P4–P11) and the frontend chain (P12–P14) are independent** and can run in parallel with two people.
- **P15 requires both chains complete.**

---

## 7. What the technical owner is approving

1. **§2.1** — unstaging the in-flight `backend/migrations` rename and re-landing it in P5 with the R1 mitigation.
2. **§2.2** — splitting dependency removal (P12) from the CSS token migration (P13), on the measured 40-file footprint.
3. **§2.3** — re-baselining against template `cc64e49` and adopting that revision.
4. **The phase order in §6**, including P5 as a standalone release and P10 before P11.
5. **The evidence contract in §3**, including that a phase does not exit on a green build alone.
6. **Carrying exactly one failing gate** (CR-1, presigned upload) visibly for the duration.
7. **`logicalFilePath` over `changelogSync`** for R1, chosen for revertibility.

Nothing else is open. All product decisions (D1, D2, §7.1) are settled in the audit.

**On approval**, P0 starts and `docs/aiae-migration-log.md` is created with the baseline.
