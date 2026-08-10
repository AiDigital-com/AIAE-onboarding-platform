# AIAE Convergence — Executable Migration Plan (v2)

**This document is the single source of truth for the migration.** Where it disagrees
with `docs/aiae-audit.md`, this document wins. Where it is silent, the audit applies.
The v1 plan is superseded and archived at
[`archive/aiae-migration-plan-v1-superseded.md`](./archive/aiae-migration-plan-v1-superseded.md)
— do not read it, do not cite it, do not restore claims from it.

## Status: APPROVED — 2026-08-10

**Signed off by the technical owner. P0 may start.** All five decisions in §6.1 are settled;
nothing in this plan waits on an answer. An agent picking up a phase needs no further
authorisation — it needs that phase's Scope, Do-not-touch and Verification blocks, and the
log.

| | Decision | Settled as |
|---|---|---|
| **D-A** | Scope | Converge the engineering contract. **Do not touch how the product looks or navigates** — MUI/Emotion, the CSS, the left sidebar and all 12 routes stay. Four gates therefore stay red permanently, annotated, never silenced |
| **D-B** | Production access | No deploy, no restore. One read query against `databasechangelog` — **run 2026-08-10, R1 disproved, P5 cleared** (§2.1) |
| **D-C** | Coverage | Target **0.80 LINE / 0.70 BRANCH is fixed** and is a hard P15 exit condition. Only the path is relaxed: `mvp` 0.30/0.25 between P9 and P15 |
| **D-D** | Usage telemetry | **Kept.** `backend/event-logging-to-db-feature` stays; `prepare-engineering-handoff.sh` is never run (P15 step 3a); `.template-phase` still reaches `engineering` |
| **D-E** | Multi-node | Planned, not live. P6 and P7 are **preparation**, and their tests are the only proof they work |

Two things this migration deliberately never proves, recorded so nobody mistakes silence for
success: `ci.yml` never executes, and the Replit deploy path is never exercised (§1, P15
step 6).

| | |
|---|---|
| Project | `AIAE-onboarding-platform` |
| Production branch | `1.0.0` — **never checked out, never a merge target** (§1) |
| Integration branch | `migration` — branched from `1.0.0`; every phase commit lands here. No remote, no CI, no deploy |
| Standard | `C:\Users\Admin\Desktop\migrationv2\AIAE-replit-llm-aux` @ `cc64e49` — **frozen for the duration** |
| Skills pin (`llm-aux.lock`) | `690a9748657adf81d01702dafa2c7ecc8afcf5c5` (v0.2.0) |
| Source audit | [`aiae-audit.md`](./aiae-audit.md) |
| Upstream requests | [`aiae-template-change-requests.md`](./aiae-template-change-requests.md) |
| Plan date | 2026-08-10 |
| Verification basis | Every number below was measured against the working tree on 2026-08-10 |

---

## 0. How to read this document

Every phase below carries the same six blocks. They are written to be executed by an
agent with no prior context, so they are literal on purpose.

| Block | Meaning |
|---|---|
| **Scope** | The only paths the phase may create, move, or modify |
| **Do not touch** | Paths that will look relevant and are not. Editing one fails the phase |
| **Steps** | The work, in order |
| **Build / Test** | Exact commands. Not paraphrases |
| **Verification** | Named gate(s) with a before → after count, plus any non-gate assertion |
| **Rollback** | The revert, plus any out-of-repo action |

A phase is complete when its Verification block passes **and** its row is appended to
`docs/aiae-migration-log.md`. A green build alone is not completion.

### Which numbers in this document are trustworthy

Two kinds, and they must not be confused.

**Measured directly against the working tree on 2026-08-10** — treat as fact: the 14
changesets and their declared paths; 18 ehcache regions (8+8+2); 40 of 403 frontend files
importing MUI/Emotion and their per-surface split; 71 icons, 9 components, 153 `sx=` props;
153 vendored tiptap files; 12 pages and 11 `<Route>` elements with 12 paths; 10 project
scripts; 5 `link/support` classes; 3 enum `fromValue`; 12 private members in
`GlobalExceptionHandler`; `LearningServiceImpl` 357/13/16 and `LessonServiceImpl` 332/11;
**zero** `isBadRequest`; 15 controllers; 1499 raw `px` and 611 hex across 84 colours against
87 tokens; `fixedDelay` 300_000 and 900_000; every `package.json` version; every file path
and line number cited.

**Carried from the audit and NOT re-verified** — the gate harness is not installed until P2,
so nothing could run them: **55** controller violations, **91** constrained operations,
**82** OpenAPI input constraints, **4** missing schema descriptions, **25** service-shape
violations, **10** mapper findings, **8** static factories, **2222 / 2103** UI-rule findings,
and structure-lint's per-module assertion counts.

**What to do when a measured count differs from one written here.** Record the real number
in the log and use it. Do not adjust code to reproduce the number in this document, and do
not treat the difference as a defect — the audit measured a tree that has since moved. P0
step 3 replaces every figure in the second list with a measured one, and from that point the
log is authoritative over this document.

---

## 1. Branching and working model

**This migration is entirely local.** Nothing is pushed to a remote, nothing is deployed,
and no CI service runs. Every check in this plan is a command run on your own machine.
Where a phase installs CI configuration it is installing a *file* that converges the
repository to the standard — correctness is verified by inspection and by running the same
checks locally, never by watching a pipeline.

`1.0.0` is the production branch. Nothing in this migration merges into it.

```
1.0.0                       production — never checked out for phase work
└── migration               integration branch, lives for the whole migration
    ├── mig/p00-baseline
    ├── mig/p01-agent-surface
    ├── mig/p02-harness-ci
    └── mig/pNN-...
```

**Rules.**

1. One revertible unit = one branch = one squashed commit on `migration`. For most phases
   that is the whole phase; for P11 it is one aggregate per commit. Rollback inside
   `migration` is `git revert <sha>`. **There are no pull requests** — each phase's Review
   step is a review skill run against the diff, and its verdict goes in the log.
2. **No agent touches `1.0.0`** — not to merge, not to check out. Whether and when
   `migration` reaches `1.0.0`, and whether anything is ever deployed from it, is decided
   by a human outside this plan.
3. If `1.0.0` moves while the migration runs, merge it into `migration` and record it.
   Nothing in this plan pushes, pulls, or deploys.
4. **P5 still stands alone.** It is the only phase that touches a database, and it is
   rehearsed against a restored production dump *locally* before its commit lands. See P5.

---

## 2. Corrections register

These are claims from the audit and the v1 plan that were checked against the code on
2026-08-10 and found wrong. They are listed here so nobody reintroduces them. Each was
verified by direct file reading, cited by path and line.

### 2.1 R1 does not exist — the module rename cannot make Liquibase re-run

The audit (§R1) and the v1 plan both state that moving `backend/db` → `backend/migrations`
changes the recorded changelog filename, so Liquibase would see 14 unknown changesets.

**This is false.** `application.yml:75` loads the changelog from the **classpath**:

```yaml
spring:
  liquibase:
    change-log: classpath:db/changelog/db.root-master.xml
```

The changelogs live at `backend/migrations/src/main/resources/db/changelog/`. Everything
under `src/main/resources` is the classpath root, so the classpath path is
`db/changelog/db.root-master.xml`, and the included file resolves to
`db/changelog/1.0.0/db.version-master.xml`. **The Maven module directory name is not part
of that path**, and neither is the artifactId — renaming the jar does not move resources
inside it.

The filename Liquibase records in `DATABASECHANGELOG` is therefore identical before and
after the rename.

**Consequence.** No `logicalFilePath` pin. No `changelogSync`. The `logicalFilePath` vs
`changelogSync` decision in v1 §7 item 7 is withdrawn — it chose between two mitigations
for a non-risk.

**CONFIRMED EMPIRICALLY 2026-08-10.** The query below was run against production and
returned `db/changelog/1.0.0/db.version-master.xml` — the classpath path, carrying neither
`backend/db` nor `backend/migrations`. **R1 does not exist. P5 is cleared to proceed, with
no `logicalFilePath` pin and no `changelogSync`.**

The query, kept for the record and for re-running before P5 lands:

```sql
SELECT orderexecuted, id, author, filename, exectype
FROM databasechangelog
ORDER BY orderexecuted;
```

Expected — and observed — 14 rows all carrying
`filename = db/changelog/1.0.0/db.version-master.xml`. Had the filenames contained a module
directory, §2.1 would have been wrong, R1 real, and P5 stopped until the mitigation was
redesigned. They did not.

**What is actually dangerous, and is not in either source document:** renaming the
*resource* directory `src/main/resources/db/changelog/`. That changes the classpath path
and would re-run all 14 changesets against a populated schema. P5 adds a warning comment
at that path; see P5 step 4.

### 2.2 `CacheConfig` does not exist, and `spring.cache.type: jcache` is a loaded gun

The audit (lines 462, 757) and the v1 plan both instruct: *"move `CacheConfig` to the
shared-manager form so Spring and Hibernate use one `javax.cache.CacheManager`."*

**There is no `CacheConfig.java` in this project.** There is no `@EnableCaching` anywhere
in the repository. Spring's cache abstraction is not in use. Today there is exactly **one**
`javax.cache.CacheManager`: Hibernate's, built from `hibernate.javax.cache.uri: ehcache.xml`.

But the premise is not wrong so much as **premature**. `application.yml:25` declares:

```yaml
spring:
  cache:
    type: jcache        # and spring.cache.jcache.config is NOT set
```

Spring Boot's `CacheAutoConfiguration` is gated on a `CacheAspectSupport` bean, which only
`@EnableCaching` registers. Add `@EnableCaching` and `JCacheCacheConfiguration` activates,
calls `CachingProvider.getCacheManager()` with **no URI** — the provider default, not
`ehcache.xml` — and a second, empty manager appears beside Hibernate's.

**Consequence.** P6 must not add `@EnableCaching`. If Spring-side caching is ever needed,
`spring.cache.jcache.config: ehcache.xml` must be set in the same change. See P6.

### 2.3 The coverage gate is inverted and inert — but the 0.80 LINE floor is live and met

