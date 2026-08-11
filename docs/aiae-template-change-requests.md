# Change requests for `AIAE-replit-llm-aux`

**From:** AIAE Onboarding Platform team
**Against template revision:** `690a9748657adf81d01702dafa2c7ecc8afcf5c5` (`llm-aux.lock` v0.2.0)
**Raised:** 2026-08-09

## Context

We are converging an existing product — a Spring Boot + React onboarding platform, itself
originally materialized from an earlier revision of this template — back onto the current
AIAE standard. The full convergence audit is in `docs/aiae-audit.md` in our repository.

The migration is going well. The backend stack, package root, module model, Clerk auth and
contract-first OpenAPI shape all matched already, and most gate findings are ordinary work
on our side that we are simply doing.

This document covers only the cases where we believe **the gate, not the project, is what
needs to change** — where a check cannot be satisfied without violating a different AIAE
rule, or where it tests for a specific implementation rather than for the property it is
meant to protect.

Requests are ordered by severity. **CR-1 is a blocker for us**: it cannot be satisfied by
any change on our side.

---

## CR-1 — `verify-gates.sh` forbids the only compliant way to upload a large file

**Severity: blocking.** No project-side change resolves this.

### The conflict

`verify-gates.sh` fails on any occurrence of `fetch(`, `axios`, or `XMLHttpRequest` in a
non-test `.ts`/`.tsx` file under `frontend/src`:

```
! grep -RInE 'fetch[[:space:]]*\(|from[[:space:]]+["'\'']axios["'\'']|axios\.|new[[:space:]]+XMLHttpRequest' \
    --include='*.ts' --include='*.tsx' frontend/src \
    --exclude='*.test.ts' --exclude='*.test.tsx' >/dev/null || {
  fail "Frontend must use shared/api/client.ts, not raw fetch/axios/XMLHttpRequest"
}
```

Our product uploads lesson materials **directly to S3 via a presigned `PUT`**. The backend
issues a presigned URL, the browser `PUT`s the bytes straight to the storage host, and the
backend never sees the file's bytes. Two call sites do this, in
`features/library/api/useLessonMutations.ts` and `useMaterialMutations.ts`.

That `PUT` targets a third-party storage host, not our API. It cannot go through
`shared/api/client.ts`: the typed `openapi-fetch` client only knows our documented
endpoints, and routing an external host through it would attach our Bearer token to a
third party. So the request must use the platform `fetch`.

The only alternative implementation is to proxy uploads through the backend — which
`14-performance.md` explicitly forbids:

> Stream or presign file transfers; do not heap-buffer complete files without a small
> enforced limit.

**So the gate forbids the only implementation the standard's own performance rules
allow.** A project that follows `14-performance.md` cannot pass `verify-gates.sh`.

### Why we did not work around it

Two workarounds exist and we rejected both, deliberately:

- *Proxy uploads through the backend* — trades a gate failure for a performance-rule
  violation and a real product regression on large lesson materials.
- *Move the upload helper outside `frontend/src` so the scan misses it* — the gate scans a
  literal path. This is first-party code, and relocating it purely to dodge a `grep` games
  the check rather than meeting the standard.

We are leaving the two call sites in place and carrying the gate failure, documented, until
this is resolved.

### Proposed change

A committed, reviewable exemption mechanism — see **CR-4**, which proposes one mechanism
covering this, CR-2 and CR-3. For this case specifically the intent is narrow: *a request
to a host that is not our own API is out of scope for the API-client rule.*

If a general mechanism is unwelcome, a targeted alternative is to scope the rule to
same-origin calls — for example, permitting `fetch(` when the argument is not a string
literal beginning with `/`. That is weaker and easier to defeat, so we prefer the explicit
exemption.

---

## CR-2 — the sidebar gate contradicts a hard rule in `CLAUDE.md`

**Severity: high for brownfield projects. Not blocking us — we chose to absorb the cost.**

### The conflict

`verify-gates.sh` fails on any occurrence of these tokens anywhere under `frontend/src`:

```
Sidebar|SideNav|LeftNav|side-nav|side-menu|left-nav|left-menu|app__sidebar|layout__sidebar
```

`CLAUDE.md` states the opposite for exactly the case we are in:

> Never replace an established product flow, navigation model, or visual system with a
> template default unless the user explicitly asks for that change.

Our product has shipped a left sidebar across 14 pages, with deliberate behaviour attached
to it — the navigation is suppressed in lesson-reading mode and restored in activity
routes. The gate requires deleting it; the rule requires keeping it.

