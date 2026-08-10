# AIAE Convergence Audit — Onboarding Platform

**Read-only audit. No project files were changed by the audit itself.**

| | |
|---|---|
| Project | `AIAE-onboarding-platform` |
| Branch / commit | `1.0.0` @ `9f6e93e` (2 commits) |
| Standard | `C:\Users\Admin\Desktop\migrationv2\AIAE-replit-llm-aux` |
| Pinned template revision | `690a9748657adf81d01702dafa2c7ecc8afcf5c5` (`llm-aux.lock` v0.2.0) |
| Audit date | 2026-08-09 |

Every finding below was produced by running the AIAE template's own gate scripts,
unmodified except for making them non-fatal, against the working tree. Counts are
measured, not estimated. See [Method](#method) at the end.

---

## Verdict

**This is a re-convergence, not a first adoption.**

The project was materialized from an earlier revision of this same template and then
diverged. The evidence is unambiguous: six production files still carry source comments
citing `templates/generated-project/**` paths that only exist inside the control plane,
and the backend already ships the scaffold's `SpaFallbackController`,
`AuthStartupValidator`, `ClerkPublishableKeyDecoder`, `CorrelationIdFilter`, the
usage-logging module, and the `features/_template` frontend module verbatim.

That is good news for the backend: the stack, package root, module concept, auth model
and OpenAPI contract-first shape all match. The divergence is concentrated in four places:

1. **Two backend modules named and placed differently** (`db` vs `migrations`; metrics in
   `external-services` vs `observability`).
2. **A coverage gate that was inverted and then neutered.**
3. **A frontend that grew a second styling system** (MUI + Emotion + sass).
4. **An agent surface that the project's own `.gitignore` actively suppresses.**

**Multi-node operation is a stated objective**, so §8 sweeps the whole codebase for
node-affinity hazards independently of the AIAE gates. The result is encouraging: stateless
sessions, `@Version` optimistic locking across every aggregate, atomic upload confirmation
and DB-persisted generation status are all already in place. Four gaps remain (M1–M4), the
sharpest being a scheduled YouTube backfill that would burn quota once per node every five
minutes.

**All decisions are made; no open questions remain.** `AIAE-replit-llm-aux` is read-only
for this team, which shaped several of them (§6.1):

- **D1** — rebuild the left sidebar as the top-header shell, on recorded product sign-off.
- **D2** — adopt `backend/cache-management` with the persisted-invalidation protocol.
- **Uploads** — keep direct-to-S3; add server-side content-type verification; accept the
  transfer-size billing exposure; document the bucket CORS policy (§7.1).
- **Vendored tiptap** — relocate out of `frontend/src` rather than restyling upstream code.

**One failing gate is carried deliberately.** The presigned-upload assertion cannot pass
without violating `14-performance.md`; it is raised with the template maintainer as CR-1 and
documented in §6.2. Six upstream requests are in
`docs/aiae-template-change-requests.md`.

---

## 1. Measured gate baseline

AIAE scaffold gates run against the current tree. `structure-lint.sh` and
`verify-gates.sh` normally abort on the first failure; both were patched locally (in a
scratch copy) to report all findings.

| Gate | Result | Findings | What it means here |
|---|---|---:|---|
| `structure-lint.sh` | **FAIL** | 24 | Missing `migrations` + `observability` modules, misplaced metrics classes, 10 mappers hand-constructing DTOs |
| `verify-gates.sh` | **FAIL** | 12 | README, `.replit` workflow contract, frontend lint/vitest pins, sidebar, raw `fetch`, Logbook sink form |
| `check-frontend-ui-rules.sh` | **FAIL** | 2222 | 1482 raw `px`, 606 hex literals, 134 other. 2103 in product code, 119 in vendored `shared/editor` |
| `check-api-validation-tests.py` | **FAIL** | 91 | 91 constrained operations, **zero** `isBadRequest()` MVC assertions in the suite |
| `check-openapi-input-constraints.py` | **FAIL** | 82 | Parameters / request properties with no constraint and no `x-unconstrained-reason` |
| `check-thin-controllers.py` | **FAIL** | 55 | Branching, ternaries and stream transforms inside 12 API controllers |
| `check-service-contract-quality.py` | **FAIL** | 25 | 5 oversized `ServiceImpl`s, 9 over-injected, 4 with too many public methods, 2 undocumented |
| `check-coverage-integrity.sh` | **FAIL** | 15 | Missing `.template-phase`; 7 hand-written packages excluded from JaCoCo, counted twice (report + check) |
| `check-production-static-methods` | FAIL | 8 | 3 enum `fromValue`, 5 static factories in `external/link/model` |
| `check-installed-documentation-links.py` | FAIL | 6 | Source comments citing removed control-plane paths — the provenance fingerprint |
| `check-openapi-documentation.sh` | FAIL | 4 | 4 schema fields without descriptions |
| `check-production-current-time.sh` | FAIL | 2 | `CurrentTime` holds the `now()` calls itself instead of delegating to an `Impl` |
| `check-architecture-overview.sh` | FAIL | 1 | `docs/architecture-overview.md` absent |
| `check-maven-dependency-analysis.py` | FAIL | 1 | `backend/DEPENDENCY-ANALYSIS.md` absent |
| `check-liquibase-preconditions` | PASS | 0 | All 14 changesets already declare direct `preConditions` — preserve this |
| `check-openapi-strict-schemas.sh` | PASS | 0 | No loose DTO schemas |
| `check-openapi-enums.sh` | PASS | 0 | — |
| `check-api-client-paths.sh` | PASS | 0 | Frontend paths match the spec |
| `check-production-magic-values.sh` | PASS | 0 | — |
| `check-production-manual-mapping.sh` | PASS | 0 | MapStruct used throughout the service layer |

### Frontend UI findings by area

| Findings | Area |
|---:|---|
| 462 | `features/lesson-reader` |
| 462 | `features/library` |
| 305 | `shared/ui` |
| 269 | `features/activity-player` |
| 146 | `features/lessons` |
| 136 | `features/team-progress` |
| 119 | `shared/editor` *(vendored tiptap)* |
| 74 | `features/roadmaps` |
| 53 | `features/groups` |
| 51 | `shared/context` |
| 29 | `features/admin`, `features/home`, `pages/login.css` |
| 26 | `features/profile` |
| 19 | `pages/not-found.css` |
| 12 | `features/permissions` |
| 1 | `App.css` |
| **2222** | **total** |

---

## 2. Collisions

Places where the project and the standard both define the same thing, incompatibly. Each
requires a change on the project side; two also require correcting the project's own
locally-edited rule text, which currently codifies the divergence as if it were policy.

### C1 — Migration module: `backend/db` vs `backend/migrations` · **Blocking**

| | |
|---|---|
| **Current** | `backend/db`, artifactId `db`, changelog root `db.root-master.xml` → `1.0.0/db.version-master.xml` → 14 `sqlFile` changesets in `1.0.0/sql/*.sql`. No Lombok in the POM. |
| **AIAE** | `backend/migrations`, listed in the parent POM's required-module loop, changelog root `db/changelog/db.changelog-master.xml` with XML changesets under `changes/`. Lombok mandatory in every submodule. |

`structure-lint.sh` hard-requires the literal module name `migrations` in three separate
assertions and fails a fourth on the missing Lombok dependency. The project's own
`.claude/rules/12-database.md` was edited to scope itself to `backend/db/**`, so the
divergence is currently self-ratifying.

**Resolution.** Rename the module and artifact; keep the versioned `1.0.0/` folder and the
`sqlFile` style — AIAE constrains the module name and the `preConditions` requirement, not
the SQL-vs-XML choice. Restore `12-database.md` to the AIAE text. See **R1** for the
database-state hazard this creates.

### C2 — Outbound metrics live in the wrong module · **Blocking**

| | |
|---|---|
| **Current** | `ExternalCallTimer` and `ExternalClientMetricsInterceptor` in `backend/external-services/…/external/common/http/`. No `observability` module. |
| **AIAE** | A reusable `backend/observability` module owns both classes and depends on no product module. `application/pom.xml` must declare it. |

structure-lint asserts this four ways, including an explicit rule that the two classes
*belong only in* `backend/observability`. Both `00-backend-hard-rules.md` and
`10-architecture.md` in the project were edited to say the opposite — "Both live in
`backend/external-services`" — so the rule files must be reverted alongside the code move.

**Resolution.** Create `backend/observability`, move both classes to
`…observability.external`, add the dependency from `external-services` and `application`.
`PooledRestClientFactory` must keep the two registration expressions verbatim — the gate
greps for the exact strings `new ExternalClientMetricsInterceptor(name, meterRegistry)`
and `new LogbookClientHttpRequestInterceptor(logbook)`.

### C3 — Coverage gate is inverted, and the inversion is inert · **Blocking**

| | |
|---|---|
| **Current** | Strict values live only inside a `-Phandoff` profile, and *nothing reads them*: `jacoco-check` hardcodes `<minimum>0.8</minimum>` for LINE and declares no BRANCH limit at all. Seven hand-written package globs are excluded. CI runs `mvn verify -Phandoff`. |
| **AIAE** | 0.80 line / 0.70 branch are the *pom defaults*, so a bare `mvn verify` is always strict; `-Pmvp` relaxes to 0.30/0.25 and is passed only while a committed `.template-phase` reads `mvp`. |

The effect: `jacoco.line.coverage` / `jacoco.branch.coverage` are dead configuration,
branch coverage is not gated in any profile, and `**/models/**`, `**/entities/**`,
`**/repositories/**`, `**/config/**`, `**/*Entity.class`, `**/*Exception.class`,
`**/*_.class` are invisible to the check. `check-coverage-integrity.sh` reports 15
problems — each exclude counted twice because report and check duplicate the list.

**Resolution.** Move 0.80/0.70 into `<properties>`, wire both limits to those properties,
add a BRANCH limit, delete the seven hand-written excludes (keep only the `**/api/v1/**`
generated ones), replace the `handoff` profile with `mvp`, and commit `.template-phase`.
Land all of it in one change — see **R2**.

### C4 — Left navigation: an AIAE gate contradicts an AIAE rule · **Decided (D1)**

The product ships a real left sidebar — `app/AppShell.tsx`, `shared/ui/Sidebar.tsx`,
`shared/ui/sidebar.css`, with deliberate suppression in lesson-reader mode.
`verify-gates.sh` hard-fails on any occurrence of
`Sidebar|SideNav|LeftNav|side-nav|left-nav|…` anywhere under `frontend/src`, and the
scaffold ships a top-header shell instead.

But the AIAE `CLAUDE.md` itself states: *"Never replace an established product flow,
navigation model, or visual system with a template default unless the user explicitly asks
for that change."* The gate and the rule point in opposite directions, and only for a
project like this one — a real product that pre-dates the gate.

**Resolution.** Decided — keep the sidebar and add a navigation-model exemption to the
standard. Where a rule and a gate disagree, the rule is the standard. See **D1** in
section 6, and section 6.1 for the template-side change this requires.

### C5 — `.gitignore` suppresses the agent surface AIAE requires · **Blocking**

| | |
|---|---|
| **Current** | Ignores `.agents/`, `AGENTS.md`, `replit.md` under the heading "Template control plane — DO NOT COMMIT TO COMPANY REPO". |
| **AIAE** | "The active dual-agent runtime is part of the app: `AGENTS.md`, `replit.md`, `.agents/`, `CLAUDE.md`, and `.claude/` are committed so both agents work after clone." Only `templates/` and `custom_instruction/` are ignored. |

This is a load-bearing ordering constraint: adding the three surfaces before fixing
`.gitignore` produces a working tree that looks converged and a repository that isn't.
`check-agent-surfaces.sh` in the AIAE CI exists precisely to catch this. The AIAE version
also handles `.claude/tasks/*` with a `!README.md` negation the current file lacks.

### C6 — Shared rules and docs have forked · **Material**

Byte-for-byte comparison against the standard:

- **Rules** — 6 of 8 shared files differ. Missing: `13-bigquery.md`,
  `60-documentation-sources.md`. Extra: `.claude/rules/README.md`, superseded upstream by
  `agent_docs/rule-loading-conventions.md`.
- **Agent docs** — 7 of 12 differ. Missing 6 entirely: `agent-operating-model.md`,
  `context7.md`, `distributed_cache.md`, `html_only_project_migration.md`,
  `project_shape_decision.md`, `rule-loading-conventions.md` — plus all 11 canonical topic
  directories (`auth/`, `caching/`, `errors/`, `frontend/`, `generation/`,
  `integrations/`, `observability/`, `openapi/`, `performance/`, `structure/`,
  `testing/`).
- **Skills** — 5 of 6 present, 8 payload skills missing. The project's
  `rule-compliance-audit` is upstream's deliberately-unselected generic variant; the
  standard ships `aiae-rule-compliance-audit`. `agent-payload.skills` documents why they
  must not both exist: identical trigger text means no agent can choose between them.
- **CLAUDE.md** — a hand-edited fork. It drops Decision Ownership, Context7, the HTML-only
  migration section, and the `architecture-overview.md` read step; it also encodes the C2
  divergence as policy.

**Resolution.** `bash scripts/install-claude-fixtures.sh <project>` from the AIAE checkout
installs all of it and rewrites doc citations. It **overwrites** CLAUDE.md — the
project-specific paragraphs must be re-applied afterwards, not before.

### C7 — `.replit` breaks the workflow contract · **Material**

Three assertions in `verify_replit_file()` fail. The project delegates its two workflow
tasks to `replit-dev-backend.sh` / `replit-dev-frontend.sh`, so the literal strings the
gate greps for never appear in `.replit`: it never sources `scripts/replit-env.sh`, never
runs `npm run generate:api` before Vite, and never names
`backend/application/pom.xml`. There is also no `onBoot` hook.

**Resolution.** Adopt the AIAE inline workflow form, carrying over the project-only
`VITE_CLERK_JWT_TEMPLATE = "aidigital-api"` env entry, which the scaffold `.replit` does
not have. Keep the two dev scripts if useful, but the contract strings must live in
`.replit`.

### C8 — Error mapper and time boundary are not split · **Material**

AIAE ships `error/GlobalExceptionHandler` plus `error/mapper/GlobalExceptionResponseHelper`
and its `Impl`, specifically so the handler contains no private helper methods. The
project has the handler alone. Similarly, `service/common/time/CurrentTime.java` performs
`LocalDateTime.now(ZoneOffset.UTC)` and `Instant.now()` itself; AIAE splits it into an
interface plus `CurrentTimeImpl`, which is why the production scanner flags it.

Mechanical extraction in both cases; the scaffold files are drop-in references.

### C9 — Smaller mismatches · **Low risk**

- **`docker-compose.yaml`** — AIAE `local-verify.sh` and the CI `local-dev-dry-run` job
  both look for `docker-compose.yml`. Rename.
- **Ehcache without cache-management** — L2 caching is live (8 entity regions, 8 query
  regions, `use_second_level_cache: true`), but the `cache-management` module, its
  `0003-cache-invalidation` changelog, `@EnableScheduling` and
  `ApplicationCacheNamesByClassRegistry` are absent, so there is no cross-node
  invalidation path. structure-lint treats that pairing as mandatory in both directions.
  **Resolved by D2: adopt the module.**
- **Logbook sink form** — the project's `LogbookConfig` resolves formatter and strategy
  through `resolveFormatter()` / `resolveStrategy()` so bodies can be enabled locally.
  Functionally this is *stricter* than the scaffold, but the gate greps for two literal
  expressions and fails.
- **`UploadValidator` imports `MultipartFile`** — the only violation of the "service must
  not import web APIs" rule; one import in one file.
- **README** — content is excellent but lacks the API / Swagger UI / OpenAPI YAML link
  block the gate requires.
- **CI shape** — 2 jobs (backend, frontend) against the standard's 5 (static-checks,
  unit-tests, integration-tests, frontend-checks, local-dev-dry-run). The 15 KB current
  workflow versus 37 KB upstream is almost entirely the missing static-checks job.