The audit (§C3) states this correctly and precisely; the v1 plan degraded it into "dead
configuration." For the record, verified in `backend/pom.xml`:

- `jacoco-check` hardcodes `<minimum>0.8</minimum>` for **LINE** (line 308). It is live.
- There is **no BRANCH limit** at all.
- `jacoco.line.coverage` / `jacoco.branch.coverage` exist only inside the `handoff`
  profile (lines 440–441) and are **read by nothing**. `-Phandoff` is a no-op.
- Both the `report` and `check` blocks carry the same 10 excludes — 3 generated
  (`**/api/v1/**`) and **7 hand-written**.
- CI (`.github/workflows/ci.yml:36`) runs
  `mvn -f backend/pom.xml -pl service,application,external-services -am verify -Phandoff -B`.

Since CI is green, **the project already meets 0.80 LINE on the current (narrow)
denominator.** Neither source document records this. P9 moves to `mvp` (0.30/0.25), which
is the AIAE phased model and is intentional — but it lowers a floor that is currently
being cleared. That fact must be written into the log so the temporary relaxation cannot
quietly become permanent. See P9.

### 2.4 The upload defect is real, but neither the vector nor the fix was correct

The audit (§7.1.2) describes: rename `evil.html` to `report.pdf`, declared type passes the
allowlist, `confirmUpload` checks size only. Its containment analysis is correct — S3
signs the PUT content type, files are served from a different host, the app CSP cannot
frame them.

**That scenario does not execute.** `StorageClientImpl.inferContentType` (line 266) is a
closed allowlist: `mp4, m4v, webm, mov, jpg, jpeg, png, gif, webp, svg, pdf`. `.html`
returns `null`, no override is applied, S3 serves the stored `application/pdf`, and the
browser renders a broken PDF.

**The vector that does execute is `.svg`.** Verified chain:

- `StorageService.presignPut` (line 82) validates `contentType` and size against the
  purpose. **It never inspects `fileName`.**
- `sanitize` (line 396) is `fileName.replaceAll("[^a-zA-Z0-9._-]", "_")` — **dots survive**,
  so the attacker-chosen extension reaches the storage key (line 84).
- `StorageClientImpl.presignGet` (lines 89–96) calls `inferContentType(storageKey)` and
  sets `responseContentType` plus `responseContentDisposition("inline")`. **The presigned
  URL's override beats both the stored content type and the actual bytes.**
- `.svg` → `image/svg+xml` + `inline` → navigating to the URL renders SVG as a document,
  and SVG script executes.

Declare `image/png`, name the file `payload.svg`, upload SVG bytes of matching length.
Every check passes.

**Production is not exposed — confirmed 2026-08-10: `CLOUDFRONT_ENABLED` is `true` in
production.** `presignGet` returns at line 85 on the CloudFront branch, before the
content-type override is ever applied, so S3 serves the stored type and the SVG is inert.
What remains is a **config-dependent defect**: the code default is `CLOUDFRONT_ENABLED=false`,
so any environment that stands up without CloudFront — local, a new staging box, a
misconfigured redeploy — activates the vector silently. That is why it is still fixed in P8,
and why it is no longer urgent.

**Severity if it were exposed.** Script runs on the storage origin, not the app origin, so
Clerk tokens are not reachable. The app cannot iframe it (`frame-src` excludes S3), and
`<img>`-loaded SVG does not execute script. Uploads require an authenticated
`AUTH_ALLOWED_EMAIL_DOMAIN` user. **When CloudFront is enabled the vector disappears
entirely** — `presignGet` returns at line 85 before the override — but
`CLOUDFRONT_ENABLED` defaults to `false`. Realistic impact: phishing on a
corporate-looking storage URL, and the platform serving attacker-controlled active
content. Not session theft.

**Consequence.** The proposed fix — read leading bytes, compare to declared type — is
aimed at the wrong layer and would not catch SVG, which is text and has no magic bytes.
The fix belongs in `presignGet` and `presignPut`. See P8 step 4.

### 2.5 Numbers that were wrong in the v1 plan

| Claim | Measured 2026-08-10 |
|---|---|
| "16 `<Route>` declarations" | **11** `<Route`, 1 `<Routes`, **12** `path=` values |
| v1 P13 surface list included `features/permissions` | `features/permissions` has **zero** MUI/Emotion files. *(Historical — P13 is removed, §6)* |
| v1 P13 surface list | omitted `features/admin` (2), `shared/context` (1), `pages/NotFoundPage.tsx` (1), `app/AppRoot.tsx` (1) — **5 of 40 files unreachable**. *(Historical)* |
| "Align `vite` → `^5.4`" | current is **`^8.1.4`** — three majors. The audit records this (§R5); the plan lost it |
| `@EnableScheduling` listed as a P6 outcome | already present at `Application.java:12`. The audit knew; the plan lost it |
| "8 entity + 8 query regions" | plus **2 infrastructure regions**. With `missing_cache_strategy: fail`, dropping them fails startup |
| M3 "defeats the phase" | audit is correct and milder: risk is **architectural**, not runtime — `computeIfAbsent` caches nothing on a miss |
| P13 sized as "a bounded 40-file job" (v1 §2.2) / "a UI rewrite touching every screen" (audit R4) | Both size it by the wrong measure. **30 of the 40 files import icons only** and have no styling work; only **10** import components, and only **9 distinct components** are used, because `shared/ui` already ships the design system. The real work is **71 icons**, **153 `sx=` props** and **1 atomic theme teardown**. **No source document mentions the icons at all** — and removing `@mui/*` removes the icon set. Retained only as a record: the removal itself is **out of scope**, see §6 |

### 2.6 RESOLVED — the agent surface must be un-ignored (C5 is real)

The v1 plan instructed un-ignoring `.agents/`, `AGENTS.md`, `replit.md`. The project's
`.gitignore` excludes them and its header appears to justify that, so this was flagged as
an open question. **Answered 2026-08-10 against `cc64e49`.**

The standard's `templates/generated-project/scaffold/.gitignore` header states it outright:

> *"The active dual-agent runtime is part of the app: **AGENTS.md, replit.md, .agents/**,
> CLAUDE.md, and .claude/ **are committed** so both agents work after clone."*
> *"Both active agent surfaces are **deliberately NOT ignored**."*

The standard ignores only `templates/` and `custom_instruction/`. `check-agent-surfaces.sh`
asserts `AGENTS.md`, `replit.md` and `.agents` are all present or all absent.

**The project's copy has those two sentences deleted and the three paths added to the ignore
list.** This is the same self-ratifying pattern as `.claude/rules/12-database.md` scoped to
`backend/db/**` and the backend rule files edited to place metrics in `external-services`:
the local deviation was accompanied by an edit to the document that forbade it.

**Resolution.** Take `scaffold/.gitignore` as the shape. Concretely, in P1 step 0:

| Change | Status in the project today |
|---|---|
| Un-ignore `.agents/`, `AGENTS.md`, `replit.md` | **Required** — they are ignored |
| Restore the deleted header sentences | **Required** — removing them hid the divergence |
| Ignore `templates/`, `custom_instruction/` | Already correct — do not re-add |
| Add `.claude/tasks/*` + `!.claude/tasks/README.md` | **Required** — the standard has it, the project does not |

---

### 2.7 The standard hardcodes a changelog layout this project does not use

Found 2026-08-10 while checking what the gates require of the *present* usage-telemetry
module. Not in the audit, not in the v1 plan.

`structure-lint.sh` hardcodes these paths for the two optional modules:

```
db/changelog/db.changelog-master.xml          <- master changelog
db/changelog/changes/0001-usage-events.xml    <- required when event-logging is present
db/changelog/changes/0003-cache-invalidation.xml  <- required when cache-management is present
```

This project uses a different, versioned layout:

```
db/changelog/db.root-master.xml               <- include-only, ZERO changesets declared
db/changelog/1.0.0/db.version-master.xml      <- all 14 changesets, sqlFile form
```

**Two consequences, and they are not the same.**

**P6 is blocked as written.** `structure-lint` requires both
`changes/0003-cache-invalidation.xml` *and* that it be included from a file named
`db.changelog-master.xml`. Neither exists, so P6 cannot reach green.

*Fix, and it is safe:* rename the root changelog `db.root-master.xml` →
`db.changelog-master.xml`. **Zero changesets are declared in that file** — it holds a single
`<include>` — and Liquibase records the filename of the file where a changeset is *declared*,
which is `1.0.0/db.version-master.xml` and does not move. This is genuine convergence, not a
workaround. P5 does it, since P5 already owns this module. P6's new changelog is then written
at `changes/0003-cache-invalidation.xml` per the standard — it is new, nothing is recorded
against it, so no history is at risk.

**The telemetry assertion cannot be fixed honestly, and is carried.** `structure-lint`
requires `changes/0001-usage-events.xml` whenever `backend/event-logging-to-db-feature` is
present, and D-D keeps it present. The changeset `1.0.0-usage-events` is **already applied**
and recorded under `db/changelog/1.0.0/db.version-master.xml`. Declaring the same DDL again
at the path the gate wants would create a second, unknown changeset; `MARK_RAN` would absorb
it because the table exists, but `DATABASECHANGELOG` would gain a hollow 15th row purely to
turn a gate green. **That is gate-gaming (§3), and it is not done.** The assertion becomes
the fourth carried red failure and is filed upstream.

Everything else the gates ask of the present telemetry module already passes: parent POM
lists it, `service/pom.xml` attaches it, `UsageEventPersistenceService` carries
`@Async("usageLoggingExecutor")` + `@Transactional(REQUIRES_NEW)` (lines 28-29),
`UsageLoggingAspect` has no `@Transactional`, and `UsageEventEntity.attributes` is
`@JdbcTypeCode(SqlTypes.JSON) Map<String, Object>` (lines 77-78).

### 2.8 `.claude/**` must never be line-ending normalised

Found 2026-08-10 by breaking it. Recorded so the next person does not repeat it.

