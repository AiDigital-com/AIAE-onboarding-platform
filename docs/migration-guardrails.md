# Migration Guardrails

**Every phase brief points here. Read it before editing anything.**

This lives outside `CLAUDE.md` deliberately. `CLAUDE.md` is a managed fixture —
`install-managed-claude-fixtures.py:22` lists it in `managed_root_files`, and it is
checksummed in `.claude/.aiae-fixtures-manifest`. Editing it by even one line makes
`install-claude-fixtures.sh` abort permanently, which would cost the template-upgrade path
that P15 step 4 depends on. So the project's own rules live here instead, and phase briefs
carry the pointer.

An AIAE convergence migration is in progress. [`aiae-migration-plan.md`](./aiae-migration-plan.md)
is the single source of truth; [`aiae-migration-log.md`](./aiae-migration-log.md) records what
has actually been measured. Obey your phase's **Scope** and **Do not touch** blocks.

Each rule below is a trap that has already been hit or verified in this repository.

## Never

- **Touch `1.0.0`.** It is production. Never check it out, never merge into it. Work lands on
  `migration` via one `mig/pNN-*` branch per phase. Nothing is pushed, nothing is deployed.
- **Edit `CLAUDE.md`, `AI-DEVELOPMENT-GUIDE.md`, `GDS-WORKFLOW-README.md`, `agent-payload.skills`,
  or anything under `.claude/`.** All are manifest-managed. Any edit aborts the installer with
  *"managed fixture was edited locally"*.
- **Normalise line endings on those same paths.** The manifest is deliberately mixed — some
  entries hashed CRLF, some LF. `.gitattributes` marks them `-text` for exactly that reason.
  Do not run `git add --renormalize` across the tree; do not "tidy" that exemption.
- **Run `materialize-project.sh`.** It copies unconditionally; its own comment calls a second
  run "the worst shape of that failure".
- **Rename `backend/migrations/src/main/resources/db/changelog/`.** Liquibase records the
  classpath path; renaming it re-runs all 14 changesets against a populated schema. The Maven
  module name is *not* part of that path and may change freely.
- **Add `@EnableCaching`** without setting `spring.cache.jcache.config: ehcache.xml` in the same
  change. `spring.cache.type: jcache` is already declared, so the annotation alone creates a
  second, empty `javax.cache.CacheManager` beside Hibernate's.
- **Refactor `backend/external-services/.../external/link/support/`** beyond a mechanical
  static-factory conversion. Those five classes are the SSRF guard.
- **Touch the two presigned-upload `fetch()` call sites** in
  `frontend/src/features/library/api/useLessonMutations.ts` and `useMaterialMutations.ts`. They
  fail a gate on purpose; the failure is tracked as CR-1.
- **Turn a gate green by renaming.** `Sidebar` → `NavRail` and the like change nothing real and
  destroy the signal for everyone after. Four assertions are red by decision — see the log.
- **Fix the frontend lint backlog.** `npm run lint` reports **339 errors** and that is the
  accepted state. `.husky/pre-commit` is report-only on purpose. 184 of them demand moving
  constants and interfaces out of components — restructuring 145 UI source files — and the
  product decision is that UI is carried over unchanged. Do not "clean up" lint, do not flip
  the hook to blocking, do not run `eslint --fix` (it repairs 0 of 339 anyway; the rule
  declares `fixable` and ships no fixer — CR-12).
- **Remove `backend/event-logging-to-db-feature`,** and never run
  `prepare-engineering-handoff.sh`: it deletes that module at line 174 and then fails at line
  282 if it survives. Usage telemetry is kept by explicit decision (D-D).

## Know

- **`python3` here is the Windows Store stub.** It prints `Python` and exits **0**. Ten
  checkers and `verify-gates.sh` invoke it internally, so a gate run without a real `python3`
  on `PATH` reports success having read nothing. Verify before trusting any gate result:
  ```
  python3 -c "import sys; print(sys.version)"   # must print 3.14.x, not "Python"
  ```
  PATH entries must be POSIX form (`/c/...`); bash does not parse `C:/...` as one.
- **Task artifacts are `review.md` and `verification.md`.** `.claude/tasks/README.md` claims
  `review-report.md` and `test-report.md`; that file is wrong and is an upstream defect (CR-7).
  It is a managed fixture — do not edit it to correct this.
- **Maven needs JDK 21**, not the JDK 25 on `PATH`:
  `JAVA_HOME=/c/Users/Admin/.jdks/corretto-21.0.12`.