We read the gate's *intent* as **"a generated MVP should not invent a sidebar"**, which is
sound. Applied to a pre-existing product it becomes **"a shipped product may not have
one"**, which the rule explicitly forbids.

### What we did — updated 2026-08-10

**We are keeping the sidebar, and carrying this gate red for the duration.**

An earlier revision of this document reported that our product owner had signed off on
rebuilding the navigation as the scaffold's top-header `AppShell`. That sign-off existed,
but on review the rebuild was cancelled: it is a large, purely visual change to a shipped
product, and the rule quoted above is precisely the protection that makes declining it the
correct call. Complying with the gate would have meant doing the thing the rule exists to
prevent, on the strength of a sign-off obtained mainly to satisfy the gate.

So this is no longer a wall we walked around. It is a wall we stopped at:
`verify-gates.sh` fails on our repository, permanently, on an assertion we cannot satisfy
without breaking `CLAUDE.md`. The failure is allow-listed and annotated in our CI
configuration, never silenced, and this document is the reason it is allowed to stand.

That makes CR-2 blocking for us in a way the previous revision understated.

### Proposed change

Let a project declare its navigation model explicitly, and check that declaration instead
of grepping for component names. For example a committed `x-navigation-model: sidebar |
top-header` entry, defaulting to `top-header` when absent, so generated MVPs are unchanged
while an existing product can state its model and be checked against *that*.

### Related: the rename loophole

Because the gate matches identifier and class-name tokens, any project can pass it by
renaming `Sidebar` to `NavRail` and `app__sidebar` to `app__rail`, changing nothing about
the UI. We rejected that for ourselves, but a token-matching gate cannot detect it. A
declared-model check closes the loophole as a side effect.

---

## CR-3 — gates scan `frontend/src` literally, so vendored code has no defined home

**Severity: medium. We have a workable answer; we would like it confirmed as intended.**

Our lesson editor is built on Tiptap, and the vendored Tiptap template sources live under
`frontend/src/shared/editor` (~120 files). `check-frontend-ui-rules.sh` reports **119
violations** in them — raw `px` values and hex colors in upstream CSS we did not author.

Restyling vendored code to satisfy a house style forks upstream permanently and is undone
by the next dependency update. Nothing in the standard says how a project should hold
third-party code it must vendor rather than import.

### What we plan to do

Relocate genuinely vendored sources to `frontend/vendor/editor`, outside the scanned path,
with `@/` alias and `tsconfig` paths updated. We think this is defensible on its own merits
— vendored dependency code is not project source — and not merely gate avoidance.

**Please confirm this is the intended pattern.** If instead the scan path is meant to be
`frontend/` with a documented vendor exclusion, we would rather follow that. We want to be
explicit that we are *not* extending this reasoning to first-party code: see CR-1, where we
declined exactly that move.

---

## CR-4 — one exemption mechanism would resolve CR-1, CR-2 and CR-3

The three above are the same shape: a gate is right in general and wrong in one specific,
justifiable case, and today there is no way to say so. The available responses are all bad
— fork the gate, game the pattern, or carry an unexplained red build.

### Proposed shape

A committed exemption file, e.g. `.aiae-gate-exemptions`, read by `verify-gates.sh` and
`structure-lint.sh`:

```
# <gate-id> <path-glob> <reason>
frontend-api-client  frontend/src/features/library/api/useMaterialMutations.ts  presigned direct-to-S3 PUT; backend must not buffer bytes (14-performance.md)
frontend-ui-rules    frontend/vendor/**                                        vendored upstream Tiptap sources
```

Properties we think matter, in rough priority order:

1. **A reason string is mandatory.** An exemption without a justification should fail to
   parse. This is the whole difference between an exemption and a suppression.
2. **Committed and diffable.** Adding one shows up in review, exactly like `.template-phase`
   — which we think is a good precedent to follow here.
3. **Per-gate and per-path**, never global. No project-wide "skip this check."
4. **Fails when stale.** An exemption whose path no longer matches, or whose gate no longer
   reports a finding there, should error — otherwise the file silently accumulates.
5. **Reported, not silent.** `verify-gates.sh` should print the active exemptions on every
   run, so they stay visible rather than becoming invisible permanent state.

Point 4 is the one we would most want kept. Exemption mechanisms rot when nothing forces
them to justify their own continued existence, and a stale-exemption error is what turns
this into a ratchet rather than an escape hatch.

---

### Measured consequence: one accepted exception blinds the rest of the run