**The manifest covers more than `.claude/`.** `install-managed-claude-fixtures.py:22` also
owns four root-level files — `CLAUDE.md`, `AI-DEVELOPMENT-GUIDE.md`, `GDS-WORKFLOW-README.md`
and `agent-payload.skills` — checksummed by the same logic. An earlier revision of this
section said "every file under `.claude/`" and that was wrong; it cost a blocked P1 attempt on
2026-08-10, when P0's guardrails edit to `CLAUDE.md` broke its hash.

**Consequence, and it is permanent.** The standard instructs projects to adapt `CLAUDE.md`,
then hash-protects it. A project that follows that instruction can never re-run
`install-claude-fixtures.sh` — and P15 step 4 builds the template-upgrade path on that script.
**So `CLAUDE.md` is kept byte-identical to its manifest entry here.** Project rules live in
[`migration-guardrails.md`](./migration-guardrails.md), and every phase brief names it among
the things to read first. Filed upstream as CR-9.

Every one of those files is checksummed in `.claude/.aiae-fixtures-manifest`, and
`scripts/lib/install-managed-claude-fixtures.py:141` aborts the entire install with

> `managed fixture was edited locally; refusing overwrite: <path>`

on any hash mismatch. **There is no override flag** — it is a bare `SystemExit`. And the
message misdescribes the cause when git did the rewriting rather than a person.

**The manifest is mixed, and this is not a defect you can fix locally.** The standard
checkout on a Windows machine carries CRLF. `install-claude-fixtures.sh` copies most
fixtures verbatim — those arrive CRLF and are hashed CRLF — while the files it passes
through `rewrite-installed-documentation-paths.py` are written by Python and come out LF.
Measured here: **50 entries hashed as CRLF, 30 as LF**. No single line-ending policy
satisfies that, so git must apply none.

`.gitattributes` therefore reads:

```
* text=auto eol=lf
.claude/** -text
```

The default is deliberate — `scripts/*.sh` fail on Linux with `bad interpreter` under CRLF,
and P2 adds 50 more shell files there. The exemption is equally deliberate: `-text` stores
and restores `.claude/**` byte-for-byte as the installer wrote it.

**Do not "tidy" the exemption.** Normalising `.claude/**` in either direction breaks the
manifest and blocks `install-claude-fixtures.sh` permanently — which P1 step 1 depends on.
Anyone who runs `git add --renormalize .` across the whole tree, or drops the second line
while cleaning up, reproduces this.

## 3. Ground rules

**Evidence contract.** Every phase appends a row to `docs/aiae-migration-log.md` in the
**same commit** as the work, with all five fields. There is no CI to fall back on, so this
log is the only record that a phase actually ran: Build command and exit status; Test command with pass/fail
counts and coverage where touched; the named review skill and its verdict; the named
gate(s) with before → after counts; the revert command plus any out-of-repo action.

A phase whose gate count did not move in the predicted direction does not exit, even if
the build is green.

**The carried failures — there are four.** Three come from the same cause: this migration
converges the engineering contract and deliberately does not touch how the product looks or
navigates — the whole UI-rules gate and the `verify-gates.sh` sidebar assertion are described
in §6 *Not in scope*. The fourth is `structure-lint`'s
`changes/0001-usage-events.xml` assertion, carried because the only way to satisfy it is to
re-declare an applied changeset (§2.7). The first:

`verify-gates.sh` fails on exactly one assertion for the whole
migration: the two presigned direct-to-S3 `fetch()` call sites at
`frontend/src/features/library/api/useLessonMutations.ts:307` and
`frontend/src/features/library/api/useMaterialMutations.ts:171`. Reasoning is in audit
§6.2 and CR-1. **Every phase's evidence must show that count as exactly 1.** P2 makes this
mechanical — the assertion is `== 1`, so it fails at 0 (someone gamed it) and at 2 (a new
direct-fetch site appeared).

**Six things that must never happen.** Each is a real hazard found in this codebase, not a
generic warning.

1. **No re-materialization.** `materialize-project.sh` copies unconditionally; its own
   comment calls a second run "the worst shape of that failure." Install by explicit copy.
2. **No gate-gaming.** Renaming `Sidebar` to `NavRail`, or relocating a file out of the
   scanned tree to turn a count green, destroys the signal for everyone after. A
   documented red gate is worth more than a fake green one.
3. **No routing uploads through the backend.** Trades one gate failure for a
   `14-performance.md` violation and a real regression on large materials.
4. **Never rename `backend/migrations/src/main/resources/db/changelog/`.** See §2.1.
5. **Never add `@EnableCaching`** without `spring.cache.jcache.config` in the same change.
   See §2.2.
6. **Never refactor `backend/external-services/.../external/link/support/`** beyond the
   mechanical static-factory conversion. Its five classes are the SSRF guard.

**Agent suitability.** Not every phase should be delegated.

| Delegate freely | Delegate with review | Human only |
|---|---|---|
| P2, P4, P8, P10, P11, P12 | P1, P3, P9 | P0, P5 |

P0 is decisions and measurement, not code. P5 touches a live database.

---

## 4. Phases

### P0 — Baseline and decisions · *no code changes* · **human**

**Goal.** An honest starting line, measured against `cc64e49`, plus the one answer the rest
of the plan depends on (the R5 spike).

**Scope.** `docs/aiae-migration-log.md`, `docs/architecture-overview.md` (one paragraph),
`CLAUDE.md` (guardrails block).

**Steps.**

1. Create `migration` from `1.0.0`. `1.0.0` is not checked out again for the rest of the
   migration — there is no remote and no branch protection to lean on, so this is a rule
   rather than a setting.
1a. **Write `docs/migration-guardrails.md`** — the rules from §3 plus the environment traps.
   **Never put them in `CLAUDE.md`**: it is a manifest-managed fixture and editing it blocks
   `install-claude-fixtures.sh` permanently (§2.8). Every phase brief must name this file
   among the things the agent reads first. Done 2026-08-10.

   **Include the task-artifact override.** `.claude/tasks/README.md` documents the workflow
   artifacts as `review-report.md` and `test-report.md`; the `task-workflow` skill actually
   writes `review.md` and `verification.md`. Both files ship from the template, so this is an
   upstream defect (filed as CR-7), not ours to fix in place — and it **must not** be fixed in
   place: `.claude/tasks/README.md` is a managed fixture with a sha256 in
   `.claude/.aiae-fixtures-manifest`, and `install-managed-claude-fixtures.py:141` aborts with
   *"managed fixture was edited locally; refusing overwrite"* rather than proceeding. Editing
   it breaks P1 step 1 and every future fixture sync. State the correct names in `CLAUDE.md`
   instead: agents read it every turn, and it is outside the manifest.
1b. **Adopt a commit-message template for phase commits:** the five evidence-contract
   fields as trailer lines, plus the four carried assertion counts. With no pull requests
   and no CI, the commit message and the log are the entire audit trail — keep both in the
   same commit so they cannot drift apart.
1c. **Create `docs/aiae-migration-log.md`** — header, a link to this plan, and the evidence
   table with columns Phase / Build / Test / Review / Verification / Rollback. Step 3 below
   writes the first row.
2. Confirm the working tree state: `backend/db` no longer exists, `backend/migrations`
   is present, `artifactId` is `migrations`, the build is green. **The rename is already
   applied in the working tree.** Do not revert it — §2.1 removed the reason to.
   Verify only that `backend/migrations/pom.xml` still lacks Lombok (it does — it declares
   `liquibase-core` alone); P5 adds it.
3. Copy the gate suite from the standard into a scratch directory and run it against the
   tree unmodified. **First make `python3` resolve to a real Python** — see the environment
   section of the log. Ten checkers and `verify-gates.sh` invoke `python3` internally, so
   this cannot be fixed from the call site, and without it they exit 0 having read nothing.
   Pass explicit source roots to the production scanners, as `verify-gates.sh` does; bare
   invocation makes them scan the scaffold they live in, not this project. Record every count in `docs/aiae-migration-log.md` as the **tracking
   baseline**. Use an explicit interpreter for the Python gates — `python3` here resolves
   to the Windows Store stub and silently no-ops.
3a. **DONE 2026-08-10 — R1 settled.** The `databasechangelog` query in §2.1 was run against
   production and returned `db/changelog/1.0.0/db.version-master.xml`. §2.1 is confirmed and
   P5 is cleared. Copy the result into the log verbatim; the only thing left to note is the
   row count, which should read 14 and is a secondary check, not a gate.
4. Record the product baseline: `mvn -f backend/pom.xml -Phandoff verify` and
   `cd frontend && npm test`, with counts. Note that `-Phandoff` is inert (§2.3) and the
   effective floor is the hardcoded 0.80 LINE — **record that it currently passes**.
5. Send `docs/aiae-template-change-requests.md` to the template maintainer.
6. Confirm the standard checkout is reachable and at `cc64e49` — P1 and P2 copy from
   `templates/generated-project/scaffold/`. Verified present 2026-08-10.
7. **R5 spike, two variants.** On a throwaway branch, not on `migration`:
   - **A:** `vitest@^3.2.6` + `vite@^5.4` + `@vitejs/plugin-react@^4.3.4`
   - **B:** `vitest@^3.2.6` + `vite@^6` + matching plugin

   Run install, build, test on each. The gate enforces only the **vitest** pin; the audit
   itself records `vite ^5.4` / plugin `^4.3.4` as *drift, not gate-enforced* (§1 table).
   "Meeting `vitest ^3.2.6` in practice means Vite 5" is an assumption — vitest 3.2 also
   supports Vite 6. **If B passes and A fails, there is no deadlock**, only a narrower CR-6
   with evidence. Record both results.
**Build** `mvn -f backend/pom.xml clean install -DskipTests`
**Test** `mvn -f backend/pom.xml -Phandoff verify`; `cd frontend && npm test`
**Verification** Full gate suite run and archived; baseline row exists in the log.
**Rollback** n/a — no shipped code changed.

> **Exit** — baseline recorded against `cc64e49`; the standard checkout confirmed; both
> R5 variants answered; CRs sent.

---

### P1 — Agent surface · *no runtime risk*

**Preconditions.** P0 complete. §2.6 is resolved — no decision is pending.

**Goal.** Make the dual-agent runtime real and committable.

**Scope.** `.gitignore`, `.claude/**`, `AGENTS.md`, `replit.md`, `.agents/**`,
`llm-aux.lock`, `.template-version`, `.template-phase`, `CLAUDE.md`.