---

## 3. Missing AIAE requirements

Present in the standard, absent here. Verified by direct file-existence checks.

| Area | Missing | Consequence |
|---|---|---|
| **Agent runtime** | `AGENTS.md`, `replit.md`, `.agents/skills/` (11 skills), `.mcp.json`, `agent-payload.skills`, `llm-aux.lock`, `.template-phase`, `.template-version` | Replit Agent has no entry point; no Context7 MCP; no pinned template revision, so `sync-llm-aux.sh` has nothing to verify against |
| **Backend modules** | `backend/observability/`, `backend/migrations/` (as named), `backend/cache-management/`, `backend/DEPENDENCY-ANALYSIS.md`, `backend/config/checkstyle-test-fields.xml` | structure-lint fails 6 module assertions; Checkstyle cannot enforce the private test-field rule |
| **Gate harness** | All 16 AIAE runtime scripts (`structure-lint`, `verify-gates`, `setup-project`, `materialize-project`, `apply-package-name`, `prepare-engineering-handoff`, `configure-clerk-development`, `docker-local-smoke`, `docker-context-path-smoke`, both `remove-*`, `strip-scaffold-samples`, …) plus all 24 `scripts/lib/*` checkers | No local or CI enforcement of anything in section 1. The project's `local-verify.sh` runs tests only — no gates, no `mvn verify`, no compose check |
| **Frontend tooling** | `eslint.config.js`, `eslint-rules/import-section-order.mjs`, a `lint` npm script, `scripts/prepare-husky.mjs`, `.husky/pre-commit` | No linting at all today; the pre-commit hook that runs it is absent |
| **Documentation** | `docs/architecture-overview.md` (`docs/` exists but was empty and untracked) | `prepare-engineering-handoff.sh` refuses to run without it |
| **Rules / docs / skills** | 2 rule files, 6 agent docs, 11 canonical topic directories, 8 payload skills — see C6 | Agents work from a stale, locally-forked contract |