`verify-gates.sh`'s `fail()` is `echo` + `exit 1`. That is correct for a generated MVP,
where the first failure is a defect to fix and re-run. For a brownfield adoption carrying
decided exceptions it is not: **the earliest accepted exception hides every assertion after
it.**

Measured on this project. Before the change, the run stopped at the README assertion on
line 78 and reported one failure. After making `fail()` collect and continue, the same tree
reports **22** — including assertions the migration actively depends on:

- `Every Maven submodule must declare Lombok: migrations` — the entire purpose of one phase
- `Third-party PooledRestClientFactory must register ExternalClientMetricsInterceptor` —
  another phase's exit criterion
- `Logbook DefaultSink must be built with formatter + writer` — see CR-5
- five `.replit` and `docker-compose` assertions
- `service source must not import web/security/JWT/servlet APIs`

Twenty-one of twenty-two were invisible. Every one maps to real, known work; none was a
false positive. A project could have closed phase after phase believing the gate had
checked them.

We patched our copy to record and continue, then print the full list and exit non-zero. No
assertion was removed, reordered or weakened. But it is a local fork of a template script,
which is what CR-4 exists to make unnecessary.

**This raises CR-4 from convenience to blocking:** without an exemption mechanism, a
brownfield project cannot use `verify-gates.sh` at all — the first thing it legitimately
declines to fix silences the remainder of the file.

---

## CR-5 — the Logbook gate tests for an expression, not for the property

**Severity: low. Cosmetic for us, but the gate currently rejects a stricter implementation.**

`verify-gates.sh` requires two literal strings in `LogbookConfig`:

```
grep -Fq 'new DefaultSink(new JsonHttpLogFormatter(), new DefaultHttpLogWriter())'
grep -Fq '.strategy(new WithoutBodyStrategy())'
```

Ours reads:

```java
.strategy(resolveStrategy(props))
.sink(new DefaultSink(resolveFormatter(props), new DefaultHttpLogWriter()))
```

where `resolveStrategy` returns `WithoutBodyStrategy` unless `logBodies` is explicitly
enabled, and `resolveFormatter` returns a `MetadataOnlyHttpLogFormatter` — **more**
redacting than `JsonHttpLogFormatter`, not less. The default posture is stricter than the
scaffold's, and bodies are still never buffered in production.

The gate fails us anyway, because it matches an implementation rather than the property it
protects ("production HTTP logging is metadata-only and body-free by default"). We would
rather not flatten a configurable, stricter implementation into a literal to satisfy a
`grep`.

A property-level check — asserting the resolved strategy and formatter under the
production/Replit profile, ideally as a test in the scaffold rather than a `grep` — would
accept both shapes and actually verify the behaviour.

---

## CR-6 — conditional: the `vitest ^3.2.6` pin may be unreachable with current Tiptap

**Severity: unknown pending our spike. Raising early so it is not a surprise.**

`verify-gates.sh` requires `vitest ^3.2.6` exactly ("firewall-approved"). In practice that
implies Vite 5 and `@vitejs/plugin-react ^4`.

We currently run Vite 8, `vitest 4.1.10`, and Tiptap 3.22. We are running a compatibility
spike to determine whether Tiptap 3.22 (plus `@base-ui/react` and
`@tsparticles/confetti`) works on Vite 5.

- If it does, we downgrade and this request is withdrawn.
- If it does not, we will need the approved `vitest` version raised, because forking the
  pin locally re-creates exactly the drift this migration exists to remove.

We will report the spike result either way. No action needed yet — this is advance notice.

---

## CR-7 — `.claude/tasks/README.md` documents artifact names the `task-workflow` skill does not write

**Severity: low. Not blocking. Trivial to fix on your side.**

Both files ship from this template, and they disagree.

`.claude/tasks/README.md` documents the workflow artifacts as:

```
.claude/tasks/<task>/plan.md
.claude/tasks/<task>/dev-summary.md
.claude/tasks/<task>/review-report.md
.claude/tasks/<task>/test-report.md
```

`.claude/skills/task-workflow/SKILL.md` writes:

```
plan.md          matches
dev-summary.md   matches
review.md        README expects review-report.md
verification.md  README expects test-report.md
```

### Why it costs anything

An agent instructed to read the review output follows the README, looks for
`review-report.md`, finds nothing, and either burns a turn searching or concludes the review
never ran and repeats it.

### Why we did not fix it locally

`.claude/tasks/README.md` is a managed fixture. It carries a sha256 in
`.claude/.aiae-fixtures-manifest`, and `scripts/lib/install-managed-claude-fixtures.py:141`
aborts the whole install with *"managed fixture was edited locally; refusing overwrite"*.
Editing it would break `install-claude-fixtures.sh` for us permanently. We have stated the
correct names in our project `CLAUDE.md` instead, which is a workaround, not a fix.