**Do not touch.** `backend/**`, `frontend/**`, `.github/**`, `scripts/**`.

**Steps.**

0. Fix `.gitignore` **first**, per the §2.6 table — before creating any of the files below,
   or the tree looks converged while the repository is not. Four changes: un-ignore
   `.agents/`, `AGENTS.md`, `replit.md`; restore the two deleted header sentences; add
   `.claude/tasks/*` + `!.claude/tasks/README.md`. Leave `templates/` and
   `custom_instruction/` alone — already correct.
0a. **Validate the fixture manifest before touching anything.** For every
   `path<TAB>sha256` line in `.claude/.aiae-fixtures-manifest`, hash the file on disk and
   compare. **80 of 80 must match.** If any do not, stop — step 1 will abort with
   *"managed fixture was edited locally"*, and that message will point you at the wrong
   cause. The usual real cause is line-ending normalisation; see §2.8 before doing anything
   else, and check `git check-attr text eol -- .claude/agent_docs/agent-operating-model.md`
   reports `-text`.
1. Re-run `bash scripts/install-claude-fixtures.sh <project>` from the standard checkout
   to bring the installed fixtures to `cc64e49` and let it rewrite doc citations.
   **It overwrites `CLAUDE.md`** — re-apply the project-specific paragraphs afterwards.
   Do not reintroduce the C2 divergence: the rule text must say metrics belong in
   `backend/observability`.
2. Add `AGENTS.md` and `replit.md` from `scaffold/AGENTS.md.template` /
   `replit.md.template`.
3. Mirror `.agents/skills/`; add `llm-aux.lock` (`690a9748…`), `.template-version` =
   `cc64e49`, `.template-phase` = `mvp`.
4. Confirm what is already correct and must survive step 1: `.claude/rules/README.md` is
   absent; `aiae-rule-compliance-audit` is present and `rule-compliance-audit` is not;
   `.claude/agent_docs/` carries 29 files; `.aiae-fixtures-manifest` is present.
5. **Leave `CLAUDE.md` alone.** Project rules live in `docs/migration-guardrails.md` (§2.8).
   Nothing is re-applied here, and the file must still match its manifest hash when this
   phase ends.

**Build** n/a **Test** n/a
**Review** `aiae-rule-compliance-audit`
**Verification** `check-agent-surfaces.sh` = 0; `check-installed-documentation-links.py`
6 → 0; `git ls-files` reflects the P0 step 6 decision.
**Rollback** `git revert`.

> **Exit** — both gates zero, and the agent surface is *tracked* exactly as decided, not
> merely present.

---

### P2 — Harness and the local runner · *measure, do not fix*

**Goal.** Every gate runs locally and publishes a number. Nothing blocks yet.

**Scope.** `scripts/lib/**` (new), `scripts/*` (additions only),
`.github/workflows/ci.yml`, `backend/config/checkstyle-test-fields.xml`.

**Do not touch.** Nine project-specific scripts (audit §7) — the AIAE scripts are added
**alongside** them, never over them:
`backend-jacoco-coverage-targets.sh`, `local-dev-backend.sh`, `local-dev-frontend.sh`,
`replit-build.sh`, `replit-dev-backend.sh`, `replit-dev-frontend.sh`, `replit-env.sh`,
`replit-run.sh`, `report-bundle-size.sh`.

The tenth, `local-verify.sh`, **is** replaced — deliberately, in step 5. It is the only one.

**Steps.**

1. Copy the 22 runtime scripts from `scaffold/scripts/` and all 28 checkers from
   `scaffold/scripts/lib/`. `scripts/lib/` does not exist today; create it.
2. Replace `.github/workflows/ci.yml` with the standard's 5-job workflow
   (`static-checks`, `unit-tests`, `integration-tests`, `frontend-checks`,
   `local-dev-dry-run`), with **`static-checks` marked `continue-on-error: true`**.
   **Nothing executes this file** — no CI service runs in this migration. It is installed
   because the standard requires it and a future remote will use it. Verify it by
   inspection against `templates/generated-project/.github/workflows/ci.yml`; the checks it
   would run are run locally in step 5. P15 flips the flag, equally unobserved.
3. Add `backend/config/checkstyle-test-fields.xml`. The directory currently holds
   `checkstyle.xml` and `checkstyle-suppressions.xml` only.
4. **Make the carried failure mechanical.** Assert the presigned-upload count is
   **exactly 1**, not `>= 1`. It must fail at 0 and at 2. See §3.
5. **Replace `local-verify.sh` with the AIAE one here, not in P15.** With no CI, this
   script is the only thing that ever runs the gates — it is the enforcement mechanism, not
   a convenience, and leaving it until P15 would mean thirteen phases with nothing to
   enforce them. Install it in **report-only** mode: it prints every count and exits 0, so
   the numbers are visible without blocking on day one. P15 makes it blocking. Leave
   `local-dev-*.sh` and `replit-dev-*.sh` untouched.

**Build** unchanged **Test** unchanged
**Review** `production-code-review` on the harness diff
**Verification** `bash scripts/local-verify.sh` runs all 28 checkers and prints a count for
each; the numbers match P0's local baseline within noise; the presigned assertion reports
exactly 1; `ci.yml` matches the standard's file by inspection.
**Rollback** `git revert` restores the 2-job workflow and the project's `local-verify.sh`.

> **Exit** — one local command runs every gate and publishes a count, and the log records
> all 28 numbers as the working baseline.

---

### P3 — Documentation, Replit contract, Context7 · *deploy contract, not verified locally*

Not "no runtime risk" — this phase changes how Replit boots the app.

**Scope.** `docs/architecture-overview.md`, `backend/DEPENDENCY-ANALYSIS.md`, `README.md`,
`docker-compose.yaml` → `docker-compose.yml`, `.replit`.

**Do not touch.** `scripts/replit-dev-*.sh` — kept for local use. And never overwrite
`docs/architecture-overview.md` with a template copy — step 1 is additive.

**Steps.**

1. **`docs/architecture-overview.md` — ADD ONLY. Do not regenerate this file.**

   It already exists, 78 lines, and carries two sections that no template ships and that
   must survive verbatim in structure:

   - **`## Adopted standards the code has not caught up to yet`** — the standing record of
     where the implementation lags the contract, written to keep the gap visible rather
     than mistaken for compliance.
   - **`## Known upstream defect`** — the `.claude/tasks/README.md` vs `task-workflow`
     artifact-name mismatch.

   Also already present and correct: `## Identity`, `## Backend modules`,
   `## Deployment / runtime constraints`. Read the file before writing to it. Taking the
   template's copy and overwriting is a phase failure, not a shortcut.

   **What to add**, verified against the implemented repository rather than the scaffold:
   the five outbound integrations; the Clerk-only auth model with
   `AUTH_ALLOWED_EMAIL_DOMAIN` and `PermissionEvaluator`; the L2 cache inventory (8 entity
   + 8 query + 2 infrastructure regions, §2.2); the two scheduled jobs with their intervals;
   the presigned upload path; **the MVP usage-telemetry design** — the handoff script
   validates that this document names `backend/event-logging-to-db-feature` explicitly, and
   D-D keeps the module, so this paragraph is load-bearing; and **the required S3 bucket
   CORS policy with exact origins per environment** (audit §7.1.1).

   That CORS block is the highest-value paragraph in this phase: the configuration lives in
   AWS, nothing in this repository can see it, and it breaks uploads with an error the
   browser deliberately makes unreadable.

1a. **Make the lag section a living list.** Every entry in
   `## Adopted standards the code has not caught up to yet` is something this migration
   either closes or deliberately declines:

   | Entry | Closed by |
   |---|---|
   | Observability module | P4 |
   | Distributed cache | P6 |
   | Coverage phase tooling | P2 and P9 |
   | Import ordering | P12 |
   | CSS tokens and units | **Not closed — declined, §6** |

   Each of those phases strikes its own entry when it lands, and the CSS entry is rewritten
   now from "the code has not caught up" to a recorded deliberate divergence with its
   reason. By P15 the section should contain only divergences, not debt — which makes it a
   second, human-readable progress signal beside the gate counts.
2. **`backend/DEPENDENCY-ANALYSIS.md`** — does not exist; create from the scaffold, filled
   in for this project's actual dependency set.
3. **README** — add the API / Swagger UI / OpenAPI YAML link block the gate requires. The
   README mentions OpenAPI but carries no link block. Leave the rest; the audit rates the
   content as excellent.
4. **`docker-compose.yaml` → `docker-compose.yml`** — `local-verify.sh` and the
   `local-dev-dry-run` job both look for `.yml`.
5. **`.replit` (C7)** — adopt the scaffold's inline workflow form so the literal contract
   strings live in `.replit` rather than inside shell scripts: `source scripts/replit-env.sh`,
   `npm run generate:api` before Vite, `backend/application/pom.xml`, and the
   `onBoot = "bash scripts/setup-project.sh"` hook (absent today).
   **`VITE_CLERK_JWT_TEMPLATE = "aidigital-api"` is already present in `[env]` and must
   survive** — the scaffold does not have it and losing it breaks Clerk token exchange.
   Also preserve: `modules`, `deploymentTarget = "gce"`, port 5000 → 80,
   `SPRING_PROFILES_ACTIVE = "replit"`.
6. **Context7** — `.mcp.json` is present and points at the HTTP OAuth endpoint, so there
   is no key to manage. Verify the server resolves in-session. Never a key in chat.

**Build** n/a **Test** `docker compose -f docker-compose.yml config` parses
**Review** `production-code-review` on `.replit` and the docs diff
**Verification** `check-architecture-overview.sh` 1 → 0;
`check-maven-dependency-analysis.py` 1 → 0; `verify_replit_file()` assertions pass.
**No deploy is performed.** `.replit` is verified by inspection — the literal contract
strings are present, `VITE_CLERK_JWT_TEMPLATE = "aidigital-api"` survives in `[env]`, and
`onBoot` is set — plus `bash scripts/local-verify.sh` and a local
`scripts/local-dev-backend.sh` + `local-dev-frontend.sh` run that serves the SPA. Whether
the Replit deploy actually boots from the new contract is **unverified by this migration**
and must be checked by whoever deploys next; record that explicitly in the log.
**Rollback** `git revert`.