---

## 4. Incompatible technology

The backend stack is fully compatible — Java 21, Spring Boot 3.4.0, Maven multi-module,
PostgreSQL, Liquibase, MapStruct, Lombok all match, and the extra dependencies
(commons-csv, OWASP sanitizer, Testcontainers, Instancio) are additive. **Every
incompatibility is in the frontend.**

| Technology | Status | Why |
|---|---|---|
| `@emotion/react`, `@emotion/styled` | **Banned** | CSS-in-JS. Forbidden by name in the standard's `40-frontend-rules.md` — *and by the project's own copy of that file*, which was never edited to permit it |
| `@mui/material`, `@mui/icons-material` | **Banned** | A second component and styling system layered on Emotion. The standard's visual system is plain CSS with BEM and semantic tokens |
| `sass` | **Banned** | "Use plain CSS with BEM naming" |
| `vitest ^4.1.10` | **Pin conflict** | `verify-gates.sh` requires the firewall-approved `^3.2.6` and fails on any other value |
| `vite ^8.1.4`, `@vitejs/plugin-react ^5.2.0` | Drift | Standard pins `vite ^5.4` / plugin `^4.3.4`. Not gate-enforced, but the vitest pin drags the build toolchain with it |
| `eslint` | **Absent** | Not a dependency at all. The standard requires a flat config plus a custom import-section-order rule |
| `@base-ui/react`, `@radix-ui/*`, `class-variance-authority` | Unreviewed | Headless-primitive and variant libraries outside the standard's dependency set. Not banned, but "do not add dependencies casually" applies |
| `@tiptap/*` (13 packages), `shared/editor/` | **Keep** | A genuine product capability with no scaffold equivalent. Accounts for 119 of the 2222 CSS findings. **Decided:** relocate to `frontend/vendor/editor`, outside the scanned path — vendored dependency code is not project source. Confirmation requested upstream as CR-3 |
| Raw `fetch()` ×2 | **Keep** | Presigned direct-to-S3 `PUT` in `useLessonMutations.ts` and `useMaterialMutations.ts`. These *must not* go through the typed API client, and the only alternative violates `14-performance.md`. **Gate failure carried deliberately** — see §6.2 and CR-1 |

---

## 5. Risky migrations

