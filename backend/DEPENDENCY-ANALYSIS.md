# Maven dependency analysis

**Status as of P3 (2026-08-10): this policy is written, but the enforcement
mechanism it documents — `maven-dependency-plugin:analyze-only` bound to
`verify` — is not yet wired in `backend/pom.xml`.** Wiring it means adding a
plugin activation to the root `build/plugins` and an execution under
`pluginManagement` with `failOnWarning=true` and `ignoreNonCompile=true`.
`backend/pom.xml` is outside this phase's Scope (`docs/aiae-migration-plan.md`
P3 lists `docs/architecture-overview.md`, `backend/DEPENDENCY-ANALYSIS.md`,
`README.md`, `docker-compose.yaml` → `docker-compose.yml`, and `.replit` —
not `backend/pom.xml`), so `check-maven-dependency-analysis.py` still fails
today, now at its second check (`maven-dependency-plugin is required`)
instead of its first (`missing policy`). This is recorded here rather than
silently worked around; see `docs/aiae-migration-log.md`'s P3 entry for the
full explanation and the decision this leaves for the technical owner.

The rest of this document states the policy `check-maven-dependency-analysis.py`
will enforce once a later phase wires the plugin — written now, for this
project's actual dependency set, so the wiring phase has nothing left to
decide.

Once wired, `mvn verify` will run `maven-dependency-plugin:analyze-only` in
every backend module and fail on an unused compile dependency. Keep the
dependency graph small: remove a library when the gate identifies it as
unused, rather than adding a broad ignore.

The allowlist must contain exactly two coordinates:

- `org.projectlombok:lombok` — required at compile time for its annotation
  processor, while the compiled classes no longer reference the Lombok JAR.
- `${project.groupId}:event-logging-to-db-feature` — `backend/service/pom.xml`
  declares this dependency (with a comment explaining why:
  "`@LogUsage` lives in the `event-logging-to-db-feature` module — declared
  here so `*ServiceImpl` classes can annotate methods") so that services can
  use `@LogUsage` / `UsageAttributes` for MVP usage telemetry (D-D). Measured
  2026-08-10: no class under `backend/service/src` or
  `backend/application/src` references `LogUsage` or `UsageAttributes` yet —
  every real `*ServiceImpl` predates that integration — so `analyze-only`
  would flag this edge as unused today. It is kept anyway: the edge is what
  lets the first real service opt into `@LogUsage` without a `pom.xml` change,
  and `application` also receives the runtime feature transitively through
  it.

The second allowance covers only that pre-adoption window. It is not a
wildcard and does not suppress analysis of any other internal or external
dependency; the moment any `*ServiceImpl` uses `@LogUsage`, this edge becomes
a real compile dependency and the allowlist entry should be removed.

Runtime and test dependencies are intentionally excluded by Maven scope
(`ignoreNonCompile=true`); they are not compile dependencies and must not be
copied into this allowlist.

If a new framework is legitimately loaded only through reflection or
annotation processing, document the exact coordinate and reason here, then
add only that coordinate under `ignoredUnusedDeclaredDependencies` in
`backend/pom.xml`.