> **Exit** — three gates zero, `.replit` matches the standard's contract by inspection with
> `VITE_CLERK_JWT_TEMPLATE` intact, the app serves locally, Context7 verified reachable,
> and the log states that the deploy path is untested.

---

### P4 — `backend/observability` · *low risk, first module move*

**Goal.** Close C2 and prove the module-adding procedure on a safe change before P5.

**Scope.** `backend/observability/**` (new), `backend/pom.xml`,
`backend/external-services/**`, `backend/application/pom.xml`,
`.claude/rules/00-backend-hard-rules.md`, `.claude/rules/10-architecture.md`.

**Do not touch.** These stay in `application` — only two reusable classes move:
`PerformanceMetricsFilter`, `RequestAuthenticationCacheFilter`, `IntegrationHealthIndicator`,
`ByteCountingResponseWrapper`, `CountingServletOutputStream`, `MetadataOnlyHttpLogFormatter`.

**Steps.**

1. Create `backend/observability` with Lombok declared; add to the parent POM module list
   (currently `domain, migrations, event-logging-to-db-feature, service, application,
   external-services`).
2. Move `ExternalCallTimer` and `ExternalClientMetricsInterceptor` from
   `backend/external-services/src/main/java/com/aidigital/aionboarding/external/common/http/`
   to `…observability.external`.
3. Add the dependency from `external-services` and `application`.
4. **`PooledRestClientFactory` must keep both registration expressions byte-identical.**
   The gate greps the literal strings `new ExternalClientMetricsInterceptor(name, meterRegistry)`
   and `new LogbookClientHttpRequestInterceptor(logbook)`. They occur **four times, in two
   factory methods** — lines 68–69 and 113–114. Only the imports change.
5. Revert the rule-file divergence: `00-backend-hard-rules.md` and `10-architecture.md`
   were edited to say both classes live in `external-services`. They must say
   `backend/observability` (audit §C2).

**Build** `mvn -f backend/pom.xml clean install`
**Test** Full backend suite; assert outbound metrics still appear on
`/actuator/prometheus` after one real external call.
**Review** `backend-rule-review`
**Verification** structure-lint's 4 observability assertions pass; metric names identical
before → after.
**Rollback** `git revert`.

> **Exit** — 4 structure-lint assertions cleared, metrics still emitted under the same names.

---

### P5 — `backend/migrations` completion · *data risk, disproved but verified* · **human**

**Preconditions.** P4 merged. P0 step 3a is **already satisfied**: the production
`databasechangelog` query returned `db/changelog/1.0.0/db.version-master.xml` on 2026-08-10,
so §2.1 holds and this phase is cleared.

**Nothing is deployed here**, and no database restore is required (§6.1 D-B). The phase
produces a commit on `migration` like any other; what made it look dangerous — R1 — was
disproved in §2.1 and confirmed by query in P0.

**Goal.** Finish C1 correctly. **Scope reduced from v1** — §2.1 removed the R1 mitigation,
because the risk does not exist.

**Scope.** `backend/migrations/pom.xml`, `.claude/rules/12-database.md`,
`backend/migrations/src/main/resources/db/changelog/` (comment only).

**Do not touch.** The 14 changesets, their ids, authors, and `sqlFile` paths. The
`1.0.0/` folder name and everything inside it. **The `db/changelog/` directory name — see
step 4.** Step 6 renames one file *inside* that directory; nothing else moves.

**Steps.**

1. **Add the Lombok dependency** to `backend/migrations/pom.xml`. It currently declares
   `liquibase-core` alone, which violates the hard rule that every backend submodule
   declares Lombok. This is the one genuine defect left from the rename.
2. Fix the stale POM header comment: it reads *"`application` depends on `db`"* and the
   module is now `migrations`.
3. Restore `.claude/rules/12-database.md` to the AIAE text — the local edit scoped it to
   `backend/db/**`, which made the divergence self-ratifying.
4. **Add a warning comment** at the top of `db/changelog/db.root-master.xml`:

   > Liquibase records applied changesets by *(id, author, filename)*, where filename is
   > the **classpath** path `db/changelog/…` — see `spring.liquibase.change-log`.
   > Renaming this directory changes that path and will re-run all 14 changesets against
   > a populated schema. The Maven module name is *not* part of this path and may change
   > freely. Do not "tidy" `db/` to match the module name.

5. **Rename the root changelog** `db/changelog/db.root-master.xml` →
   `db/changelog/db.changelog-master.xml`, and update `spring.liquibase.change-log` in
   `backend/application/src/main/resources/application.yml:75` to match. Grep for
   `db.root-master` across the repository first and update every reference.

   **This is safe, and §2.7 explains why:** that file declares **zero** changesets — it
   contains one `<include>` — and Liquibase records the filename of the file where a
   changeset is *declared*. All 14 are declared in `1.0.0/db.version-master.xml`, which does
   not move. Nothing in `DATABASECHANGELOG` changes.

   It is done here because P6 needs it: `structure-lint` requires the cache-invalidation
   changelog to be included from a file with this exact name, and P6 cannot pass without it.
6. **Verify against the P0 step 3a result, not a restore.** The 14 recorded filenames must
   be byte-identical to what the renamed module produces on the classpath — compare them to
   `db/changelog/1.0.0/db.version-master.xml` explicitly and record the comparison. If a
   restored copy is available anyway, booting against it and asserting **14 rows** unchanged
   is a welcome second-order check, but it is not a precondition for merging.
   This is now a confirmation of §2.1 rather than a mitigation test, but it still runs —
   it is the only empirical check on a live schema.
6. Watch `1.0.0-jsonb-array-contains-ci-function` specifically: it uses an `sqlCheck`
   precondition rather than table-existence, so it is the least protected by `MARK_RAN`.
   It is also the only changeset carrying an explicit `<rollback>`.

**Build** `mvn -f backend/pom.xml clean install`
**Test** Full suite plus Testcontainers integration tests on an empty schema (proves a
green-field apply still works).
**Review** `backend-rule-review` + a second human reviewer on the changelog diff.
**Verification** `check-liquibase-preconditions` stays 0 — all 14 already comply, preserve
this; the 14 production filenames from P0 step 3a match the post-rename classpath path
exactly; structure-lint's
3 `migrations` assertions pass.
**Rollback** `git revert`. No out-of-repo action is needed while nothing is deployed — but
whoever eventually deploys this must take a `DATABASECHANGELOG` backup first, and that
requirement belongs in the log so it survives to release day.

> **Exit** — the 14 recorded production filenames are unaffected by the rename, Liquibase
> applies cleanly to an empty schema from scratch, and Lombok is declared.

---

### P6 — `cache-management` and the second cache · *D2, M1, M3*

**Preconditions.** `.claude/agent_docs/distributed_cache.md` installed in P1 — it is the
protocol being adopted, so P6 cannot precede it.

**Scope.** `backend/cache-management/**` (new), `backend/service/**`, `backend/pom.xml`,
`backend/application/src/main/resources/application.yml`, the `0003-cache-invalidation`
changelog.

**Do not touch.** `ehcache.xml` heap bounds. `Application.java` — **`@EnableScheduling` is
already present at line 12**; do not add it, do not duplicate it.

**Steps.**

1. Install `backend/cache-management` with POM wiring (`service` depends on it, Lombok
   declared). **The changelog path is fixed by the gate, not by preference** (§2.7): write it
   at `backend/migrations/src/main/resources/db/changelog/changes/0003-cache-invalidation.xml`
   and include it from `db.changelog-master.xml` — the file P5 step 6 renamed. This changelog
   is new, so no recorded history is at risk and the standard's layout can be followed
   exactly.

   `structure-lint` checks six things once this module exists, all verified in the standard:
   the module `pom.xml`; `<module>cache-management</module>` in the parent POM;
   `<artifactId>cache-management</artifactId>` in `service/pom.xml`; `@EnableScheduling`
   somewhere under `backend/application` (already true — `Application.java:12`);
   `JpaCacheInvalidationEventService.java` and `ApplicationCacheNamesByClassRegistry.java`
   under `backend/service`; and the changelog file plus its include. It also **fails** on
   `updatesAfter(LocalDateTime` anywhere in the module — poll by monotonic event ID, never by
   timestamp — and on `parallelStream()` under `backend/application/*/cache`.
2. Add `JpaCacheInvalidationEventService` and an **initially empty**
   `ApplicationCacheNamesByClassRegistry`. Like P7 this is preparation for the planned
   multi-node deployment (§6.1 D-E), not a fix for a live defect. Empty is deliberate: all 8 cached sources are
   `READ_ONLY` `@Immutable` dictionary entities changed only by Liquibase at deploy time,
   so registering them would add publish calls no transaction ever makes. The registry
   becomes load-bearing the moment a mutable source is cached.
3. **Cache manager — read §2.2 before touching this.** There is no `CacheConfig.java` and
   no `@EnableCaching`. Today exactly one `javax.cache.CacheManager` exists. **Do not add
   `@EnableCaching`.** If this phase introduces any Spring-side cache usage, it must set
   `spring.cache.jcache.config: ehcache.xml` in the same change, or a second empty manager
   appears beside Hibernate's because `spring.cache.type: jcache` is already declared at
   `application.yml:25`.
4. **Preserve all 18 regions** in `ehcache.xml`: 8 entity, 8 query, and **2 infrastructure**
   (`hibernate-cache.default-query-results-region`,
   `hibernate-cache.default-update-timestamps-region`). With
   `missing_cache_strategy: fail`, dropping either infrastructure region fails startup.
5. Warm-up stays **disabled**. Never with an unbounded `parallelStream()` — structure-lint
   rejects that shape by name. There is no warm-up code today.
6. **M3** — fold `DictionaryLookupService`'s bean-level `ConcurrentHashMap`
   (`DictionaryLookupService.java:38`) into the managed cache. Stated accurately, per audit
   §M3: **runtime risk today is genuinely low** — the rows are Liquibase-seeded, the ids are
   immutable, and `computeIfAbsent` caches nothing on a miss, so the map cannot go stale or
   grow unbounded. The problem is architectural: it duplicates what the 8 entity regions and
   8 `findXByCode` query regions already cache, and it sits outside the invalidation
   protocol being adopted here.