Ordered by blast radius. R1 is the only one that can damage production data.

### R1 — Renaming the changelog paths breaks `DATABASECHANGELOG` · **Data risk**

Liquibase identifies an applied changeset by the triple *(id, author, filename)*. The 14
changesets in `1.0.0/db.version-master.xml` are recorded in every live database under their
current path. Moving the module from `backend/db` to `backend/migrations` changes the
resource path, so on the next startup Liquibase sees 14 *unknown* changesets and attempts
to re-run them — against a schema where every object already exists.

The `preConditions onFail="MARK_RAN"` already present on 13 of 14 would mostly absorb this,
which is a real piece of luck. It is not a guarantee: the `jsonb_array_contains_ci`
changeset uses an `sqlCheck` precondition rather than a table-existence one, and `MARK_RAN`
writes new rows rather than reconciling old ones, leaving 28 rows describing 14 changes.

**Mitigation.** Pin `logicalFilePath` on the master changelog to its current value so the
recorded filenames do not change, *or* run `liquibase changelogSync` against every
environment as part of the release. Rehearse against a restore of production before
merging. Do this step alone, in its own commit.

### R2 — Strict coverage turns CI red the moment it lands · **Delivery risk**

Removing the seven hand-written JaCoCo excludes changes the denominator. Nobody knows the
real line and branch coverage of this codebase today, because `**/models/**`,
`**/entities/**`, `**/repositories/**` and `**/config/**` have never been counted, and
BRANCH has never been gated at all.

**Mitigation.** Measure first with the excludes removed and the gate skipped, then land the
pom rewrite and `.template-phase=mvp` in the same commit so the 0.30/0.25 floor applies
immediately. Only raise to `engineering` via the `finalize-coverage` skill once the real
number clears 0.80/0.70.

### R3 — Adding OpenAPI constraints is a live contract change · **Behaviour risk**

82 inputs currently accept anything. Adding `minLength`, `maxLength` or `pattern` means
requests that succeed today start returning `400` — including requests the existing
frontend sends. With zero negative-400 tests in the suite, nothing will catch a constraint
that is too tight.

**Mitigation.** Write the negative tests *before* the constraints. Derive each bound from
the actual column width in `1.0.0/sql/*.sql` rather than guessing. Where a field genuinely
has no bound, use `x-unconstrained-reason` — the gate accepts it and it is the honest
answer.

### R4 — Removing MUI and Emotion is a UI rewrite, not a dependency bump · **Regression risk**

MUI components and Emotion styling are woven through the product surfaces —
`features/library` alone is 54 files. Removing them touches visual output on every screen,
and the CSS token migration (2103 findings in product code) overlaps the same files.

**Mitigation.** Do it surface by surface in parity mode, one feature per PR, with
before/after screenshots. Combine the MUI removal and the token migration per surface
rather than doing two full sweeps.

### R5 — The vitest pin drags the whole build toolchain backwards · **Compatibility risk**

Meeting `vitest ^3.2.6` in practice means Vite 5 and `@vitejs/plugin-react ^4`. Tiptap
3.22, `@base-ui/react` and `@tsparticles/confetti` were adopted on Vite 8 and may not
support that. This is the one item where the project may be genuinely ahead of the standard
rather than behind it.

**Mitigation.** The downgrade is now **mandatory**, not conditional — with the template
read-only (§6.1), raising the pin upstream is not available to this team. Run the
compatibility spike in **Phase 0**, not Phase 6, because the result determines whether this
is a routine version alignment or a third deadlock. If tiptap 3.22 cannot run on Vite 5,
escalate alongside §6.2; forking the pin locally re-creates exactly the drift this
migration exists to remove.

### R6 — Thin-controller and service-size refactors touch live business logic · **Regression risk**

55 controller violations across 12 controllers and 25 service-shape violations. Splitting
`LearningServiceImpl` (357 lines, 16 public methods, 13 injected fields) or
`LessonServiceImpl` (332 lines, 11 fields) is the kind of change that is easy to get subtly
wrong, and the safety net — negative MVC tests — does not exist yet.

**Mitigation.** Sequence after Phase 4. Controllers first (mechanical: push branching into
services or API mappers), service splits second, one aggregate per PR.

---

## 6. Decisions

Both product decisions are settled. **D1** (navigation) is implemented in Phase 6; **D2**
(caching) in Phase 3c. §6.1 records the constraint that shaped them — the template
repository is read-only for this team — §6.2 the single gate failure that constraint leaves
behind, and §6.3 the requests sent upstream. Upload-path decisions are in §7.1.

### D1 — Left sidebar navigation · **DECIDED: rebuild as the top-header shell**

**Decision (signed off 2026-08-09).** Replace the left sidebar with the scaffold's
top-header `AppShell`.

**Why this is compliant, which is subtle.** The AIAE rule reads *"Never replace an
established product flow, navigation model, or visual system with a template default
**unless the user explicitly asks for that change**."* The sign-off is not paperwork around
the rule — it is the clause that makes the rebuild permitted. Without it, complying with
the gate would itself have broken the rule. Record the sign-off in the commit that does the
work, so the compliance argument survives with the code.

**History.** First decided as *"keep the sidebar, add a gate exemption to the standard"* —
the only resolution where both the product and the template end up correct. That was voided
when `AIAE-replit-llm-aux` was established as read-only for this team (§6.1). The reasoning
still holds and has been forwarded upstream as **CR-2** in
`docs/aiae-template-change-requests.md`, because the next brownfield project hits the same
wall and may not have a navigation model it can afford to discard.

**Scope, so nothing is lost in the rebuild.** `app/AppShell.tsx`, `shared/ui/Sidebar.tsx`
and `shared/ui/sidebar.css` are replaced and 14 pages are re-laid-out. The one item that is
**not** incidental styling: `isLessonReadingRoute` currently suppresses the whole sidebar on
`/lessons/:id` but restores it on activity routes, giving lesson reading a distraction-free
surface. That is product behaviour and must be deliberately re-expressed in a top-header
layout — decide what "reading mode" means with a top bar before the rebuild starts, not
during it.

**Explicitly rejected.** The gate matches identifier and class-name tokens, so renaming
`Sidebar` to `NavRail` and `app__sidebar` to `app__rail` would turn it green while changing
nothing about the UI. Someone will suggest this. A green gate obtained that way is worse
than a documented red one, because it destroys the signal for everyone afterwards.

### D2 — Hibernate L2 cache · **DECIDED: adopt `cache-management`**

**Decision.** Adopt `backend/cache-management` with the persisted-invalidation protocol in
`.claude/agent_docs/distributed_cache.md`, ahead of a future multi-node deployment.

This is the right call, and the current state is worse than "an unused `ehcache.xml`" —
L2 caching is fully live today with no cross-node invalidation path of any kind:

| Cached surface | Count | Detail |
|---|---:|---|
| Hibernate entity regions | 8 | `UserRole`, `LessonStatus`, `LessonPublicationStatus`, `LessonContentFormat`, `LessonAssetKind`, `MaterialFileKind`, `ActivityType`, `ActivityProgressStatus` — all `CacheConcurrencyStrategy.READ_ONLY`, all with explicit region names |
| Named query regions | 8 | `findUserRoleByCode`, `findLessonStatusByCode`, `findLessonPublicationStatusByCode`, `findLessonContentFormatByCode`, `findLessonAssetKindByCode`, `findMaterialFileKindByCode`, `findActivityTypeByCode`, `findActivityProgressStatusByCode` |
| Hibernate default regions | 2 | `default-query-results-region`, `default-update-timestamps-region` |

`application.yml` sets `use_second_level_cache: true`, `use_query_cache: true`,
`region_prefix: hibernate-cache`, `missing_cache_strategy: fail`, and
`sharedCache.mode: ENABLE_SELECTIVE`. Every region has an explicit
`<heap unit="entries">` bound in `ehcache.xml`. Against the standard's *"When L2 is
appropriate"* checklist this is a textbook-correct use — read-mostly dictionary entities,
stable IDs, bounded cardinality, explicit regions, explicit sizing.

**What is actually missing is only the invalidation half.** Two properties keep today's
exposure low: all 8 entities are `READ_ONLY`, and
`immutable_entity_update_query_handling_mode: exception` makes Hibernate throw rather than
silently drift if anything tries to update one. Dictionary rows change only through
Liquibase at deploy time, when nodes restart with empty heap-local caches anyway. So the
present risk is genuinely small — but it is small *by accident of what happens to be
cached*, not by design. The first mutable entity or admin-editable dictionary added to a
region breaks silently, and only on multi-node.

**Scope of adoption (Phase 3c).** Install the module, its `0003-cache-invalidation`
changelog and `@EnableScheduling`; wire `CacheConfig` so Spring and Hibernate share one
`javax.cache.CacheManager` instance; add the empty `ApplicationCacheNamesByClassRegistry`
extension point. Two deliberate scoping notes:

- The registry stays **empty on day one**. The protocol's mutation/outbox contract applies
  to *mutable* sources feeding a region; all 8 current sources are `READ_ONLY` and
  deploy-time only. Registering them would add publish calls no transaction ever makes.
  The registry becomes load-bearing the moment a mutable source is cached — which is
  exactly the trigger the standard's *"Required verification when installed"* list guards.
- Warm-up stays **disabled by default**, per the standard. Turn it on only with
  measurements, and never with an unbounded `parallelStream()` — structure-lint rejects
  that shape explicitly.

**Dependency.** `.claude/agent_docs/distributed_cache.md` is one of the six agent docs
missing today (see C6); it arrives in **Phase 1**. Phase 3c must not be executed before
that document is installed, since it is the protocol being adopted.

### 6.1 Constraint: `AIAE-replit-llm-aux` is read-only for this team

**Established 2026-08-09.** The template repository cannot be edited here, so every
resolution that depended on changing a gate had to be re-decided. It does have a
maintainer, and requests are being routed to them — see §6.3.

| Finding | Preferred resolution | Outcome |
|---|---|---|
| **D1** — sidebar | Navigation-model exemption in `verify-gates.sh` | Re-decided: rebuild as top-header shell. Forwarded upstream as **CR-2** |
| **§4** — raw `fetch()` ×2 | Exemption for presigned direct-to-S3 `PUT` | **Unresolvable here.** Raised as **CR-1**; failure carried in the interim (§6.2) |
| **§4** — vendored tiptap | Vendored-code exemption for `shared/editor` | Resolved project-side by relocating out of `frontend/src`; confirmation requested as **CR-3** |
| **R5** — `vitest ^3.2.6` | Raise the pin upstream if tiptap needs Vite 6+ | Downgrade attempted first; escalated as **CR-6** only if the spike fails |

### 6.2 One carried failure: the presigned upload gate