### Proposed change

Update `.claude/tasks/README.md` to the names the skill actually writes, or rename the
skill's outputs to match the README — either direction works, they simply have to agree.

---

## CR-8 — `structure-lint.sh` hardcodes a changelog layout, so a project cannot keep its own

**Severity: medium. Blocking one assertion for us permanently.**

### The conflict

`structure-lint.sh` requires these exact paths whenever the corresponding optional module is
present:

```
db/changelog/db.changelog-master.xml
db/changelog/changes/0001-usage-events.xml        (event-logging-to-db-feature)
db/changelog/changes/0003-cache-invalidation.xml  (cache-management)
```

Our project uses a versioned layout, chosen long before this migration:

```
db/changelog/db.root-master.xml            include-only, zero changesets declared
db/changelog/1.0.0/db.version-master.xml   all 14 changesets, sqlFile form
```

We can and will rename the root changelog to `db.changelog-master.xml` — it declares no
changesets, so nothing recorded in `DATABASECHANGELOG` moves — and we will place the new
cache-invalidation changelog at the required path, since it is new and has no history.

**The usage-events assertion has no honest fix.** Our `1.0.0-usage-events` changeset is
already applied in production and recorded under
`db/changelog/1.0.0/db.version-master.xml`. Creating
`db/changelog/changes/0001-usage-events.xml` would declare the same DDL under a filename
Liquibase has never seen. `MARK_RAN` would absorb it, because the table exists — leaving a
hollow extra row in `DATABASECHANGELOG` whose only purpose is turning a gate green. We
declined to do that, so the assertion fails permanently.

### Why this matters beyond us

The gate is checking a *filename convention*, not a property of the database. Any brownfield
project with applied Liquibase history and a different layout hits the same wall, and the
only ways through are a hollow changeset or a permanent red gate.

### Proposed change

Check the property rather than the path: assert that a changelog declaring the usage-events
/ cache-invalidation schema is reachable from the configured master changelog, resolved
through `spring.liquibase.change-log`, rather than requiring a literal path. Failing that,
allow a project to declare its changelog root and `changes/` equivalent.

---

## CR-9 — `CLAUDE.md` is both "adapt this" and hash-protected

**Severity: high. It blocks the upgrade path for any project that follows your own instruction.**

`CLAUDE.md` ships saying: "Apply it to the current repository after discovering that
repository's actual structure, package root, build commands, ports, and existing product
decisions." A generated project is therefore expected to edit it.

It is also a managed fixture. `install-managed-claude-fixtures.py:22` lists it in
`managed_root_files`, its sha256 is recorded in `.claude/.aiae-fixtures-manifest`, and line 141
aborts the whole install with "managed fixture was edited locally; refusing overwrite" on any
mismatch. There is no override flag and no partial-update path; the manifest itself says
"Managed... Do not edit."

**The result: adapt `CLAUDE.md` as instructed, and `install-claude-fixtures.sh` can never run
again.** Every later template revision has to be applied by hand. We hit this on the first
phase, lost an attempt to it, and resolved it by keeping `CLAUDE.md` pristine and moving all
project rules into a separate unmanaged file. That works — but it means the one file agents
read automatically is the one file we may not put project rules in.

The same trap applies to the other three root entries: `AI-DEVELOPMENT-GUIDE.md`,
`GDS-WORKFLOW-README.md`, `agent-payload.skills`.

### Proposed change

Either drop `CLAUDE.md` from `managed_root_files` and treat it as seeded once, or add a
supported way to re-adopt a locally modified fixture: an explicit `--accept-local` flag, or a
marker region inside `CLAUDE.md` excluded from the hash, so projects have a sanctioned place
for their own paragraphs.

---

## CR-10 — the link checker flags files its own rewriter cannot fix

**Severity: low. Costs a manual step per install.**

`check-installed-documentation-links.py` scans installed content for removed control-plane
paths. On this project it reports six:

```
backend/.../UsageLoggingAspect.java:29            .../observability/usage-logging-rules.md
backend/.../LogbookConfig.java:2                  .../observability/logbook-http-logging-rules.md
backend/.../MetadataOnlyHttpLogFormatter.java:2   (same)
backend/.../OpenApiSpecConfig.java:6              .../openapi/canonical-openapi-rules.md
frontend/vite.config.ts:12                        .../frontend/canonical-react-frontend-rules.md
frontend/src/features/_template/README.md:20      .../frontend/bem-naming-rules.md
```