**Build** `mvn -f backend/pom.xml clean install`
**Test** The standard's cache verification list in full: registry verification resolves
every configured region; a mutation commit creates exactly one event and a rollback none;
publication outside a transaction fails; polling processes increasing ids and retries a
failed eviction; Spring and Hibernate receive the same manager instance. Plus an M3 test
asserting no second cache instance is reachable from `DictionaryLookupService`.
**Review** `backend-rule-review`
**Verification** structure-lint's cache-pairing assertion passes; `@EnableScheduling`
present (already true); 18 regions present; cache hit ratios on `/actuator/prometheus`
unchanged before → after.
**Rollback** `git revert`; the `0003` changelog needs its rollback exercised in rehearsal.

> **Exit** — the full cache verification list passes, exactly one cache manager exists in
> the context, and all 18 regions are declared.

---

### P7 — Coordinate node-affine work · *M2, M4*

**Goal.** Make scheduled and on-read work safe on N nodes.

**Scope.** `backend/application/.../jobs/MaterialYoutubeBackfillJob.java`,
`.../jobs/AbandonedUploadCleanupJob.java`,
`backend/service/.../teachervideo/services/TeacherVideoRefreshService.java`, and the
repositories they claim through.

**This is preparation, not remediation.** The deployment runs a single node today, so
nothing is currently duplicated and no quota is currently being burned twice. The work
exists so that adding a second node is a deployment decision rather than a correctness
event — decided 2026-08-10, multi-node is planned but not yet live (§6.1 D-E).

**Verified today:** `MaterialYoutubeBackfillJob` is `@Scheduled(fixedDelay = 300_000)` —
five minutes. `AbandonedUploadCleanupJob` is `fixedDelay = 900_000` — fifteen minutes.
**There are zero `SELECT … FOR UPDATE SKIP LOCKED` and zero advisory locks in the
repository.** No coordination exists, and none is needed until the second node appears.

**Steps.**

1. `MaterialYoutubeBackfillJob` — the priority, because it is the one whose failure mode
   costs money rather than cycles: on N nodes it would burn a hard daily YouTube quota N
   times over, every five minutes. Claim batches with `SELECT … FOR UPDATE SKIP LOCKED`.
   Preferred over an advisory lock because both jobs are bounded batch sweeps, and claiming
   parallelises — two nodes take disjoint batches — where a lock wastes the other nodes' tick.
2. `AbandonedUploadCleanupJob` — same treatment. Duplicated S3 deletes are idempotent but
   wasteful.
3. `TeacherVideoRefreshService` — different shape: read-triggered, so two nodes serving the
   same lesson both poll HeyGen. `Lesson.@Version` protects correctness but the loser sees a
   409 for something they did not do. Add a conditional claim on the lesson's refresh
   timestamp so only the winner polls.

**Build** standard
**Test** A job invoked concurrently from two threads processes disjoint batches; two
concurrent teacher-video refreshes produce exactly one HeyGen call.
**Review** `backend-rule-review`
**Verification** Both concurrency tests pass; no transaction is held open across the HeyGen
or YouTube call (`14-performance.md`).
**Rollback** `git revert`.

> **Exit** — both concurrency tests green, and YouTube call volume is provably independent
> of node count *before* a second node exists.

---

### P8 — Boundary splits and the upload content check · *C8*

**Scope.** `backend/application/.../GlobalExceptionHandler.java` and a new
`error/mapper/` package; `backend/service/.../common/time/CurrentTime.java`;
`backend/service/.../material/services/UploadValidator.java`;
`backend/service/.../storage/StorageService.java`;
`backend/external-services/.../storage/impl/StorageClientImpl.java`.

**Steps.**

1. Split `GlobalExceptionHandler` (it holds **12 private members** today) into the handler
   plus `error/mapper/GlobalExceptionResponseHelper` and its `Impl`, so the handler holds
   no private helpers. The scaffold files are drop-in references.
2. Split `service/common/time/CurrentTime` — currently a single file with no `Impl` — into
   an interface plus `CurrentTimeImpl` that owns the `now()` calls. That clears the two
   violations at `CurrentTime.java:21` and `:30`.
2a. **Third violation, in a file the v1 plan never mentioned.**
   `external/storage/impl/CloudFrontUrlSigner.java:56` calls
   `Instant.now().plus(expiresIn)` directly. Inject `CurrentTime` and use it. Measured
   2026-08-10: the gate reports **3**, not the 2 both source documents claimed.
3. Move the `MultipartFile` import out of `UploadValidator` — the only place a service
   imports a web API.
4. **Upload content verification — read §2.4 first. The v1 instruction was wrong, and this
   is not urgent.** Production runs with CloudFront enabled, which bypasses the vulnerable
   branch entirely, so nothing live is exposed. What is being closed is a config-dependent
   defect: the code default is `CLOUDFRONT_ENABLED=false`, and any environment without
   CloudFront activates it.

   Do not implement "read leading bytes and compare to the declared type": SVG is text and
   has no magic bytes, so that check misses the only vector that executes. Fix at the two
   places that actually decide:

   - **`StorageClientImpl.presignGet`** (lines 89–96): stop deriving the response content
     type from the storage key's extension. `inferContentType(storageKey)` lets an
     attacker-chosen `.svg` filename override the stored type and be served
     `image/svg+xml` + `inline`. Serve the **stored** content type instead, or the
     `expectedContentType` recorded on the `PendingUpload`.
   - **`StorageService.presignPut`** (line 82): `validateUploadPolicy` checks the declared
     content type and size and **never inspects `fileName`**, while `sanitize` (line 396)
     preserves dots so the extension survives into the key. Validate that the extension is
     consistent with the declared type, or normalise it away.

   While here: `PendingUpload.expectedContentType` is written at presign (line 92) and
   **read nowhere**. Either wire it into `confirmUpload` — the `headObject` metadata is
   already in hand at line 136 — or remove it.

   Note for the log: production is already safe via CloudFront; this change makes the code
   safe regardless of that setting.

**Build** standard
**Test** Existing suite, plus a negative test per split, plus a test that a `.svg`-named
upload declared as `image/png` is either rejected at presign or not served as
`image/svg+xml`.
**Review** `backend-rule-review`
**Verification** `check-production-current-time.sh` **3** → 0 (measured, see log); the handler contains no private
methods; the SVG test fails before the change and passes after.
**Rollback** `git revert`.

> **Exit** — the current-time gate is zero, and an attacker-chosen file extension can no
> longer dictate the served content type.

---

### P9 — Coverage gate · *CI risk · one commit*

**Goal.** Adopt the AIAE phased coverage model. **Read §2.3 first — the current gate is
live, not dead.**

**Scope.** `backend/pom.xml`, `.template-phase`, `.github/workflows/ci.yml`.

**Steps.**

1. **Measure first**, with the seven hand-written excludes removed and the check skipped.
   `**/models/**`, `**/entities/**`, `**/repositories/**`, `**/config/**`,
   `**/*Entity.class`, `**/*Exception.class`, `**/*_.class` have never been counted.
   Record line and branch coverage against the new denominator. **Blocked until Maven is
   available** — it is not installed on the machine that produced the P0 baseline, so this
   measurement is still outstanding and P9 cannot start without it.
2. **Record in the log, explicitly:** the effective floor today is a hardcoded
   `0.80` LINE (`pom.xml:308`), CI runs it, and it passes. Moving to `mvp` (0.30/0.25)
   **lowers a floor the project currently clears**, deliberately and temporarily — see
   §6.1 D-C. Write down both numbers: the measured coverage against the new wide
   denominator, and the gap to 0.80/0.70. That gap is the size of the P15 job, and knowing
   it here is what stops P15 from being a surprise.
3. Move `0.80` / `0.70` into the parent `<properties>`; wire **both** limits to those
   properties; **add the missing BRANCH limit**. These are the strict values and they apply
   to every module by default — see §6.1 D-C.
4. Delete the seven hand-written excludes, keeping only the three `**/api/v1/**` generated
   ones. Delete from **both** the `report` and `check` blocks — the duplication is why the
   gate reports more problems than there are excludes.
5. **Override the floor in exactly two places, not globally.** `-Phandoff` is currently a
   no-op — it sets two properties nothing reads — so replacing it wholesale is not needed.
   Instead, in `backend/domain/pom.xml` and `backend/external-services/pom.xml`, override
   `jacoco.branch.coverage` (and for `domain`, `jacoco.line.coverage`) to the measured
   current values, with a comment naming this decision and the date. Every other module runs
   strict from this commit.

   Measured starting points, for the comments: `domain` 0.0236 / 0.0714;
   `external-services` branch 0.6978. Set the floors at those values, so the modules cannot
   regress further while their gap is being closed.
6. **Same commit**: commit `.template-phase` (`check-coverage-integrity.sh` requires the
   file to exist and hold a known value) and drop `-Phandoff` from the CI invocation, so the
   new floors apply the moment the properties become live. Splitting these leaves the build
   red between commits.

**Build** `mvn -f backend/pom.xml clean verify` with no flags
**Test** Full suite; record real line and branch coverage against the new denominator.
**Review** `production-code-review` on the POM diff
**Verification** `check-coverage-integrity.sh` → 0; a bare `mvn verify` is strict and green
under `-Pmvp`; the log carries the step-2 note.
**Rollback** `git revert` restores `handoff`.

> **Exit** — coverage integrity zero; three of five modules enforced at the full 0.80/0.70
> from this commit; `domain` and `external-services` pinned at their measured values so they
> cannot regress; and the log names the two overrides, their reason and their removal
> condition.

---

### P10 — API validation · *tests before constraints* · R3

**Goal.** Close the largest hole in the suite, then make the contract honest — in that order.

**Verified today:** `isBadRequest` appears **zero times** in the entire backend.

**Steps.**