`verify-gates.sh` fails on any `fetch(` in a non-test `.ts`/`.tsx` file under
`frontend/src`; `axios` and `XMLHttpRequest` are banned by the same assertion. The two call
sites in `useLessonMutations.ts` and `useMaterialMutations.ts` perform a presigned `PUT`
straight to S3, which by design must not pass through the typed API client. The only
alternative implementation is proxying uploads through the backend — which violates
`14-performance.md` ("Stream or presign file transfers; do not heap-buffer complete files
without a small enforced limit") and is a real regression on large lesson materials.

**The gate forbids the only implementation the standard's own performance rules allow.**
This is a defect in the gate, not in the project.

**Status: raised with the template maintainer as CR-1.** Until it is resolved, this project
carries exactly one failing gate assertion, knowingly and with the reasoning recorded here
and in `docs/aiae-template-change-requests.md`. Two things must not happen in the meantime:
uploads must not be routed through the backend to turn the gate green, and the upload
helper must not be relocated out of `frontend/src` to dodge the scan — it is first-party
code, and that is the same gaming rejected under D1.

### 6.3 Upstream requests

Six change requests are recorded in **`docs/aiae-template-change-requests.md`**, written to
stand alone for readers who do not have this audit. CR-1 is the blocker; CR-4 proposes a
single exemption mechanism that would resolve CR-1 through CR-3 together. That file is the
one to hand to the template maintainer.

---

## 7. Project facts that must survive

These have no counterpart in the scaffold. `materialize-project.sh` refuses to run twice for
exactly this reason — every copy in it is unconditional, so a second run silently
overwrites edited files while keeping added ones, which its own comment calls *"the worst
shape of that failure"*. **Nothing in this migration may be executed by re-materializing.**

| Area | Must be preserved |
|---|---|
| **Identity** | Package root `com.aidigital.aionboarding`; groupId `com.aidigital.aionboarding`; parent artifact `app-parent`; version and branch `1.0.0` |
| **Schema** | 14 applied changesets, all with direct `preConditions` (already gate-clean); the versioned `1.0.0/` folder convention; the `jsonb_array_contains_ci` function with `splitStatements="false"` and its explicit rollback |
| **Security** | `PermissionEvaluator.java` — project-only bean with no scaffold equivalent. Clerk-only auth with `AUTH_ALLOWED_EMAIL_DOMAIN` defaulting to `aidigital.com`, `VITE_CLERK_JWT_TEMPLATE=aidigital-api` (set in `.replit [env]`, absent from the scaffold), and CSP `frame-src` allowances for Clerk, Cloudflare Turnstile and YouTube |
| **Integrations** | Five outbound clients in `external-services`: OpenAI, HeyGen, YouTube, S3/CloudFront storage, and the SSRF-resistant link fetcher with its five-class `link/support` guard. The link guard is security-critical and must not be refactored casually |
| **Scheduled work** | `AbandonedUploadCleanupJob`, `MaterialYoutubeBackfillJob` — no AIAE equivalent |
| **App-owned observability** | `PerformanceMetricsFilter`, `RequestAuthenticationCacheFilter`, `IntegrationHealthIndicator`, `ByteCountingResponseWrapper`, `CountingServletOutputStream`, `MetadataOnlyHttpLogFormatter`. These stay in `application` — only the two reusable metrics classes move to `observability` |
| **Web config** | `DashboardPeriodV1Converter`, `UserRoleCodeV1Converter`, `StaticAssetCacheConfig`, `support/ApiResponses` |
| **Frontend** | Sidebar navigation with reading-mode suppression (pending D1); the vendored tiptap editor under `shared/editor`; the direct-to-S3 presigned upload path; `shared/api/queryPolicies.ts` and `queryClient.ts` conventions; `features/library` (54 files, the largest surface) |
| **Tooling** | `backend-jacoco-coverage-targets.sh`, `report-bundle-size.sh`, `local-dev-backend.sh`, `local-dev-frontend.sh`, `replit-dev-*.sh` — keep alongside the AIAE scripts, do not replace them with it |
| **Environment** | ~40 project-only `.env.example` keys: `OPENAI_*`, `HEYGEN_*`, `YOUTUBE_*`, `S3_*`/`AWS_*`/`RAILWAY_BUCKET_*`, `APP_EXTERNAL_HTTP_*`. The AIAE `.env.example` is a subset, not a superset |

---

## 7.1 Direct-to-S3 upload path — decisions and residual risk

The presigned direct-to-S3 upload is **retained**; it predates this migration and is the
implementation the standard's own performance rules require. §6.2 covers the gate conflict.
This section covers the upload path on its own merits, independent of any gate.

**The implementation is careful.** Reviewing it end to end, it already closes the traps that
usually sink direct-to-storage uploads:

| Trap | Closed by |
|---|---|
| Client chooses its own storage path | Key is server-generated: `uploads/<uuid>/<sanitized-name>` |
| One user registers another's object | `pending_uploads.owner_user_id` verified at confirm |
| Same object attached to two entities | `markConfirmedIfUnconfirmed` — atomic conditional update, deliberately not read-then-save |
| Entity saved for a file that never arrived | `headObject` existence check at confirm |
| Client lies about file size | Confirm compares real `sizeBytes` against `expected_size_bytes` |
| Objects orphaned by abandoned uploads | `AbandonedUploadCleanupJob`, 15 min, bounded batch, `idx_pending_uploads_cleanup_sweep` |
| Browser blocked by CSP | `connect-src 'self' https:` already permits it |

Four residual items, with decisions.

### 7.1.1 Bucket CORS lives outside the repository — **fix in Phase 2**

The browser `PUT` requires the S3 bucket to allow the app origin with the `PUT` method and
the `Content-Type` header. That configuration lives in AWS, not in git: CI cannot see it,
review cannot catch it, and a new environment or changed deployment domain breaks uploads
with an error the browser deliberately makes unreadable. The frontend already anticipates
this — the catch block in `useMaterialMutations.ts` notes the failure "surfaces from
`fetch()` as an opaque, unreadable" error.

**Decision: document the required CORS policy in `docs/architecture-overview.md`** when that
file is written in Phase 2, including the exact origins per environment. This is the
highest-value item in this section: it costs nothing and it is the one that will actually
bite, on the day someone stands up a new environment.

### 7.1.2 Declared content type is trusted — **add a real content check**

S3 enforces that the `PUT` carries the *signed* content type, so the stored type always
matches the declared one — but the declared type comes from the browser (`file.type`).
Renaming `evil.html` to `report.pdf` yields `application/pdf`, passes the purpose
allowlist, and is stored as such. `confirmUpload` re-checks size only, never content.
Containment is real but partial: files are served from CloudFront/S3 on a different host
from the app, under `nosniff` and the app CSP — but materials are shown to other staff.

**Decision (2026-08-09): add server-side content verification.** On confirm, read the
object's leading bytes and verify they match the declared type; reject on mismatch. This
fits the existing `confirmUpload` step, which already fetches object metadata, and it
composes with the existing per-purpose allowlist rather than replacing it. Schedule
alongside Phase 3's storage work.

### 7.1.3 Presigned PUT cannot cap transfer size — **accepted risk**

A presigned `PUT`, unlike a POST policy, carries no maximum content length. A client can
push 5 GB against a URL signed for a 1 MB declared size. `confirmUpload` rejects it on the
size comparison and the sweep reclaims it, so it never becomes a material — but the bytes
are transferred and stored, and billed, until cleanup.

**Decision (2026-08-09): accepted, no change.** Upload URLs are only issued to
authenticated users inside the allowed email domain, and the 15-minute sweep bounds the
window. Recorded as a knowingly accepted risk rather than an oversight. Revisit if the user
base ever widens beyond the company domain, or if storage cost anomalies appear — an S3
lifecycle rule on unconfirmed uploads is the cheap mitigation if it becomes real.

### 7.1.4 Cleanup job is not multi-node safe

Covered as **M4** in §8, where it belongs with the rest of the multi-node work.

---

## 8. Multi-node readiness

Multi-node operation is a stated objective, not a hypothetical. This section is a full
sweep of the codebase for node-affinity hazards, independent of the AIAE gates — none of
the gates check most of this.

**The hard parts are already done.** This codebase was written with more multi-node
discipline than most single-node deployments have:

| Property | Evidence |
|---|---|
| Stateless sessions | `SessionCreationPolicy.STATELESS`; Clerk JWT per request. No sticky sessions or session replication needed |
| Optimistic locking on aggregates | `@Version` on `Lesson`, `Material`, `Roadmap`, `Group`, `UserLesson`, `LessonActivity`, `UserLessonActivityProgress`, `LessonAssistantConversation`, `RoadmapGroupAssignment` — with a `GlobalExceptionHandler` mapping for the conflict. This is the single most important multi-node property and it is in place |
| Contended user provisioning | `UserRepository` uses `@Lock(PESSIMISTIC_WRITE)` |
| Upload confirmation races | `markConfirmedIfUnconfirmed` is an atomic conditional update, deliberately not read-then-save. Already cross-node safe |
| Long-running generation | Lesson generation runs in-request with status persisted to the database; the client polls a status endpoint, so a poll may land on any node and still be correct |
| No local disk state | All file bytes go to S3; no temp files, no local uploads directory |
| Request-scoped auth cache | `RequestAuthenticationCacheFilter` clears a per-request `ThreadLocal`. Despite the name this is not a shared cache and is not a hazard |
| Per-node async logging | The usage-logging `@Async` executor writes each node's own events with a bounded queue. Correct as-is |
| Shared constants | Every `static final Map`/`Set` found is an immutable constant, not mutable shared state |

**Four gaps to close.** Ranked by what actually bites first.

| # | Gap | Impact on N nodes | Fix |
|---|---|---|---|
| **M1** | L2 cache has no cross-node invalidation | A write on node A leaves node B serving stale reads | Already **D2** — adopt `cache-management` (Phase 3c) |
| **M2** | `MaterialYoutubeBackfillJob` runs on every node | N× YouTube API quota burn every 5 minutes, plus duplicate writes to the same materials. YouTube quota is a hard daily limit, so this fails loudly and expensively | Coordinate the job (below) |
| **M3** | `DictionaryLookupService` holds its own `ConcurrentHashMap` | A second, unmanaged cache beside the JCache manager, never evicted or invalidated | Fold into the managed cache (Phase 3c) |
| **M4** | `AbandonedUploadCleanupJob` runs on every node; `TeacherVideoRefreshService` refreshes on read | Duplicate S3 deletes (idempotent, wasteful) and duplicate HeyGen polls that then collide on `Lesson.@Version`, surfacing as a 409 to one user | Claim-based batching and a refresh guard |

### M2 / M4 — coordinating scheduled and on-read work

Both `@Scheduled` jobs are plain `fixedDelay` methods with no coordination, so every node
runs them. `AbandonedUploadCleanupJob` is nearly harmless duplicated — a second S3 delete
of the same key is a no-op — but `MaterialYoutubeBackfillJob` calls a quota-limited third
party every five minutes, and that scales linearly with node count.

Two mechanisms fit, and the codebase already knows both idioms:

- **Claim the batch** — `SELECT … FOR UPDATE SKIP LOCKED` on the rows each run processes.
  Natural fit for both jobs, since both are already batch sweeps over a bounded set, and it
  parallelises rather than serialises: two nodes take disjoint batches. Preferred.
- **Advisory lock** — `pg_try_advisory_lock` around the whole run, so exactly one node
  executes and the others skip. Simpler, but wastes the other nodes' scheduled tick.

`TeacherVideoRefreshService` is a different shape: it is triggered by reads, so two nodes
serving concurrent reads of the same lesson both poll HeyGen. `Lesson.@Version` means
correctness is protected — the second write fails rather than corrupting — but the second
HeyGen call is wasted quota and one user sees a conflict for something they did not do.
Guard it with a conditional claim on the lesson's refresh timestamp so only the winner
polls.

### M3 — the second cache

`DictionaryLookupService` memoises dictionary `code → id` in a bean-level
`ConcurrentHashMap` with no eviction. `distributed_cache.md` names this exact shape as
forbidden: *"Do not create `ConcurrentMapCacheManager`, static maps, or a second JCache
manager."*

Runtime risk today is genuinely low — dictionary rows are Liquibase-seeded, their IDs are
immutable, and `computeIfAbsent` caches nothing on a miss, so a newly added code is still
picked up. The problem is architectural: it duplicates what the L2 entity regions and the
eight `findXByCode` query regions already cache, and it sits outside the invalidation
protocol being adopted in D2. Two caches for the same data, one of which no invalidation
can reach, is precisely the state that makes a future stale-read bug unexplainable. Fold it
into the managed cache in Phase 3c, while that code is already open.

### Verification to add

The standard's cache checklist covers M1. These do not appear in any gate and need
explicit tests:

- A scheduled job invoked concurrently from two threads processes disjoint batches (M2/M4).
- Two concurrent teacher-video refreshes result in one HeyGen call, not two (M4).
- Dictionary lookups resolve through the shared cache manager, with no second cache
  instance reachable from `DictionaryLookupService` (M3).

---

## 9. Phased migration plan

Eight phases, ordered by dependency rather than by size. Phases 1 and 2 carry no runtime
risk and can start immediately. Phase 3 touches the database and should be its own release.
Each phase has an exit gate that can be checked mechanically.

### Phase 0 — Baseline and decisions *(no change)*

Capture the "before" state while it is still green: run the current
`mvn -f backend/pom.xml -Phandoff verify` and the frontend suite and record the results,
then re-run the AIAE gate suite and archive the numbers in section 1 as the tracking
baseline. **All decisions are already made** (§6) — no phase is waiting on one. Two
carry-over actions belong here: send `docs/aiae-template-change-requests.md` to the
template maintainer, and run the R5 compatibility spike, which is now on the critical path
because the `vitest ^3.2.6` downgrade is mandatory rather than conditional and its result
decides whether CR-6 gets raised. Before the sidebar rebuild starts, settle what "reading
mode" means in a top-header layout (§ D1) — that is product behaviour, not styling, and
deciding it mid-rebuild is how it gets lost. Read `materialize-project.sh`'s re-run guard
before anyone is tempted to shortcut this with a re-materialization.

> **Exit** — baseline recorded; change requests sent; R5 spike result known; top-header
> reading-mode behaviour specified.

### Phase 1 — Agent surface *(no runtime risk)*

Fix `.gitignore` **first** (C5) — un-ignore `.agents/`, `AGENTS.md`, `replit.md`; ignore
`templates/` and `custom_instruction/`; add the `.claude/tasks/*` negation. Then run
`install-claude-fixtures.sh` to bring rules, agent docs, the 11 topic directories and the
payload skills to the pinned revision. Add `AGENTS.md` and `replit.md` from their
templates, mirror `.agents/skills`, add `.mcp.json`, `agent-payload.skills`, `llm-aux.lock`
(revision `690a9748`), `.template-version` and `.template-phase=mvp`. Drop
`rule-compliance-audit` in favour of `aiae-rule-compliance-audit` and remove
`.claude/rules/README.md`. Finally, re-apply the project-specific paragraphs to the
freshly-overwritten `CLAUDE.md` — and do not re-introduce the C2 divergence while doing so.

> **Exit** — `check-agent-surfaces.sh` and `check-installed-documentation-links.py` report
> zero.

### Phase 2 — Install the harness: measure, don't fix *(no runtime risk)*

Copy the 16 AIAE runtime scripts and all of `scripts/lib/` in alongside the project's own
ten. Replace the CI workflow with the AIAE five-job version, with `static-checks` marked
non-blocking so the team sees every number without the pipeline going red on day one. Write
`docs/architecture-overview.md` from the template — the README already contains most of the
content. Add `backend/DEPENDENCY-ANALYSIS.md` and `checkstyle-test-fields.xml`. Rename
`docker-compose.yaml` to `.yml`. Fix the README API/Swagger link block and the `.replit`
workflow contract (C7), carrying `VITE_CLERK_JWT_TEMPLATE` over.

> **Exit** — every gate runs in CI and publishes counts; nothing blocks yet.

### Phase 3 — Backend module topology *(data risk · own release)*

Four sub-steps, each its own commit.

- **3a** Create `backend/observability`; move `ExternalCallTimer` and
  `ExternalClientMetricsInterceptor` to `…observability.external`; wire the dependency from
  `external-services` and `application`; keep the two interceptor registration expressions
  byte-identical.
- **3b** Rename `backend/db` to `backend/migrations` (artifactId too, plus Lombok), keeping
  the `1.0.0/` folder and the 14 `sqlFile` changesets — with `logicalFilePath` pinned or a
  `changelogSync` planned per environment (**R1**), rehearsed against a production restore.
- **3c** Adopt `backend/cache-management` per **D2** / **M1**: the module and its POM
  wiring (`service` depends on it, Lombok declared), the `0003-cache-invalidation`
  changelog included from the master changelog, `JpaCacheInvalidationEventService`, and an
  initially-empty `ApplicationCacheNamesByClassRegistry`. `@EnableScheduling` is already
  present on `Application`. Keep the existing 8 entity + 8 query regions and their
  `ehcache.xml` bounds unchanged; move `CacheConfig` to the shared-manager form so Spring
  and Hibernate use one `javax.cache.CacheManager`. Warm-up stays disabled. In the same
  change, fold `DictionaryLookupService`'s private `ConcurrentHashMap` into the managed
  cache (**M3**) — leaving a second unmanaged cache beside the one being made
  invalidation-aware defeats the point of the phase. Requires
  `.claude/agent_docs/distributed_cache.md` from Phase 1.
- **3d** Coordinate node-affine work per **M2** / **M4**: claim batches with
  `SELECT … FOR UPDATE SKIP LOCKED` in `AbandonedUploadCleanupJob` and
  `MaterialYoutubeBackfillJob`, and add a conditional refresh claim to
  `TeacherVideoRefreshService`. `MaterialYoutubeBackfillJob` is the priority — it burns
  quota-limited YouTube API calls once per node every five minutes.
- **3e** Split `GlobalExceptionHandler` and `CurrentTime` (C8), and move the
  `MultipartFile` boundary out of `UploadValidator`.

> **Exit** — `structure-lint.sh` reports only the 10 mapper findings, which Phase 5 clears.
> Liquibase applies cleanly to a restored production database. The standard's cache
> verification list holds: runtime depends on `cache-management` and enables scheduling,
> registry verification resolves every configured region in CI, mutation commit creates one
> event and rollback none, publication outside a transaction fails, polling processes
> increasing IDs and retries a failed eviction, and Spring and Hibernate receive the same
> JCache manager instance. Plus the §8 verification set: concurrent scheduled runs take
> disjoint batches, two concurrent teacher-video refreshes produce one HeyGen call, and no
> second cache instance is reachable from `DictionaryLookupService`.

### Phase 4 — Coverage and the test contract *(CI risk)*

Rewrite the JaCoCo configuration per C3 — strict defaults in `<properties>`, a real BRANCH
limit, the seven hand-written excludes deleted, the `handoff` profile replaced by `mvp`.
Land it together with `.template-phase=mvp` and drop `-Phandoff` from CI in the same commit
(**R2**). Then close the biggest hole in the suite: 91 constrained operations with zero
`isBadRequest()` assertions. Bring test style to the AIAE rules along the way — private
fields, package-private classes, targeted Instancio `.set(field(...))` — enforced by the new
Checkstyle config.

> **Exit** — `check-coverage-integrity.sh` and `check-api-validation-tests.py` both report
> zero; `mvn verify` with no flags is strict and green.

### Phase 5 — Contract and controller conformance *(regression risk)*

With negative tests in place, add the 82 OpenAPI constraints — bounds derived from the
actual column widths, `x-unconstrained-reason` where a field genuinely has none (**R3**) —
plus the 4 missing descriptions. Then the refactors, one aggregate per PR: 55 controller
violations pushed down into services or API mappers; 10 application mappers stopped from
hand-constructing `new *V1(...)`; 5 oversized and 9 over-injected `ServiceImpl`s split into
collaborators; 8 static factories converted. Resolve the Logbook sink form (C9) — either
match the literal or file a template exception for the configurable resolver, which is the
stricter design.

> **Exit** — `structure-lint.sh`, `verify-gates.sh` and every backend `lib/` checker report
> zero.

### Phase 6 — Frontend convergence *(visual risk)*

Add the tooling first — ESLint flat config, the import-section-order rule, the `lint`
script, Husky `prepare` and `.husky/pre-commit` — so the rest of the phase is guarded.
Align vitest, Vite and the React plugin to the pinned versions; the downgrade is mandatory
now, and the Phase 0 spike should already have told you whether tiptap 3.22 survives it.
Remove Emotion, MUI and sass surface by surface, folding the CSS token migration into the
same PRs: 2103 product findings. Relocate vendored `shared/editor` outside `frontend/src`
per §6.2, which removes its 119 findings honestly rather than by forking upstream tiptap.

Per **D1**, replace the sidebar with the scaffold's top-header `AppShell`, using the
reading-mode behaviour specified in Phase 0. Cite the product sign-off in that commit —
it is what makes the change rule-compliant. Do not settle for renaming `Sidebar` to
`NavRail`; that turns the gate green without changing the UI.

**Leave the two presigned-upload `fetch()` call sites intact** (§6.2). Do not route uploads
through the backend to make the gate pass — that trades one gate failure for a
performance-rule violation and a real product regression.

> **Exit** — `check-frontend-ui-rules.sh` reports zero; `npm run lint` is green and hooked
> to pre-commit. `verify-gates.sh` fails on exactly one assertion — the presigned upload —
> and that failure stays visible and attributed until CR-1 is resolved (§6.2).

### Phase 7 — Make it stick *(lock-in)*

Flip CI `static-checks` to blocking and replace the project's `local-verify.sh` with the
AIAE one, so gates, `mvn clean verify`, the frontend suite and the compose syntax check all
run before every push. Run the `finalize-coverage` skill and move `.template-phase` to
`engineering`, which unlocks `prepare-engineering-handoff.sh`. Document the
`sync-llm-aux.sh --update-lock` upgrade path so the next template revision arrives as a
reviewed diff rather than as drift.

> **Exit** — `bash scripts/local-verify.sh` passes end to end with no skipped step, and the
> AIAE revision in `llm-aux.lock` is current.

---

## Method

AIAE scaffold gates were copied to a scratch directory and run unmodified against the
working tree, except that `structure-lint.sh` and `verify-gates.sh` had their `fail()`
function and `set -e` relaxed so they report all findings instead of aborting at the first.
Python gates were run directly with an explicit interpreter, because `python3` on this
machine resolves to the Windows Store stub and silently no-ops.

No file in `AIAE-onboarding-platform` was modified or created during the audit itself.
Counts reflect the tree at commit `9f6e93e` on branch `1.0.0`.