`rewrite-installed-documentation-paths.py` applies exactly the right replacement — the target
files do exist under `.claude/agent_docs/` — but it iterates `content_root.rglob("*.md")`.
Five of the six are `.java` and `.ts`, so the rewriter can never fix what the checker reports.

### Proposed change

Widen the rewriter to the file set the checker inspects, or narrow the checker to `.md`.
Either makes the pair self-consistent.

---

## CR-11 — the scaffold's own services violate the entity-service rule, and ship no example of it

**Severity: medium. Every generated project starts from a sample that breaks a hard rule.**

### The conflict

`.claude/rules/10-architecture.md` says, in the copy that ships from this template:

```
11: - Follow the rule `1 entity = 1 repository = 1 service to work with that entity`.
12: - Only the paired entity service implementation may inject that entity's repository.
```

`.claude/rules/00-backend-hard-rules.md:13` repeats it: *"Every backend JPA entity has one
repository in `domain` and one paired entity service in `service/entity`."*

Both scaffold service classes inject a repository directly:

```
scaffold/backend/service/.../service/cache/JpaCacheInvalidationEventService.java:34
    private final CacheInvalidationEventRepository repository;

scaffold/backend/service/.../service/sample/services/impl/SampleServiceImpl.java:37
    private final SampleRepository repo;
```

Neither is a paired entity service. `JpaCacheInvalidationEventService` is the cache
invalidation protocol adapter; `SampleServiceImpl` is the worked example a generated project
is meant to imitate.

**And `scaffold/backend/service/src/main/java/*/service/entity/` does not exist.** The
scaffold ships no paired entity service at all, so there is no reference implementation of
the shape the rule mandates — only two counter-examples, one of them the sample.

### Why it costs something

An agent generating a new feature reads the rule, looks at the sample for the house style,
and finds the sample contradicting the rule. Whichever it follows, a reviewer using
`backend-rule-review` will object.

For us it surfaced while installing `cache-management`. Taking your reference implementation
verbatim would have put a second class into our repository injecting a repository directly,
which our installed rules forbid. We added a paired
`CacheInvalidationEventEntityService` between the adapter and the repository instead — a
deliberate divergence from your reference, which we now have to remember at every template
sync.

### Proposed change

Pick one and make the scaffold agree with itself:

1. **Add the missing layer** — ship `service/entity/` with a paired entity service per
   scaffold entity, and route `SampleServiceImpl` and `JpaCacheInvalidationEventService`
   through it. This also gives generated projects the reference implementation the rule
   currently describes but never demonstrates.
2. **Or scope the rule** — state explicitly that protocol adapters under `service/cache/`
   and the sample feature are exempt, so a project can follow the scaffold without a
   documented divergence.

We would prefer (1): the rule is a good one, and the sample is the first thing every
generated project copies.

---

## Summary

| ID | Gate | Severity | Blocking us? | We need |
|---|---|---|---|---|
| CR-1 | `verify-gates.sh` — raw `fetch` | Blocking | **Yes** | An exemption path; the rule as written is unsatisfiable alongside `14-performance.md` |
| CR-2 | `verify-gates.sh` — sidebar tokens | **Blocking** | **Yes** | A declared navigation model instead of token matching |
| CR-3 | `check-frontend-ui-rules.sh` — scan path | Medium | No | Confirmation that relocating vendored code is the intended pattern |
| CR-4 | Cross-cutting | **Blocking** | **Yes** | An exemption mechanism — without it one accepted exception hides 21 further assertions |
| CR-5 | `verify-gates.sh` — Logbook literals | Low | No | A property check rather than a string match |
| CR-6 | `verify-gates.sh` — `vitest` pin | TBD | TBD | Advance notice; spike result to follow |
| CR-7 | `.claude/tasks/README.md` vs `task-workflow` | Low | No | The two to agree on artifact names |
| CR-8 | `structure-lint.sh` — changelog paths | Medium | **Yes** | A property check instead of hardcoded paths |
| CR-9 | `CLAUDE.md` managed *and* meant to be adapted | **High** | **Yes** | Seed-once, or a sanctioned unhashed region |
| CR-10 | link checker vs path rewriter file sets | Low | No | The two to cover the same files |
| CR-11 | scaffold services break the entity-service rule | Medium | No — diverged | A paired entity service in the scaffold, or an explicit exemption |

Everything else in our audit is our own work and is in progress. We are happy to supply the
full audit, reproduction commands, or a patch for any of the above.