1. **First**, write negative MVC tests. 91 constrained operations currently have no
   `isBadRequest()` assertion anywhere. Without them, nothing catches a constraint that is
   too tight — including one that rejects requests the existing frontend sends today.
2. **Then** add the 82 OpenAPI input constraints. Derive every bound from the actual column
   width in `backend/migrations/src/main/resources/db/changelog/1.0.0/sql/*.sql` rather than
   guessing. Where a field genuinely has no bound, use `x-unconstrained-reason` — the gate
   accepts it and it is the honest answer.
3. Add the 4 missing schema descriptions.
4. Regenerate both sides through `openapi-contract-first`; never hand-edit generated sources.
5. Bring test style to `20-tests.md` — private fields, package-private classes, targeted
   Instancio `.set(field(...))` — now enforced by the P2 Checkstyle config.

**Build** standard, including OpenAPI regeneration
**Test** `mvn verify`; `cd frontend && npm run check:api`
**Review** `backend-rule-review` + `openapi-contract-first`
**Verification** `check-api-validation-tests.py` 91 → 0;
`check-openapi-input-constraints.py` 82 → 0; `check-openapi-documentation.sh` 4 → 0;
`check-api-client-paths.sh` stays 0.
**Rollback** `git revert` — but a constraint that reached production and started rejecting
real traffic needs a forward fix, which is why the negative tests come first.

> **Exit** — three gates zero, and every existing frontend request still succeeds.

---

### P11 — Thin controllers and service shape · *regression risk · one aggregate per commit*

**Preconditions.** P10 merged — the negative tests are this phase's safety net.

**Verified today:** 15 controllers exist; the gate reports 55 violations across 12 of them.
`LearningServiceImpl` is **357 lines, 13 injected fields, 16 public members**.
`LessonServiceImpl` is **332 lines, 11 injected fields**. `LogbookConfig.resolveFormatter`
is at line 126 and `resolveStrategy` at line 138. `external/link/support/` holds exactly
**5** production classes.

**Do not touch.** `backend/external-services/.../external/link/support/` beyond the
mechanical static-factory conversion — `HostnameResolver`, `LinkFetchBlockedException`,
`LinkUrlPolicy`, `OutboundAddressValidator`, `PinnedDnsResolver` are the SSRF guard.

**Steps.**

1. **Controllers first** — mechanical. Push branching, ternaries and stream transforms down
   into services or API mappers.
2. **Then services** — split the 5 oversized and 9 over-injected `ServiceImpl`s into
   collaborators (validator / policy / assembler). `LearningServiceImpl` and
   `LessonServiceImpl` are the two that are easy to get subtly wrong. **One aggregate per
   commit**, so any one of them can be reverted alone.
3. Stop the 10 application mappers hand-constructing `new *V1(...)`.
4. Convert the 8 static factories — 3 enum `fromValue` (`LessonAssistantPreset:32`,
   `LessonStatusAction:35`, `QuizQuestionType:35`) and 5 in `external/link/model`.
5. Resolve the Logbook sink form (C9): either match the gate's literal expressions, or keep
   the configurable `resolveFormatter()` / `resolveStrategy()` design — which is genuinely
   *stricter* than the scaffold, and is already covered by tests — and file it as a template
   exception alongside CR-1.

**Build** standard **Test** Full suite per aggregate; no coverage regression
**Review** `backend-rule-review` per aggregate, `production-code-review` on the service
splits
**Verification** `check-thin-controllers.py` 55 → 0;
`check-service-contract-quality.py` 25 → 0; `check-production-static-methods` 8 → 0;
`check-production-manual-mapping.sh` stays 0; structure-lint's 10 mapper findings → 0.
**Rollback** per-commit `git revert`.

> **Exit** — `structure-lint.sh` reports zero and every backend `lib/` checker is zero.

---

### P12 — Frontend tooling and dependency cleanup · *risk retired 2026-08-10*

The v1 plan labelled this "low risk" while asking for a three-major-version downgrade, and
audit §R5 rated it a **compatibility risk** and possible *"third deadlock"*. **The spike
settled it: the downgrade works.** Install, build and the full test suite are green on
`vite 5.4.21` / `vitest 3.2.7`. What remains is ordinary work.

**Measured today:**

| Package | Current | Standard pins | Gate-enforced? |
|---|---|---|---|
| `vitest` | `^4.1.10` | `^3.2.6` | **Yes** — `verify-gates.sh` fails on any other value |
| `vite` | `^8.1.4` | `^5.4` | No — drift only |
| `@vitejs/plugin-react` | `^5.2.0` | `^4.3.4` | No — drift only |

**Steps.**

1. **ANSWERED 2026-08-10 — take the standard's pins as written.** The spike ran variant A
   (`vitest@^3.2.6` + `vite@^5.4` + `@vitejs/plugin-react@^4.3.4`) against a scratch copy of
   the frontend: install clean, `npm run build` green in 10.2s, **78 tests passing**. Tiptap
   3.22, `@base-ui/react` and `@tsparticles/confetti` all survive Vite 5. There is no
   deadlock and **CR-6 is withdrawn**. Variant B was the fallback and was not needed.
   Run `npm run generate:api` before `npm run build` — the generated schema is gitignored,
   and without it the build fails with `TS2307` plus a cascade of `TS7006` that looks like a
   toolchain problem and is not.
2. ESLint flat config, the `eslint-rules/import-section-order.mjs` rule, a `lint` npm
   script, `scripts/prepare-husky.mjs`, `.husky/pre-commit`. **There is no linting in this
   project at all today** — no `eslint` dependency, no `lint` script, no husky.
3. **Delete `sass`** (`^1.100.0`, devDependencies) — zero `.scss` files exist in the
   repository. A one-line removal with no code impact.
4. **Keep the three unreviewed libraries; record the reason.** `@base-ui/react` (**1**
   file), `@radix-ui/*` (**2** files) and `class-variance-authority` (**1** file) are in
   live use, and with MUI out of scope (§6) nothing in this migration replaces or removes
   the components they back. Removing a working dependency is itself an unrequested product
   change. Log the usage counts and the decision to keep; do not carry it as an open
   question.

**Build** `cd frontend && npm run build`
**Test** `npm test`, `npm run typecheck`, `npm run lint`
**Review** `frontend-style-review`
**Verification** `npm run lint` green and firing on pre-commit; `verify-gates.sh` vitest-pin
assertion passes; bundle size recorded via `report-bundle-size.sh` before → after.
**Rollback** `git revert`; `npm ci` restores the lockfile.

> **Exit** — lint is green and hooked, the chosen toolchain builds and tests, `sass` is gone,
> and the three libraries have a recorded decision.

---

### P15 — Make it stick · *lock-in*

**Preconditions.** Both chains complete.

**Steps.**

1. Flip `static-checks` to blocking in `ci.yml` (remove `continue-on-error`), with **all
   four** carried assertions explicitly allow-listed and **annotated** — not silenced:
   CR-1 presigned upload, `check-frontend-ui-rules.sh`, the `verify-gates.sh` sidebar
   assertion, and `structure-lint`'s `changes/0001-usage-events.xml`. Each annotation names the rule the gate conflicts with and the CR tracking
   it. The `== 1` form from P2 stays for CR-1. **This file still executes nowhere** — it is
   converged for a future remote; the enforcement that actually bites is step 5.
2. Confirm the AIAE `local-verify.sh` installed in P2 still runs gates, `mvn clean verify`,
   the frontend suite and the compose syntax check, end to end with no skipped step.
3. **Raise coverage to 0.80 LINE / 0.70 BRANCH and flip `.template-phase` to `engineering`.**
   This is a hard exit condition (§6.1 D-C): P15 does not close below it, and the temporary
   `mvp` floor ends here. Run `finalize-coverage`; the gap was measured in P9 step 2, so
   the size of this job is already known rather than discovered now. **Close out the P9
   step 2 note in the log.**

   Flipping the phase is what enforces 0.80/0.70 — the `-Pmvp` relaxation is passed only
   while `.template-phase` reads `mvp`. It is *not* the same thing as running the handoff
   script; see step 3a.
3a. **Do not run `prepare-engineering-handoff.sh`.** It is not a checker — it is a
   destructive migration. Verified in the standard: at line 174 it executes
   `remove-usage-logging.sh --apply`, and at line 282 it hard-fails if
   `backend/event-logging-to-db-feature` still exists. Running it would delete the usage
   telemetry this project is keeping (§6.1 D-D). Record in the log that the script is
   deliberately not run and why, so the next person does not read its absence as an
   oversight.
4. Document the `sync-llm-aux.sh --update-lock` upgrade path in `AGENTS.md`, so the next
   template revision arrives as a reviewed diff rather than as drift. The standard stays
   frozen at `cc64e49` until this point.
5. **Make `local-verify.sh` blocking** — remove the report-only mode installed in P2 step
   5, so it exits non-zero on any gate failure except the three allow-listed ones. With no
   CI, this is the only enforcement that exists; until now it has only reported.
6. **Write the handover note for whoever deploys.** Nothing in this migration was deployed
   or run in CI, so the release is somebody else's first contact with it. Record: the three
   carried assertions and why; that `ci.yml` has never executed; that the Replit deploy
   path is unverified (P3); and that P5 needs a `DATABASECHANGELOG` backup before it
   reaches a live database.

**Build** `mvn -f backend/pom.xml clean verify` (no flags, strict)
**Test** `bash scripts/local-verify.sh` end to end with no skipped step
**Review** `aiae-rule-compliance-audit` over the whole repository as the closing check
**Verification** Every gate zero except the four carried assertions, each annotated;
`.template-phase` = `engineering`; `llm-aux.lock` and `.template-version` current; the
handover note exists.
**Rollback** Revert `local-verify.sh` to report-only and `static-checks` to advisory;
`.template-phase` back to `mvp`.

> **Exit** — `local-verify.sh` passes end to end and blocks on failure, a fresh
> `aiae-rule-compliance-audit` reproduces it, and the handover note lists everything this
> migration could not verify locally.

---

## 5. Sequencing

```
P0 ─▶ P1 ─▶ P2 ─▶ P3 ─┬─▶ P4 ─▶ P5 ─▶ P6 ─▶ P7 ─▶ P8 ─▶ P9 ─▶ P10 ─▶ P11 ─┐
                      │                                                     ├─▶ P15
                      └──────────────────────────────────▶ P12 ───────────┘
```

- **P0 → P1 → P2 → P3** are strictly ordered. Nothing is blocked on a pending decision.
- **P6 requires P1** — `distributed_cache.md` is the protocol being adopted.
- **P10 must precede P11** — negative tests are the refactor's safety net.
- **P5 is a release of its own**, straight to `1.0.0`, rehearsed on a production restore.
- **P12 is gated on P0 step 7.** If both spike variants fail, the frontend chain stops at
  P12 and waits on CR-6; the backend chain continues independently.
- **There is no P13 and no P14.** Both numbers are retired, not reused — all frontend
  visual work is out of scope (§6), so the frontend chain is **P12 alone**. Numbering stays
  aligned with the audit's phases; do not renumber.
- **The backend chain (P4–P11) and P12 are independent** and can
  run in parallel with two people.
- **P15 requires both chains complete**, and carries **four** annotated red assertions:
  CR-1 (presigned upload), the whole UI-rules gate, the sidebar/navigation assertion in
  `verify-gates.sh` (§6), and `structure-lint`'s usage-events changelog path (§2.7).

---

## 6. What the technical owner decided

**All five settled 2026-08-10**, summarised in the header and recorded in full below.
Everything else in this plan is engineering and needed no signature — §6.3 lists what is
merely *recorded*, so nothing is hidden behind that claim.

### 6.1 The five decisions

**D-A — Scope: converge the engineering contract, leave the product's appearance alone.**

This migration fixes architecture, tests, coverage, CI configuration, docs and the deploy
contract. It does **not** touch how the product looks or navigates: MUI/Emotion stay, the
CSS stays, the left sidebar stays, all 12 routes stay.

It also runs **entirely locally** (§1) — nothing is pushed and nothing is deployed. Two
things therefore ship converged but unproven: `ci.yml` never executes, and the Replit deploy
path is never exercised. P15 step 6 writes both into a handover note rather than letting
them pass as done.

*The consequence, stated plainly:* **four gates stay red permanently** —
`check-frontend-ui-rules.sh` (~2222 findings), the `verify-gates.sh` sidebar assertion,
CR-1's presigned upload, and `structure-lint`'s `changes/0001-usage-events.xml` assertion
(§2.7, a consequence of D-D). At
the end of P15 this repository will **not** report "fully converged," and that is the
intended outcome, not a shortfall. Reasoning in *Not in scope* below. Four change requests
carry the conflict upstream: CR-1, CR-2, CR-3, and a new one for the UI rules.

**D-B — P5 needs one read query against production. Answered.**

Nothing is deployed in this migration, so there is no release slot to book, and — settled
2026-08-10 — **no database restore is needed either.** The whole of P5's risk reduces to one
question: what filename did Liquibase record in production? A single `SELECT` against
`databasechangelog` (§2.1) answers it, and access to run it is confirmed available.

*What the query decides:* classpath-form filenames prove §2.1 and clear P5 to proceed. A
module directory in the path invalidates §2.1, makes R1 real, and stops P5 until the
mitigation is redesigned. It runs in **P0 step 3a**, before any module work begins, so the
answer arrives while it is still cheap to act on.

*What is knowingly not done:* booting the application against a restored copy. That would
additionally prove the app starts clean against live data. If the query returns the expected
form the marginal value is low, and the log records that this second-order check was skipped
deliberately rather than forgotten.

**D-C — The path to coverage: relax per module, not globally.** *(revised 2026-08-10 on
measured data)*

**The target is fixed: 0.80 LINE / 0.70 BRANCH, with the seven hand-written excludes gone
and BRANCH gated for the first time.** Hard P15 exit condition. What is decided here is only
how the tree gets there.

The original decision was to drop every module to `mvp` 0.30/0.25 between P9 and P15,
because the climb was assumed to be large. **Measurement showed it is not.** With the
excludes removed:

| Module | LINE | BRANCH | |
|---|---|---|---|
| `application` | 0.8626 | 0.7110 | already passes |
| `event-logging-to-db-feature` | 0.9526 | 0.7778 | already passes |
| `service` | 0.8420 | 0.7075 | already passes |
| `external-services` | 0.8066 | **0.6978** | branch short by 0.0022 |
| `domain` | **0.0236** | **0.0714** | untested |

**Decision: hold 0.80/0.70 everywhere it already holds; relax only where it does not.**
`domain` and `external-services`' branch limit get scoped, temporary floors with the reason
recorded in their own POMs. Everything else is strict from P9 onward.

*Why the original shape was wrong:* a global 0.30 would have let the three passing modules
regress by fifty points across P10 and P11 — the largest refactors in the plan — with no
gate objecting. That is the exact risk D-C existed to manage, and the global relaxation
created it rather than containing it.

*What this costs:* it **diverges from the standard's phase model**, which expects one
project-wide flag (`.template-phase` plus `-Pmvp` = 0.30/0.25 for everything). Per-module
floors are not something that model expresses. Record the divergence in the log, and raise
it upstream: a brownfield project with one weak module should not have to choose between
lying about four healthy ones and blocking on the fifth.

*Still open, and it belongs before P9 rather than inside P15:* what happens to `domain`.
Three shapes — write real tests (entity `equals`/`hashCode` contracts and Testcontainers
repository tests, both already achievable here); keep the `entities`/`repositories` excludes
and diverge deliberately with a CR; or give `domain` a permanent lower floor with a recorded
reason. The scoped relaxation above makes this a P15 question instead of a P9 blocker, but
it does not answer it.

### 6.2 What this does not ask you for

Nothing about frameworks, module layout, caching mechanism, API shape, test tooling or
branching. Those are resolved in the plan with the reasoning recorded. If a phase turns out
to need a product judgement, it stops and asks — no phase currently does.

### 6.3 Recorded for information, not for signature

- **§2.1–§2.4** — four corrections where the audit or the v1 plan contradicted the code:
  R1 does not exist; `CacheConfig` does not exist and `@EnableCaching` must not be added;
  the coverage gate is live rather than dead; the upload fix belongs in `presignGet`.
- **§2.6** — resolved against the standard; the agent surface is un-ignored.
- **§1** — branching: `1.0.0` is never a phase merge target except P5.
- **§5** — phase order, including P10 before P11.
- **§3** — the evidence contract: no phase exits on a green build alone.
- The standard is frozen at `cc64e49` for the duration.
- Also excluded, unchanged from the audit: routing uploads through the backend (CR-1);
  forking the vitest pin locally (CR-6); re-materializing the project; and any change to
  the SSRF link guard beyond the mechanical static-factory conversion.

### Not in scope, deliberately

**All frontend visual and navigation work.** This was P13 and P14, both removed outright:
the MUI/Emotion removal, the CSS token migration, the vendored-tiptap relocation, **and the
sidebar → top-header navigation rebuild**. The rule the audit itself quotes when justifying
D1 is the reason, and it names all three protected things:

> *"Never replace an established product flow, navigation model, **or visual system** with a
> template default **unless the user explicitly asks for that change**."*

MUI, Emotion and `app/AppRoot.tsx`'s theme **are** this product's visual system. The audit
settles exactly two product decisions — **D1** navigation and **D2** caching — and files
MUI removal as **R4, a risk, not a decision**. It is also absent from the §6.1 table where
every other gate-versus-rule conflict was consciously resolved. **No sign-off exists**, and
by the audit's own reasoning for D1, *"without it, complying with the gate would itself have
broken the rule."*

**The UI is carried over, not rebuilt.** That extends past MUI. Measured 2026-08-10, the
project's own CSS holds **1499 raw `px`** and **611 hex literals across 84 distinct colours**
in 43 files, against only 87 tokens defined — so a token migration would have to *invent* a
palette for most colours, and consolidating 84 colours onto a smaller one is a redesign by
another name. `px` → `rem` is not identity-preserving either: `rem` follows the reader's
browser font-size setting and `px` does not. Neither belongs in a migration that is not
supposed to touch how the product looks.

The vendored-tiptap relocation goes with it. Its only purpose was to shave ~131 findings off
a gate that stays red regardless; moving 153 files for that is churn. The question belongs
upstream in **CR-3**, where a vendored-code exemption would remove the need entirely.

**The consequence is accepted openly:** `check-frontend-ui-rules.sh` stays red in full —
roughly 2222 findings — **carried as a known red assertion** alongside CR-1, annotated in
P15, never silenced. This is the CR-1 pattern applied again, and it should be forwarded
upstream as a change request the way D1 became CR-2.

**On navigation specifically (D1).** The audit records a sign-off dated 2026-08-09 to
replace the left sidebar with the scaffold's top-header shell, and correctly notes that
without such a sign-off the rebuild would itself break the rule. **That work is not being
done.** The product's navigation model is preserved: `app/AppShell.tsx`,
`shared/ui/Sidebar.tsx`, `shared/ui/sidebar.css`, the `isLessonReadingRoute` reading-mode
behaviour at `AppShell.tsx:11`, and all 12 route paths in `app/AppRoot.tsx` stay as they
are. `verify-gates.sh`'s sidebar assertion therefore stays red and is carried, annotated,
alongside the other two. This is what **CR-2** already asks the template maintainer to
resolve.

**This is the rule-compliant outcome, not a shortfall.** The gates encode a template
default; the rule says a template default must not replace an established product's flow,
navigation model, or visual system without an explicit request. Where the two conflict, the
rule wins and the conflict is documented — the same resolution the audit chose for CR-1.
The upstream package is now coherent: **CR-1** (presigned upload), **CR-2** (navigation
model), **CR-3** (vendored code), plus a new request covering the UI-rules gate.

If the technical owner ever explicitly asks for any of this, it returns as a new,
separately approved phase — not by reinstating P13 or P14. The measured shape is recorded
in §2.5 and here.

**Also not in scope:** routing uploads through the backend (CR-1); forking the vitest pin
locally (CR-6); re-materializing the project; and any change to the SSRF link guard beyond
the mechanical static-factory conversion.

**On approval**, P0 starts and `docs/aiae-migration-log.md` is created with the baseline.
