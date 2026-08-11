# AIAE Migration Log

Evidence record for [`aiae-migration-plan.md`](./aiae-migration-plan.md). One row per phase,
appended in the same commit as the work. A phase is not complete until its row exists.

**This log is authoritative over the plan for any measured number.** The plan's §0 lists
which of its figures were carried from the audit unverified; every one of those is replaced
below by a measured value.

---

## P0 — Baseline · *in progress*

Measured 2026-08-10 against `c53a76d` (pre-migration baseline), branch `migration`.

### Environment — read this before running anything

| Tool | Found | Expected by the project | Consequence |
|---|---|---|---|
| `python` | 3.14.5 | — | real interpreter |
| **`python3`** | **Windows Store stub** | a real Python 3 | **see below — this silently disables gates** |
| `mvn` | **absent** | Maven | backend build/test/coverage cannot run here |
| `java` | 25.0.3 | **21** | untested against the build |
| `node` | 24.16.0 | 22 (CI) | untested against the build |
| `bash` | 5.2.37 (msys) | — | fine |

**The `python3` stub is the single most dangerous thing in this environment.** It accepts
stdin, prints `Python`, and exits **0**. Ten of the 28 checkers embed
`python3 - "$@" <<'PY'`, and `verify-gates.sh` itself calls `python3` directly at lines 187,
194, 198 and 201. Run the suite without fixing this and those checks report success having
read nothing — the exact "green because the check does not run" failure the plan forbids.

The plan's §P0 wording ("use an explicit interpreter for the Python gates") is not
sufficient: the invocation is *inside* the scripts, so it cannot be overridden from the call
site. `python3` must resolve to a real Python before anything is run. Verified fix used for
this baseline: copy the real `python.exe` to a directory named `python3.exe` and prepend
that directory to `PATH` in POSIX form (`/c/...`, not `C:/...` — bash will not parse the
latter as a PATH entry).

**Not measured, and why:** `mvn -f backend/pom.xml -Phandoff verify` and the backend test
suite. Maven is not installed on this machine. The coverage measurement that P9 step 1
depends on is therefore still outstanding, and P9 cannot start until someone runs it.

### Gate baseline

Run from the project root against a scratch copy of `cc64e49`'s
`templates/generated-project/scaffold/scripts/`, with a working `python3`.
Scanners that take source roots were given them explicitly, as `verify-gates.sh` does.

| Gate | Measured | Audit said | |
|---|---|---|---|
| `check-thin-controllers.py` | **55** | 55 | ✅ |
| `check-api-validation-tests.py` | **91** constrained ops, **0** `isBadRequest` | 91 / 0 | ✅ |
| `check-openapi-input-constraints.py` | **82** | 82 | ✅ |
| `check-openapi-documentation.sh` | **4** | 4 | ✅ |
| `check-service-contract-quality` | **25** | 25 | ✅ |
| `check-coverage-integrity.sh` | **15** | 15 | ✅ |
| `check-installed-documentation-links.py` | **6** | 6 | ✅ |
| `check-frontend-ui-rules.sh` | **2222** | 2222 | ✅ |
| `check-production-static-methods.sh` | **8** | 8 | ✅ |
| `check-production-current-time.sh` | **3** | 2 | ❌ **off by one — see below** |
| `check-production-magic-values.sh` | 0 | 0 | ✅ |
| `check-production-manual-mapping.sh` | 0 | 0 | ✅ |
| `check-api-client-paths.sh` | 0 | 0 | ✅ |
| `check-liquibase-preconditions` (`.sh` + `.py`) | 0 | 0 | ✅ |
| `check-openapi-enums.sh` | 0 | — | ✅ |
| `check-openapi-strict-schemas.sh` | 0 | — | ✅ |
| `check-architecture-overview.sh` | 1 — missing section `Document status` | 1 | ✅ |
| `check-agent-surfaces.sh` | mode = `handoff` (AGENTS.md / replit.md / `.agents` absent) | — | P1 outstanding |
| `check-maven-dependency-analysis.py` | invocation-dependent — reports "no backend pom" when given `.` | 1 | resolve in P2 |

**The audit measured honestly.** Sixteen of seventeen comparable figures reproduce exactly.

### Correction — `check-production-current-time` is 3, not 2

The third violation is in a file no phase currently touches:

```
service/common/time/CurrentTime.java:21   return LocalDateTime.now(ZoneOffset.UTC);
service/common/time/CurrentTime.java:30   return Instant.now();
external/storage/impl/CloudFrontUrlSigner.java:56   .expirationDate(Instant.now().plus(expiresIn))
```

P8 step 2 splits `CurrentTime` into an interface plus `CurrentTimeImpl`, which addresses the
first two. **`CloudFrontUrlSigner` is a separate fix** — it must take the injected
`CurrentTime` instead of calling `Instant.now()` directly. P8's exit criterion is 3 → 0.

### R1 — settled

Query run against production 2026-08-10:

```sql
SELECT orderexecuted, id, author, filename, exectype
FROM   databasechangelog ORDER BY orderexecuted;
```

Result: `filename = db/changelog/1.0.0/db.version-master.xml` — the classpath path, carrying
neither `backend/db` nor `backend/migrations`.

**§2.1 confirmed. R1 does not exist.** The module rename is invisible to Liquibase because
the recorded filename is resolved from the classpath root (`src/main/resources`), which the
Maven module directory name is not part of. P5 proceeds with no `logicalFilePath` pin and no
`changelogSync`.

### A retracted finding, recorded so it is not rediscovered

Running the scanners bare from a scratch directory makes them print
`no source directories to scan` and exit **0**. This is not a defect: with no arguments they
`cd` to `SCRIPT_DIR/../..` and scan the scaffold they live in. Invoked through
`verify-gates.sh` — or with explicit source roots, as above — they behave correctly. Do not
"fix" this.

### R5 spike — answered, and it is not a deadlock

Run 2026-08-10 in a scratch copy of `frontend/`, variant A only.

```
vitest  ^3.2.6  -> resolved 3.2.7
vite    ^5.4    -> resolved 5.4.21
@vitejs/plugin-react ^4.3.4
```

| Step | Result |
|---|---|
| `npm install` | **430 packages, no peer conflicts** |
| `npm run generate:api` | ok |
| `npm run build` | **✓ built in 10.23s** |
| `npm test` | **22 files, 78 tests, all passed** |

Tiptap 3.22, `@base-ui/react` and `@tsparticles/confetti` all work under Vite 5.
**CR-6 is not needed and should be withdrawn.** P12 takes the standard's pins as written.

Variant B (`vite ^6`) was designed as the fallback if A failed. A did not fail, so B was not
run — it could only have produced a smaller downgrade nobody now needs.

Two notes for later, neither blocking:
- The first build attempt failed with `TS2307: Cannot find module './generated/schema'` and a
  cascade of `TS7006`. That is the gitignored OpenAPI output, not a Vite problem —
  `npm run generate:api` must precede `npm run build` on any clean tree.
- The build emits a **662 kB** `SimpleEditor` chunk (tiptap), over Vite's 500 kB warning
  threshold. Record it as the bundle baseline in P12.

### Frontend product baseline — and the cost of the downgrade

`npm ci && npm test` on the tree as it stands, and the same on the spike copy:

| Toolchain | Tests | Duration |
|---|---|---|
| **current** — vite 8.1.4 / vitest 4.1.10 | **22 files, 78 tests, all pass** | **5.6s** |
| after P12 — vite 5.4.21 / vitest 3.2.7 | 22 files, 78 tests, all pass | **52.1s** |

**Zero test regression** from the downgrade — the same 78 tests pass on both. But the suite
runs roughly **nine times slower** on the pinned toolchain. Neither source document mentions
this, and it is a real recurring cost: every phase after P12 pays it on every run, and with
no CI the local suite is the only thing that runs at all.

Not a reason to reverse the decision — the `vitest ^3.2.6` pin is gate-enforced and the
template is read-only. It is a reason to raise it upstream: CR-6 was written for a
compatibility deadlock that did not materialise, and can be **repurposed** to report the
performance cost of the mandated pin instead of withdrawn outright.

### Backend product baseline — and the real coverage number

`mvn -f backend/pom.xml -Phandoff verify` on JDK 21.0.12 / Maven 3.9.16:
**BUILD SUCCESS, 507 tests, 0 failures, 44 skipped, 4m02s.** `jacoco-check` passed —
confirming §2.3 empirically: the hardcoded 0.80 LINE gate is live and the project meets it.

**Coverage, measured both ways.** The second row is P9 step 1's answer: the seven
hand-written excludes removed from the `report` block, `jacoco:report` re-run against the
same `jacoco.exec`, POM reverted afterwards.

| Denominator | LINE | BRANCH |
|---|---|---|
| current — 7 hand-written excludes in place | **0.8517** | **0.7183** |
| **real — excludes removed** | **0.8284** | **0.6946** |

Per module, which is what matters because `jacoco-check` uses `<element>BUNDLE</element>`
and each module is graded on its own:

| Module | LINE | | BRANCH | |
|---|---|---|---|---|
| `application` | 0.8626 | ok | 0.7110 | ok |
| `event-logging-to-db-feature` | 0.9526 | ok | 0.7778 | ok |
| `service` | 0.8420 | ok | 0.7075 | ok |
| `external-services` | 0.8066 | ok | **0.6978** | short by 0.0022 |
| **`domain`** | **0.0236** | **miss** | **0.0714** | **miss** |

**Four of five modules already clear 0.80 / 0.70 on the honest denominator.** The entire gap
is `backend/domain` — the entities and repositories the `**/entities/**` and
`**/repositories/**` excludes were hiding — plus a rounding-margin miss on
`external-services` branch coverage.

### This undercuts the premise of D-C

D-C chose to relax to `mvp` 0.30/0.25 between P9 and P15 because the climb to 0.80/0.70 was
assumed to be large. It is not. It is one untested module and two-tenths of a percent
elsewhere.

Dropping every module to 0.30/0.25 would let the four that already pass **regress by fifty
points** during P10 and P11 without any gate objecting — which is the opposite of the risk
D-C was written to manage. Worth re-deciding with these numbers in hand. A shape that fits
the measurement better: hold 0.80/0.70 where it already holds, and give `domain` alone a
temporary floor with the reason recorded, raised when its tests land.

**This is the technical owner's call**, since D-C was. The plan is not changed on this
point until they decide.

### Still outstanding in P0

| Step | Status |
|---|---|
| 1 — branch `migration` from `1.0.0` | done — `c53a76d`, `adc9f49`, `4d9b187` |
| 1a — `CLAUDE.md` guardrails | done — `c5cb107` |
| 1b — commit-message template | outstanding — the evidence-contract trailer is not yet written down |
| 1c — this log | done |
| 2 — working-tree state confirmed | done — `backend/db` gone, `backend/migrations` present, Lombok still absent from its POM |
| 3 — gate baseline | done, above |
| 3a — R1 query | done, above |
| 4 — product baseline | **done** — frontend 78/78, backend 507 tests green, real coverage measured |
| 5 — send change requests upstream | outstanding — owner's action |
| 6 — standard checkout reachable at `cc64e49` | done — all required paths present |
| 7 — R5 spike | **done — variant A passes; CR-6 withdrawn** |

---

## P1 — Agent surface · **BLOCKED at step 0a — no file changes made**

Attempted 2026-08-10 on branch `mig/p01-agent-surface` (from `migration` @ `cb7e61a`).

### Step 0a — fixture-manifest validation: 79 of 80 match, not 80 of 80

Every `path<TAB>sha256` line in `.claude/.aiae-fixtures-manifest` was hashed against the file
on disk (`python3 -c` sha256, real interpreter shimmed onto `PATH` first — see P0 environment
notes). **79 of 80 matched. `CLAUDE.md` did not:**

```
expected (manifest): f65b04a7125c5a8fd94c02d2f99af2066d57ec5e5eeea4161807b7f791423fd8
actual (on disk)   : d4ba51af8106a9ce9a76166d19e24dd0d564bb5ee183024b202fe8464cbbc7a3
```

Per the plan's own instruction, this is where the phase must stop: *"If any do not [match],
stop — step 1 will abort with 'managed fixture was edited locally', and that message will
point you at the wrong cause."* It does. But the plan's diagnosis of the likely wrong cause
("the usual real cause is line-ending normalisation") is itself not what is happening here,
and that distinction matters enough to record.

### Root cause — not §2.8, a different and more basic problem

1. **`CLAUDE.md` is a manifest-checked fixture even though §2.8 says the manifest covers
   "every file under `.claude/`."** It does not, and the code proves it:
   `install-managed-claude-fixtures.py`'s `managed_root_files` set is
   `{AI-DEVELOPMENT-GUIDE.md, CLAUDE.md, GDS-WORKFLOW-README.md, agent-payload.skills}` —
   four **root-level** files, none of them under `.claude/`. `.claude/.aiae-fixtures-manifest`
   lists `CLAUDE.md` explicitly (confirmed by `grep`), and the installer's first loop
   (`install-managed-claude-fixtures.py:134-142`) checks it exactly like any `.claude/**`
   entry: any hash mismatch on any managed root file aborts the whole run with the same
   `SystemExit`, before a single file is written. §2.8 undercounts the manifest's scope by
   four files; this is now filed alongside the other corrections in §2.

2. **The actual mismatch is content, not line endings.** Tested directly: the current
   `CLAUDE.md`, LF-normalized, hashes to `30c26e7c…` — matching neither the manifest's
   `f65b04a7…` nor the raw-on-disk `d4ba51af…`. Line endings were checked too and are a real,
   separate, non-blocking oddity (`core.autocrlf=true` on this machine checks the file out as
   CRLF while the git blob and `.gitattributes`'s `eol=lf` both say LF — `git ls-files --eol`
   shows `i/lf w/crlf`), but forcing every candidate rendering of the *current* content
   through LF, CRLF, and raw-as-read produces three different hashes and **none of them is
   the manifest's expected value.** The manifest's hash matches exactly one thing: the
   **pre-guardrails** `CLAUDE.md` from baseline commit `c53a76d`, rendered CRLF —
   i.e. the file as `install-claude-fixtures.sh` originally installed it from the standard
   checkout, before P0 touched it.
3. **P0 step 1a added the "Migration Guardrails" block to `CLAUDE.md` (commit `c5cb107`) and
   nothing re-derived the manifest's `CLAUDE.md` entry afterward.** There is no supported way
   to update one entry — the manifest header reads "Managed by
   `scripts/install-claude-fixtures.sh`. Do not edit," and regenerating it means re-running
   that exact installer, which is the thing that now refuses to run because of this mismatch.

### This is a contradiction in the plan, not a mistake in this run

P0 step 1a requires editing `CLAUDE.md` — a file the installer treats as a managed root
fixture — *before* the first agent runs, i.e. before P1. P1 step 0a requires 80/80 of the
manifest to match before proceeding, and P1 step 1 assumes re-running the installer "just"
overwrites `CLAUDE.md` (plan line 469: *"P1 step 1 overwrites `CLAUDE.md`"*). It does not:
`install-managed-claude-fixtures.py` checks every previously-owned path against the manifest
**before** writing anything, and aborts on the first mismatch — which, given P0's edit, is
now `CLAUDE.md` itself. The three instructions cannot all be satisfied in the order the plan
gives them. Nothing in P0 or P1 tells anyone to regenerate the manifest's `CLAUDE.md` entry,
and hand-editing the manifest or reverting the guardrails block are both excluded by this
same step 0a ("do not try to 'fix' the files") and by the manifest's own "do not edit" header.

**Per the plan's explicit instruction, this phase stops here.** No file in the working tree
was changed — `.gitignore` was read but not edited, no fixture was copied, `AGENTS.md` /
`replit.md` / `.agents/**` were not created. `git status --porcelain` is empty on this branch
except for this log entry.

### What was safe to measure anyway (read-only, no fixture involved)

Run against this project's tree with the standard's scanners (`cc64e49`, scratch invocation,
real `python3` on `PATH`) to record the "before" state for whoever unblocks this:

| Gate | Result |
|---|---|
| `check-agent-surfaces.sh` | `handoff` — `AGENTS.md`/`replit.md`/`.agents` all absent, consistent with P0's baseline |
| `check-installed-documentation-links.py` | **FAIL, 6** broken references — identical set to P0's baseline |

Both are unchanged from P0, as expected: nothing that would move them ran.

**Build** n/a — P1 has none in the plan; also moot, no code changed.
**Test** n/a — P1 has none in the plan; also moot, no code changed.
**Review** not run — `aiae-rule-compliance-audit` reviews a diff, and there is none.
**Verification** `check-agent-surfaces.sh`: `handoff` → `handoff` (no movement);
`check-installed-documentation-links.py`: 6 → 6 (no movement). Both gates stay exactly where
P0 measured them, which is the expected/correct outcome for a phase that made no changes.
**Rollback** n/a — nothing was changed; the branch `mig/p01-agent-surface` holds only this
log entry.

**Unblocking this requires a decision outside this phase's authority**: whether to
(a) accept `CLAUDE.md`'s guardrails-block content as the new baseline and regenerate
`.claude/.aiae-fixtures-manifest`'s `CLAUDE.md` entry to match it (mechanically correct, but
"do not edit" is written into the file for a reason and no tooling exists to do this
partially), (b) special-case `CLAUDE.md` out of the managed-root-files check upstream, or
(c) re-order P0/P1 so the guardrails block is added *after* P1's fixture sync instead of
before it. This is recorded as a plan defect for the technical owner, not fixed here.

---

## P1 — Agent surface · **COMPLETE**

Completed 2026-08-10 on branch `mig/p01-agent-surface` (from `migration` @ `cb5f57a`), after the
blocked attempt above. **Unblocked by two fixes made before this run started** (not by this
phase): `CLAUDE.md` was restored to be byte-identical to its manifest entry, with the
guardrails block moved to `docs/migration-guardrails.md`; and `.gitattributes` gained the
`-text` exemption for the four managed root files (`CLAUDE.md`, `AI-DEVELOPMENT-GUIDE.md`,
`GDS-WORKFLOW-README.md`, `agent-payload.skills`), not only `.claude/**`. Both fixes are
visible in the repository, not part of this phase's diff.

### Step 0a — fixture manifest: 80 of 80 match

Re-ran the same hash-comparison as the blocked attempt (real `python3` shimmed onto `PATH`
first — see environment note below). **80 of 80 matched**, including `CLAUDE.md`. Cleared to
proceed.

### Step 0 — `.gitignore`, the four §2.6 changes

Applied before creating any of the files below: un-ignored `.agents/`, `AGENTS.md`,
`replit.md`; restored the two deleted header sentences ("The active dual-agent runtime is
part of the app…" / "Both active agent surfaces are deliberately NOT ignored."); added
`.claude/tasks/*` + `!.claude/tasks/README.md`. Left `templates/` and `custom_instruction/`
ignored, unchanged.

### Step 1 — re-ran `install-claude-fixtures.sh`

**Environment gap not listed in the plan's traps: `rsync` is not installed in this shell.**
The installer's only two `rsync` calls are always the shape `rsync -a SRC/ DST/` (copy tree
contents). Installed a same-directory shim on `PATH` (ahead of the `python3` shim) that
translates that exact call to `mkdir -p DST && cp -a SRC/. DST/` via GNU `cp`; verified against
a throwaway directory pair before use. No project file or script was touched to work around
this — the shim lives outside the repository, like the `python3` fix.

Ran `bash scripts/install-claude-fixtures.sh <project>` from the standard checkout
(`cc64e49`). It installed 80 files, removed 0, and exited **1** — but only at its own final
`check-installed-documentation-links` step, which failed with exactly the 6 broken references
named in Scope for step 6. All fixture copying had already completed by that point. Confirmed
every changed file under `.claude/agent_docs/**`, `.claude/skills/**`, `AI-DEVELOPMENT-GUIDE.md`
and `GDS-WORKFLOW-README.md` is byte-identical to the prior version once both are normalised to
LF — i.e. every one of these diffs is a line-ending rewrite from the mixed-CRLF/LF manifest
(§2.8), not a content change. `.mcp.json` had no diff at all (merge preserved it). Re-ran the
80-entry manifest hash check immediately afterward: **80 of 80 still match**, `CLAUDE.md`
included (`git diff --quiet -- CLAUDE.md` reports no difference from `HEAD`).

### Steps 2–5 — AGENTS.md, replit.md, `.agents/skills/`, pins, and the "already correct" checks

Copied `AGENTS.md` / `replit.md` verbatim from `scaffold/AGENTS.md.template` /
`replit.md.template` (byte-identical, confirmed by `diff`). Mirrored `.agents/skills/` verbatim
from the standard (`cp -a`, 24 files across 12 skill directories, including
`.agents/skills/verification-gate/SKILL.md`). Added `llm-aux.lock` (verified its `revision=`
line reads `690a9748657adf81d01702dafa2c7ecc8afcf5c5`, `version=0.2.0`, matching the plan's
pin exactly), `.template-version=cc64e49`, `.template-phase=mvp`.

Confirmed step 4's four "already correct" claims, all true: `.claude/rules/README.md` absent;
`aiae-rule-compliance-audit` present and `rule-compliance-audit` absent from
`.claude/skills/`; `.claude/agent_docs/` carries **29** top-level entries (11 directories + 18
files — matches exactly); `.aiae-fixtures-manifest` present. Left `CLAUDE.md` untouched per
step 5; re-validated the manifest at 80/80 immediately before committing (repeated below as the
final count).

### Step 6 — the six documentation citations, and a seventh place the plan did not anticipate

Fixed the prefix `templates/generated-project/` → `.claude/agent_docs/` by hand in exactly the
six files named in Scope, comment lines only, no executable code changed (each diff below is a
single line):

```
backend/event-logging-to-db-feature/.../usagelogging/UsageLoggingAspect.java:29
backend/application/.../config/LogbookConfig.java:2
backend/application/.../config/MetadataOnlyHttpLogFormatter.java:2
backend/application/.../config/OpenApiSpecConfig.java:6
frontend/vite.config.ts:12
frontend/src/features/_template/README.md:20
```

**Found beyond the plan, and fixed within the general `.agents/**` Scope grant (not a step-6
file):** running `check-installed-documentation-links.py` after steps 0–5 but before any step-6
fix reported **33** violations, not 6. The extra 27 were all inside `.agents/skills/**/*.md` —
five skill files (`backend-java-feature/SKILL.md` and its `backend-workflow-details.md`,
`frontend-react-feature/SKILL.md`, `openapi-contract-first/SKILL.md`,
`mvp-safety-review/references/publish-gate-checks.md`) mirrored verbatim in step 3, still
carrying the same `templates/generated-project/…` citations the six-file list was written to
fix. `templates/` does not exist anywhere in this project (confirmed: `ls templates` →
"No such file or directory"), so every one of those citations is genuinely dangling, exactly
the class of defect the checker exists to catch — not a false positive.

**Root cause:** `install-claude-fixtures.sh` runs `rewrite-installed-documentation-paths.py`
against `.claude/agent_docs` and `.claude/skills` (lines 90 and 106 of the installer) but never
against `.agents/skills` — the rewriter has no path into the Replit-facing skill mirror at all.
The plan's step-6 rationale ("the standard's own rewriter cannot" fix five of the six because
it only iterates `.claude/skills`'s `*.md`) is correct as far as it goes, but understates the
gap: the same rewriter also cannot reach `.agents/skills`, and step 3's "mirror `.agents/skills`
verbatim" instruction reintroduces the identical defect class 27 times over, unmentioned in
either Scope's step-6 file list or the phase's Verification block ("6 → 0").

**This is a plan gap, not a blocking contradiction**, because `.agents/**` is already listed in
P1's general Scope (not restricted to the six named files — that restriction reads as scoped to
`backend/`/`frontend/`, confirmed by the task framing given for this run), so fixing it stays
inside this phase's authority. Ran the standard's own
`rewrite-installed-documentation-paths.py` against `.agents/skills` (read-only tool invoked from
the external standard checkout; nothing under this project's `scripts/**` was touched) — the
exact same substitution the installer already trusts for `.claude/skills`. Re-ran the checker:
**0** violations. Filed here for the technical owner as an extension of CR-10: step 3 needs
either "run the rewriter against `.agents/skills` too" or "step 6 also covers every
`templates/generated-project/` citation inside `.agents/skills/**/*.md`," not just the five
originally named.

### Environment note: `rsync` is a second missing tool, alongside the documented `python3` stub

Not in the plan's environment-traps list. `which rsync` found nothing anywhere on `PATH`, and a
search of common Windows install locations (`cwrsync`, `msys64/usr/bin`, `scoop/apps`) found
nothing either. Worked around with a minimal shim (above) rather than skipping step 1 or hand-
copying files outside the installer's own logic. Recording this so the next phase that needs
`rsync` (none currently do) does not repeat the search.

### Review — `aiae-rule-compliance-audit`

Verdict: **COMPLIANT**. Scope: this phase's diff only. Evidence: `git diff --cached --stat`
(52 files, all inside the Scope list); the six named files' diffs (each exactly one comment
line); `git diff --cached --name-only | grep -E '^(backend|frontend)/'` returns exactly those
six paths, nothing else; `.github/**` and `scripts/**` show zero touched files;
`git diff --quiet -- CLAUDE.md` confirms no change. No blocking or important findings. One
architecture concern recorded above (the `.agents/skills` citation gap) — resolved in this same
commit, not left open.

### Verification

| Gate / check | Before | After |
|---|---|---|
| `check-agent-surfaces.sh` | `handoff` (mode; `AGENTS.md`/`replit.md`/`.agents` all absent) | `active`, exit 0 — all three present, `.agents/skills/verification-gate/SKILL.md` present |
| `check-installed-documentation-links.py` | **6** broken references | **0** — plan's literal instruction alone would have left this at **27** (see step 6 above); fixing the discovered `.agents/skills` gap in the same commit reaches the plan's stated exit criterion |
| `.claude/.aiae-fixtures-manifest` | 80/80 (validated at step 0a) | **80/80** (re-validated after step 1 and again before commit) — `CLAUDE.md` unchanged throughout |
| `git ls-files` | — | `AGENTS.md`, `replit.md`, `.agents/skills/**`, `llm-aux.lock`, `.template-version`, `.template-phase` all tracked and not gitignored (`git check-ignore` confirms none are excluded) |

**Build** n/a — P1 has no build step; the phase touches documentation, fixtures, and code
comments only, and nothing under `backend/` or `frontend/` compiles differently (the six edits
are comment-only, verified by diff).
**Test** n/a — P1 has no test step for the same reason; no production logic changed.
**Review** `aiae-rule-compliance-audit` — **COMPLIANT**, see above.
**Verification** `check-agent-surfaces.sh`: `handoff` → `active` (exit 0);
`check-installed-documentation-links.py`: **6 → 0** (via 33 at the midpoint, see step 6);
fixture manifest: 80/80 throughout, `CLAUDE.md` byte-identical to its manifest entry both
before and after; `git ls-files` reflects the §2.6 un-ignore decision exactly.
**Rollback** `git revert` on this phase's commit; also revert the pre-existing `CLAUDE.md`/
`.gitattributes` fixes if the intent is to reproduce the blocked state (not recommended — those
fixes were correct). No out-of-repo action.

### Things the plan got right this time, worth recording

- §2.1/R1 and §2.8's core claim (the manifest covers `.claude/**` plus four root files) both
  held exactly as documented.
- The six-file Scope list for step 6 was accurate and complete for what it claimed to cover
  (files where the installed rewriter cannot run) — it just did not anticipate step 3
  introducing the same defect class through a different, unrewritten mirror.
- `.claude/agent_docs/` — 29 entries, `aiae-rule-compliance-audit` present,
  `rule-compliance-audit` absent, `.claude/rules/README.md` absent — every "already correct"
  claim in step 4 was verified true.

### Nothing was declined in this phase

Every step in P1's Steps list (0, 0a, 1–6) was executed. The one thing done beyond the literal
text — fixing `.agents/skills/**/*.md` citations — was necessary to meet the phase's own stated
Verification target and stayed inside the general Scope grant; it is documented above rather
than silently absorbed.

## P2 — Harness and the local runner · **COMPLETE**

Completed 2026-08-10 on branch `mig/p02-harness-ci` (from `migration` @ `7f156cd`).

### Environment

Same shims as P0/P1: `python3` resolved to a real 3.14.5 via a copy on `PATH` ahead of the
Windows Store stub; `JAVA_HOME=/c/Users/Admin/.jdks/corretto-21.0.12` and
`/c/Users/Admin/tools/apache-maven-3.9.16/bin` prepended for Maven. `rsync` was not needed
this phase (nothing in P2 calls it). **One new environment trap, found running this phase's
own deliverable, not in the plan or prior logs:** Python's `subprocess.run(["bash", ...])`
on this Windows machine resolves `bash` through Windows' own `CreateProcess` search order,
which can hit the WSL launcher stub at `C:\Windows\System32\bash.exe` instead of the
`PATH`-order git-bash that an interactive shell would find, and did so for the *first* such
call in a loop while later identical calls resolved correctly — non-deterministic per call,
not merely "wrong once". Symptom: a checker invoked this way returns the literal text
`<3>WSL (498 - Relay) ERROR: CreateProcessCommon:800: execvpe(/bin/bash) failed: No such
file or directory` as if it were the checker's own output. Fixed in
`scripts/local-verify.sh`'s new report section by resolving `bash`/`python3` once via
`shutil.which` (a pure `PATH`-walk, not `CreateProcess`) and always invoking the absolute
path. Recorded here so the next phase that shells out to `bash`/`python3` from *Python* on
this machine does not lose time to it — invocations from *bash itself* (structure-lint.sh,
verify-gates.sh, every checker's own internal calls) are unaffected; this is specific to
Python's `subprocess` module resolving a bare command name on Windows.

### Steps 1–4 — harness copy, ci.yml, checkstyle-test-fields.xml, mechanical CR-1

Copied all 28 files under `scaffold/scripts/lib/` (`cc64e49`) into a newly created
`scripts/lib/` — byte-identical, verified by `diff -q` against the standard checkout for
every file. Copied 18 of the standard's 22 runtime scripts into `scripts/` — also
byte-identical. **The other 4 needed a decision the plan states but does not spell out
mechanically:** the standard's `scripts/local-verify.sh`, `replit-build.sh`, `replit-env.sh`
and `replit-run.sh` share filenames with 3 of the 9 Do-not-touch project scripts (plus
`local-verify.sh`, the one deliberate exception). Copying all 22 as literally instructed
would have overwritten those 3 Do-not-touch files — confirmed they are not accidentally
identical: `diff` shows all three genuinely differ from the standard's versions. Resolved
per the Do-not-touch block's own instruction ("added alongside them, never over them"):
skipped copying the standard's `replit-build.sh` / `replit-env.sh` / `replit-run.sh`,
keeping the project's own; installed the standard's `local-verify.sh` separately in step 5
per its explicit exception. Net result directly under `scripts/` (not counting the new
`scripts/lib/` subdirectory): the original 9 Do-not-touch scripts (untouched) + 1 rewritten
`local-verify.sh` + 18 newly added scripts = 28 files, up from P0's baseline of 10.

Replaced `.github/workflows/ci.yml` with the standard's 5-job workflow (`static-checks`,
`unit-tests`, `integration-tests`, `frontend-checks`, `local-dev-dry-run`) verbatim, adding
only `continue-on-error: true` plus an explanatory comment on `static-checks`. Diffed against
`templates/generated-project/.github/workflows/ci.yml` (`cc64e49`): the *only* difference is
that one added block. **Nothing executes this file** — confirmed no CI service, no remote,
no runner is configured anywhere in this environment; it is installed and verified by
inspection only, per the plan.

Added `backend/config/checkstyle-test-fields.xml` — byte-identical to the standard's copy,
unwired (not yet referenced from any `pom.xml` or `checkstyle.xml`), matching the plan's
literal "Add" instruction with no wiring step.

**Step 4 — made the CR-1 exception mechanical.** The previous check in
`scripts/verify-gates.sh` was a blanket boolean: `! grep -RInE 'fetch(|axios|XMLHttpRequest'
frontend/src ... || fail ...` — true/false over the whole tree, blind to *which* file
triggered it and blind to a second violation appearing in an already-exempted file. Replaced
it with two explicit per-file assertions over the two named exemption files
(`useLessonMutations.ts`, `useMaterialMutations.ts`), each asserting
`grep -c 'await fetch(' <file> -eq 1`, plus an unchanged zero-tolerance scan over the rest of
`frontend/src` (now excluding just those two files) for any other raw
`fetch`/`axios`/`XMLHttpRequest`. Verified directly against fixture copies: count 1 → passes
silently; count 0 (fetch call deleted) → fails; count 2 (fetch call duplicated) → fails. Both
real call sites measured today: `useLessonMutations.ts` → 1, `useMaterialMutations.ts` → 1 —
matching the audit's "raw `fetch()` ×2" exactly (2 files, 1 call site each, not 2 in one
file). No other raw `fetch`/`axios`/`XMLHttpRequest` exists anywhere else in `frontend/src`
(confirmed by the same scan with both files excluded — zero matches).

### Step 5 — `local-verify.sh`, installed in report-only mode

Installed the standard's `scripts/local-verify.sh` (all its steps: Java version check,
`structure-lint.sh`, `verify-gates.sh`, coverage-phase announce, `mvn clean verify`,
frontend `lint && test && build`, `docker-compose.yml` config check), with a header comment
naming this a deliberate, temporary override, pointing at `docs/aiae-migration-plan.md` P15
step 5 (which removes it) and instructing "search for REPORT-ONLY" to find every changed
line. Every step now runs through a `run_step`/`record_step` wrapper that captures and
reports PASS/FAIL/SKIP instead of aborting (`set -uo pipefail`, no `-e`), and the script
always `exit 0`.

**Added, beyond the plan's literal text:** a new itemized report section that runs each of
the 28 `scripts/lib/` files independently (with the same explicit source-root arguments
`structure-lint.sh`/`verify-gates.sh` already use internally) and prints a count for each.
This was necessary, not optional — `structure-lint.sh` and `verify-gates.sh` both call a
`fail()` that does `exit 1` on the *first* violation, so run alone they can only ever report
one failing assertion per invocation (confirmed: this run's `structure-lint.sh` stopped at
"`backend/observability/` missing" — P4's job — and `verify-gates.sh` stopped at "README
must describe the app" — P3's job; neither reached anywhere near the presigned-upload check
or the other 26 checkers). Without the itemized section, "prints a count for each" of the 28
checkers would have been unmet by construction.

### Full 28-checker count table — the new working baseline

Measured by running `bash scripts/local-verify.sh` end to end (see Verification below). All
28 files under `scripts/lib/` are accounted for; 2 `.sh`+`.py` pairs share a row (matching
the P0 baseline table's own convention), giving 26 rows for 28 files.

| Checker | P0 baseline | P2 measured | |
|---|---|---|---|
| `check-thin-controllers.py` | 55 | **55** | ✅ |
| `check-api-validation-tests.py` | 91 constrained, 0 `isBadRequest` | **91, 0** | ✅ |
| `check-openapi-input-constraints.py` | 82 | **82** | ✅ |
| `check-openapi-documentation.sh` | 4 | **4** | ✅ |
| `check-service-contract-quality` (.sh+.py) | 25 | **25** | ✅ |
| `check-coverage-integrity.sh` | 15 | **14** | ❌ **differs — explained below** |
| `check-installed-documentation-links.py` | 6 (P0) / 0 (post-P1) | **0** | ✅ matches post-P1 |
| `check-frontend-ui-rules.sh` | 2222 | **2222** | ✅ |
| `check-production-static-methods.sh` | 8 | **8** | ✅ |
| `check-production-current-time.sh` | 3 | **3** | ✅ |
| `check-production-magic-values.sh` | 0 | **0** | ✅ |
| `check-production-manual-mapping.sh` | 0 | **0** | ✅ |
| `check-api-client-paths.sh` | 0 | **0 (pass)** | ✅ |
| `check-liquibase-preconditions` (.sh+.py) | 0 | **0 (pass)** | ✅ |
| `check-openapi-enums.sh` | 0 | **0** | ✅ |
| `check-openapi-strict-schemas.sh` | 0 | **0 (pass)** | ✅ |
| `check-architecture-overview.sh` | 1 — missing `Document status` | **1 (fail)** | ✅ unchanged — P3's job |
| `check-agent-surfaces.sh` | `handoff` (P0) / `active` (post-P1) | **`active`** | ✅ matches post-P1 |
| `check-maven-dependency-analysis.py` | invocation-dependent; "resolve in P2" | **1 (fail)** | **RESOLVED — see below** |
| `coverage-phase.sh` | — | n/a (library, not executed) | — |
| `liquibase_dependency_guard.py` | — | n/a (library, not executed) | — |
| `removal_transaction.py` | — | n/a (library, not executed) | — |
| `remove-cache-management.py` | — | n/a (mutation tool, **not executed**) | — |
| `remove-usage-logging.py` | — | n/a (mutation tool, **not executed**) | — |
| `rewrite-installed-documentation-paths.py` | — | n/a (utility, not executed) | — |
| `scan-production-java.py` | — | n/a (shared library, invoked by the `check-production-*` rows above, not run standalone) | — |

`remove-cache-management.py`/`remove-usage-logging.py` are destructive (they delete a
module) and were deliberately never executed, matching the guardrails' "Never run
`prepare-engineering-handoff.sh`" spirit — D-D keeps the telemetry module, and nothing in P2
runs a removal script against it.

**`check-coverage-integrity.sh`: 15 → 14, explained, not a regression.** Reproduced directly:
temporarily removing `.template-phase` and re-running the checker returns it to **15**;
restoring the file returns it to **14**. The 15th violation at P0 was
"`.template-phase` is missing" — true at P0 (before P1 created the file) and false now
(P1 created it with `mvp`). The other 14 are unchanged: the same 7 hand-written `jacoco`
excludes (`**/entities/**`, `**/*Entity.class`, `**/models/**`, `**/*Exception.class`,
`**/repositories/**`, `**/config/**`, `**/*_.class`), counted once in the `report` block and
once in the `check` block (§2.3's "both blocks carry the same 10 excludes"). This is an
already-logged P1 side effect surfacing for the first time because P2 is the first phase to
run this checker against the real tree; it is not something P2 changed.

**`check-maven-dependency-analysis.py` resolved, per P0's "resolve in P2" flag.** P0 found it
"invocation-dependent — reports 'no backend pom' when given `.`". Root cause: its `sys.argv[1]`
is the *backend directory*, not the repo root — `python3 check-maven-dependency-analysis.py .`
resolves to `./pom.xml` (no such file at repo root) and false-negatives to "passed (no
backend pom)". The correct invocation is `backend` as arg1 (exactly what
`verify-gates.sh:227` already does) or bare with `cwd` at the repo root. With the correct
invocation it fails honestly: `missing policy: backend/DEPENDENCY-ANALYSIS.md` — that file is
P3 step 2's deliverable, not P2's; this is an expected, correctly-surfaced gap, not a defect.

### `verify-gates.sh` / `structure-lint.sh` end-to-end (fail-fast, by design)

Both run as part of `local-verify.sh` and both fail — expected, and neither failure is new:

- `structure-lint.sh` stops at `backend/observability/ missing — copy canonical backend
  scaffold` (P4's job).
- `verify-gates.sh` stops at `README must describe the app and include API, Swagger UI, and
  OpenAPI YAML links` (P3's job) — it never reaches the presigned-upload check in this run,
  which is exactly why step 5 added the itemized per-checker report above.

### Product baseline re-confirmed (incidental to running `local-verify.sh`, not a new build step)

`mvn -f backend/pom.xml clean verify` (no `-Phandoff` — `.template-phase=mvp` from P1, but no
`mvp` Maven profile exists yet, so Maven printed `[WARNING] The requested profile "mvp" could
not be activated` and fell through to the same hardcoded 0.80 LINE default P0 measured):
**BUILD SUCCESS**, application module **507 tests, 0 failures, 44 skipped** — identical to
P0's baseline. `npm test`: **22 files, 78 tests, all pass** — identical to P0. `npm run
build`: succeeds, same ~674 kB `SimpleEditor` chunk warning P0 recorded. `local-verify.sh`'s
own frontend step reports **FAIL** because `frontend/package.json` defines no `"lint"`
script yet (`npm error Missing script: "lint"`) — a real, pre-existing gap
(`verify-gates.sh:124-125` already asserts one must exist), not introduced by P2 and not
closed by it; the plan's living-list table (§P3 step 1a) assigns ESLint/import-ordering work
to P12. Confirmed independently that `npm test` and `npm run build` both still pass when run
directly, since the `&&`-chained `lint` failure inside `local-verify.sh` prevented them from
running in that one combined step.

### Fixture manifest and scope

`.claude/.aiae-fixtures-manifest`: **80/80** matched before any change and again immediately
before this commit. Nothing under `.claude/**` or the four managed root files was touched.
`git status --porcelain` after staging shows exactly: 1 modified (`.github/workflows/ci.yml`),
1 rewritten (`scripts/local-verify.sh`), 1 new config file
(`backend/config/checkstyle-test-fields.xml`), 18 new runtime scripts directly under
`scripts/`, and 28 new files under the new `scripts/lib/` — 49 paths touched in total,
matching this commit's diffstat. The 9 Do-not-touch project scripts
(`backend-jacoco-coverage-targets.sh`, `local-dev-backend.sh`, `local-dev-frontend.sh`,
`replit-build.sh`, `replit-dev-backend.sh`, `replit-dev-frontend.sh`, `replit-env.sh`,
`replit-run.sh`, `report-bundle-size.sh`) show **zero** diff.

### Review — `production-code-review`

Scope: this phase's diff (`git diff migration...mig/p02-harness-ci`). Findings applied before
this commit rather than left open: removed a dead-code round-trip (a `mktemp`/JSON-write in
the new report section that was written but never read — deleted, the printed table is the
only output). No blocking or important findings remain. Evidence: every copied file
diffed byte-identical against the standard except the two files the plan names for
modification (`verify-gates.sh`, `local-verify.sh`); the presigned-upload mechanism tested at
counts 0/1/2 against fixture copies; the Windows `subprocess`/`bash` resolution defect found
and fixed with an explicit absolute-path resolution, verified by an isolated before/after run.

**Build** unchanged, per plan — no new build step introduced. Incidentally re-confirmed via
`local-verify.sh`: `mvn -f backend/pom.xml clean verify` → BUILD SUCCESS.
**Test** unchanged, per plan. Incidentally re-confirmed: backend 507/0/44 (application
module), frontend 78/78 — both identical to P0.
**Review** `production-code-review` — no blocking findings; one dead-code cleanup applied
pre-commit (see above).
**Verification** `bash scripts/local-verify.sh` runs end to end, prints a count/status for
all 28 `scripts/lib/` files plus `structure-lint.sh`/`verify-gates.sh`/backend/frontend/
docker-compose, and exits **0** (report-only, by design). Presigned-upload assertion: **1**
per exempted file (2 files), mechanically fails at 0 and at 2 (verified against fixture
copies). `ci.yml` diffed against the standard's file: identical except the one added
`continue-on-error: true` block. Fixture manifest: **80/80**, unchanged throughout.
**Rollback** `git revert` restores the 2-job workflow and the project's original
`local-verify.sh`; the 3 Do-not-touch replit scripts were never modified so nothing to
restore there.

### Things the plan did not spell out, resolved here

- The Do-not-touch block names 4 files that collide by filename with 4 of the 22 scaffold
  scripts step 1 says to copy (`local-verify.sh`, `replit-build.sh`, `replit-env.sh`,
  `replit-run.sh`). The block's own "added alongside them, never over them" phrasing is the
  resolution — skip copying the 3 replit-* collisions from the standard, keep the project's
  (confirmed genuinely different by `diff`, not accidentally identical), and handle
  `local-verify.sh` via its named step-5 exception. Not a plan defect once read this way, but
  worth recording since a literal "copy all 22" reading would have violated Do-not-touch.
- P0 flagged `check-maven-dependency-analysis.py` as "resolve in P2" without saying how.
  Resolved: pass `backend` as the explicit argument (matching `verify-gates.sh`'s own call),
  not the repo root.

### Nothing was declined in this phase

Every plan step (1–5) was executed, including the presigned-upload mechanical conversion and
the report-only `local-verify.sh`. The one addition beyond the literal text — the itemized
per-checker report section — was necessary to meet the phase's own stated Verification target
("prints a count for each" of the 28 checkers), for the fail-fast reason explained above, and
is documented rather than silently absorbed.

---

## P3 — Documentation, Replit contract, Context7 · **COMPLETE (five of six gates); one gate blocked by a Scope conflict, reported below**

Completed 2026-08-10 on branch `mig/p03-docs-replit` (from `migration` @ `4be55ef`).

### Environment

Same shims as P0–P2: `python3` resolved to a real 3.14.5 via a copy on `PATH`
ahead of the Windows Store stub; `JAVA_HOME=/c/Users/Admin/.jdks/corretto-21.0.12`
and `/c/Users/Admin/tools/apache-maven-3.9.16/bin` prepended for Maven. `rsync`
was not needed this phase.

### Step 1 — `docs/architecture-overview.md`, ADD ONLY — and a plan gap found by reading the checker directly

The plan states the checker's only violation is a missing `Document status`
section. **Reading `scripts/lib/check-architecture-overview.sh` directly
(as instructed) shows this is true only because the checker's
`required_sections` loop calls `fail()`, which `exit 1`s on the *first*
missing section** — the same fail-fast shape P2's own commentary on
`verify-gates.sh`/`structure-lint.sh` already names ("Red for one known
reason, and blind to everything downstream"). The loop actually requires
**twelve** exact `## <name>` sections, none of which existed before this
phase except by coincidence of wording:
`Document status`, `Product and system context`, `Product-specific evidence`,
`Runtime and deployment`, `Repository and module boundaries`,
`Primary runtime flows`, `API and security boundaries`,
`Data ownership and migrations`, `Caching and consistency`,
`External integrations`, `Observability and operations`,
`Decisions, constraints, and known risks`. Confirmed by running the checker
after adding only `Document status`: it then failed on the next section, and
so on. This is not a contradiction with **ADD ONLY** — every one of the
twelve was added as a new section with project-true content (see the diff);
nothing in `Identity`, `Backend modules`, `Deployment / runtime constraints`,
`Adopted standards the code has not caught up to yet`, or
`Known upstream defect` was removed, renamed, or overwritten. One collision
was caught and fixed before commit: an initial draft's
*Repository and module boundaries* table repeated the
`` | `backend/event-logging-to-db-feature` | `` row already present in
*Backend modules*, pushing `module_row_count` to 2 and failing the checker's
`-eq 1` assertion — removed in favor of a cross-reference sentence.

Facts added (all verified by direct file read, not carried from the plan or
audit): the five outbound integrations with representative classes; the
Clerk-only auth chain (`SecurityConfig`/`ClerkJwtClaimsValidator` — issuer,
audience, `azp`) plus `CompanyEmailDomainAuthorizationManager`
(`AUTH_ALLOWED_EMAIL_DOMAIN`) and `PermissionEvaluator`; the 18 Ehcache
regions (8 entity + 8 query + 2 infrastructure), enumerated by name from
`ehcache.xml`; the two scheduled jobs (`MaterialYoutubeBackfillJob`
`fixedDelay=300_000`, `AbandonedUploadCleanupJob` `fixedDelay=900_000`); the
presigned-upload path including the `.svg` content-type-override defect
(audit §2.4) and the `CLOUDFRONT_ENABLED` default-`false` gate; the MVP
usage-telemetry design (`UsageLoggingAspect` → `RoutingUsageEventSink` →
`UsageEventPersistenceService`, D-D); and the required **S3 bucket CORS
policy per environment** (audit §7.1.1). That policy could not be copied from
anywhere — no source document states exact origins — so it was derived from
the one piece of real evidence available: the app's own Spring CORS allow-list
default in `SecurityProperties.Cors.allowedOrigins`
(`https://*.replit.dev,https://*.repl.co,http://localhost:5173,http://localhost:5000`),
on the reasoning that the browser performing the presigned PUT is the same
origin already trusted for the API. Recorded as a reasoned derivation, not a
guess, with the source line cited in the document itself.

**Step 1a — the lag section made a living list, and one entry corrected
against P2's actual result.** All five entries reviewed:
- *Observability module* and *Distributed cache* — unchanged (P4, P6 not run).
- *Coverage phase tooling* — the existing text said "`scripts/` has no
  `lib/`"; that became false the moment P2 landed. Rewritten to state P2's
  actual, verified result (`scripts/lib/` and `.template-phase=mvp` exist;
  `check-coverage-integrity.sh` runs and reports 14) and what P9 still owes
  (an actual `-Pmvp` Maven profile — today `mvp` is not a real profile and
  Maven warns and falls through to the hardcoded 0.80 default).
- *Import ordering* — unchanged (P12 not run).
- *CSS tokens and units* — rewritten from "the code has not caught up" to a
  recorded **declined, permanent divergence**, citing D-A and the measured
  1499 raw `px` / 611 hex / 87 tokens, per the plan's explicit instruction.

### Step 2 — `backend/DEPENDENCY-ANALYSIS.md` — created, and a second plan/reality gap found

The plan and the audit both state the checker's only violation is the missing
policy file. **Verified false by running the checker before and after
creating an empty policy file:** before, it fails at
`missing policy: backend/DEPENDENCY-ANALYSIS.md`; immediately after creating
the file, it fails at a *different* check — `maven-dependency-plugin is
required` — because **`maven-dependency-plugin` does not exist anywhere in
`backend/pom.xml` or any child POM** (confirmed by `grep -rn
"dependency-plugin" backend/`: zero matches). The checker requires the plugin
activated in root `build/plugins` *and* an `analyze-only` execution bound to
`verify` under `pluginManagement` with `failOnWarning=true` and
`ignoreNonCompile=true` — none of which exists.

**This is a genuine Scope conflict, not something resolved by writing more
documentation.** P3's Scope lists `backend/DEPENDENCY-ANALYSIS.md` but not
`backend/pom.xml` — the pom is explicitly reserved elsewhere (P4 adds
`backend/observability` to it, P5 touches `backend/migrations/pom.xml`, P6
adds `backend/cache-management`). Wiring the plugin is a `backend/pom.xml`
edit, which this phase has no Scope grant to make. Per the task's own
instruction ("If the plan contradicts what you find, stop and report rather
than guessing") and the precedent already in this log (P1's first attempt
stopped at a Scope-adjacent contradiction rather than improvising a fix), this
was **not** done. `backend/DEPENDENCY-ANALYSIS.md` was still created and
filled in honestly for this project's actual dependency set — confirmed by
direct evidence that the scaffold's exact two-coordinate rationale applies
here too: `backend/service/pom.xml` already carries the
`event-logging-to-db-feature` dependency edge with a comment explaining why,
and a repo-wide grep for `LogUsage`/`UsageAttributes` under
`backend/service/src` and `backend/application/src` returns zero matches, so
the edge is genuinely unused-today, exactly the scaffold's documented
scenario. The document states plainly, in its own first paragraph, that the
enforcement mechanism is not yet wired and why, so it cannot be mistaken for
a claim that the gate is enforced. **This leaves gate #13 failing — expected,
and explained in the Verification section below** — and is a decision for the
technical owner: either widen P3's Scope to include this one plugin
activation in `backend/pom.xml`, or accept the gate stays red until whichever
phase next opens that file.

### Step 3 — README

Added `## What this is` (the checker's exact required heading — also
undocumented in the plan's step-3 text, which only mentioned the API/Swagger/
OpenAPI block; found the same way as the two gaps above, by reading
`scripts/verify-gates.sh:111-118` directly) directly above the existing intro
paragraph, and a new `## API` section with real, curl-verified links:
the OpenAPI YAML path and its runtime URL, and the Swagger UI path
(`springdoc.swagger-ui.url` in `application.yml:119-120`). Also fixed one
pre-existing factual error while in the file: the Layout section's backend
module list still read `` `domain`, `db`, ... `` — `backend/db` was renamed to
`backend/migrations` before P0; corrected to match `backend/pom.xml`'s actual
`<modules>` order. Left everything else exactly as it was, per the plan's
"leave the rest" instruction.

**Found but not fixed, reported instead of silently expanded:** the "Run
locally" section instructs `cp .env.local.example .env.local`, but
`.env.local.example` does not exist anywhere in the repository or its git
history (`git log --all -- .env.local.example` returns nothing). This makes
that quoted workflow non-functional as written. Creating the missing file is
outside this phase's Scope (README.md only, not a new top-level file); left
for a future phase or the technical owner to decide whether to add the file
or repoint the instructions at `.env.example`.

### Step 4 — `docker-compose.yaml` → `docker-compose.yml`

`git mv`, zero content diff (confirmed: `git diff --stat -M` shows only the
rename, no changed lines). Updated the three `docker-compose.yaml` references
in `README.md` to `.yml` in the same commit (`scripts/local-verify.sh`,
`scripts/materialize-project.sh`, and `scripts/test-materialize-project.sh`
already expected `.yml` — P2 had already written a comment in
`local-verify.sh` anticipating this exact rename, so no script needed
touching). `docker compose -f docker-compose.yml config` parses cleanly.

### Step 5 — `.replit` (C7)

Adopted the scaffold's inline-workflow form: both `[[workflows.workflow.tasks]]`
entries now embed the literal contract strings directly (`source
scripts/replit-env.sh`, `mvn -f backend/application/pom.xml ...
spring-boot:run`, `npm run generate:api` before `npm run dev`), and added
`onBoot = "bash scripts/setup-project.sh"` (absent before). Verified every
`verify_replit_file()` assertion individually by grep before running the full
gate suite (all ten passed). Preserved, confirmed unchanged: `modules =
["java-21", "nodejs-22", "postgresql-16"]`; `deploymentTarget = "gce"`; the
5000→80 port mapping; `SPRING_PROFILES_ACTIVE = "replit"`; and — the one the
brief called out by name — `VITE_CLERK_JWT_TEMPLATE = "aidigital-api"` in
`[env]`. `[deployment].build`/`.run` were left pointing at the project's own
`scripts/replit-build.sh`/`replit-run.sh` wrappers (Do-not-touch); the checker
does not require those inlined, only the workflow tasks.

**`onBoot` was not merely inspected — it was run locally, once, deliberately,
to check it is safe.** `scripts/setup-project.sh`'s own logic resolves
`SCAFFOLD` to an empty string whenever `backend/pom.xml` already exists
("Already materialized — no scaffold needed for cleanup steps"), which gates
every potentially destructive branch (`.gitignore` overwrite, runtime-script
reinstall). Ran it against this tree with `git status --porcelain` compared
before/after: **zero diff**, exit 0, logged only `cleaned .replit` (a no-op
`sed` pass — this project has no `python-*` module or Flask/Django/FastAPI
integration for it to strip) and `backend/ already present — skipping
materialize`. This is real local verification of the boot hook, not
inspection alone.

### Step 6 — Context7

`.mcp.json` present and unchanged, pointing at
`https://mcp.context7.com/mcp/oauth` (HTTP OAuth, no key to manage) — matches
the plan's description exactly. **Not verified reachable in this session**:
no Context7 MCP tool was exposed to this agent (`ToolSearch` for "context7"
returned no match), which reads as this session's own OAuth/MCP wiring, not a
repository defect — nothing under `.mcp.json` or the repo was changed to
cause it. Recorded as unverified rather than assumed working.

### Local run — beyond inspection, matching the plan's Verification block

Since P3 changes how Replit boots the app, more than a gate count was
gathered. With Postgres up via `docker compose -f docker-compose.yml up -d
postgres` (container reported `healthy`), ran the **exact commands now
embedded in `.replit`** directly (not the wrapper scripts, to verify the
literal contract strings themselves): `mvn -f backend/pom.xml -DskipTests
-Dskip.frontend=true install` (BUILD SUCCESS, 25s) then `mvn -f
backend/application/pom.xml ... spring-boot:run` in the background, with
`AUTH_JWKS_URI` set to a syntactically-valid placeholder (never fetched
unless a real token is decoded — no Clerk credential requested or used).
Result: **`Started Application in 7.634 seconds`**, Liquibase reported `Run:
0 / Previously run: 14` against the empty-then-migrated local DB, all 18
Ehcache regions registered by name. Confirmed by `curl`:

| Endpoint | HTTP status |
|---|---|
| `/actuator/health` | 200 |
| `/api/v1/specs/openapi.yaml` | 200 |
| `/swagger-ui/index.html` | 200 |
| `/` (SPA welcome page) | 200 |

Then `npm run generate:api` against the live backend (succeeded, 104.5ms,
same command `.replit` now embeds) and `npm run dev -- --host 0.0.0.0 --port
5173` in the background: Vite ready in 160ms, `curl http://localhost:5173/`
returned 200 with `<title>AIAE Onboarding platform</title>` in the body —
the real app shell, not a placeholder. Both processes stopped by PID
afterward (`Stop-Process` on the PIDs bound to ports 5000/5173, confirmed no
listener remained); `docker compose -f docker-compose.yml down -v` removed
the container, network, and volume. `git status --porcelain` before and
after this entire local run was compared and is identical except for the
intended file changes below — the run left no residue.

**What this does and does not prove.** It proves the backend boots, applies
Liquibase idempotently against an already-migrated schema, serves the
documented API/Swagger/OpenAPI endpoints, and that the frontend dev server
serves the real SPA — using the literal command strings now embedded in
`.replit`. It does **not** prove a Replit deploy boots from this contract:
Replit's actual `onBoot`/build/run execution, its network/DNS environment,
and Clerk token exchange against a real publishable key were never exercised,
per the plan's own instruction that the deploy path stays unverified by this
migration. That gap is explicit here, not silent.

`bash scripts/local-verify.sh` was also run end-to-end (report-only, exits 0
by design): `check-architecture-overview.sh` → `passed (mvp)`;
`docker-compose.yml config` → PASS (previously SKIP, per P2's own comment
anticipating this exact rename); backend `mvn clean verify` → BUILD SUCCESS,
**507 tests, 0 failures, 44 skipped** — identical to the P0/P2 baseline, no
regression; frontend step still fails on the pre-existing missing `"lint"`
script (P12's job, unrelated to P3, unchanged from P2's baseline).

### Fixture manifest and scope

`.claude/.aiae-fixtures-manifest`: **80/80** matched, re-validated immediately
before this commit. `git status --porcelain` shows exactly the Scope-listed
paths and nothing else: `.replit` (M), `README.md` (M),
`docker-compose.yaml` → `docker-compose.yml` (R, no content change),
`docs/architecture-overview.md` (M), `backend/DEPENDENCY-ANALYSIS.md` (new).
`backend/pom.xml` and every other backend/frontend source file: zero diff.

### Review — `production-code-review`

Scope: this phase's diff (`.replit` and the docs). No blocking findings. One
finding applied before commit: an early draft of *Repository and module
boundaries* duplicated the `event-logging-to-db-feature` module-table row,
which would have made `check-architecture-overview.sh`'s
`module_row_count` assertion fail at 2 — removed in favor of a
cross-reference sentence (see Step 1 above). Confirmed by diff that no file
outside Scope was touched and that `onBoot`'s local run left no residue.

### Verification — gate movement, 22 → 17 (five of six intended, one explained)

`bash scripts/verify-gates.sh`, full failure list, before (P2 baseline) →
after:

| # (before) | Assertion | After P3 |
|---|---|---|
| 1 | README must describe the app and include API, Swagger UI, and OpenAPI YAML links | **cleared** |
| 2 | check-architecture-overview.sh reported violations | **cleared** — `passed (mvp)` |
| 3 | .replit: backend workflow must use backend/application/pom.xml | **cleared** |
| 4 | .replit: frontend workflow must run npm run generate:api before Vite | **cleared** |
| 5 | .replit: workflows must source scripts/replit-env.sh | **cleared** |
| 6 | frontend/package.json must pin firewall-approved vitest ^3.2.6 | unchanged (P0 baseline, not P3's job) |
| 7 | frontend/package.json must define a "lint" script running eslint | unchanged (P12's job) |
| 8 | frontend/eslint.config.js is required | unchanged (P12's job) |
| 9 | Frontend must not use a left side menu/sidebar | unchanged — carried red, §6/CR-2 |
| 10 | check-openapi-documentation.sh reported violations | unchanged |
| 11 | check-openapi-input-constraints.py reported violations | unchanged |
| 12 | check-api-validation-tests.py reported violations | unchanged |
| 13 | **check-maven-dependency-analysis.py reported violations** | **NOT cleared — see Step 2 above; now fails at "maven-dependency-plugin is required" instead of "missing policy"** |
| 14 | Every Maven submodule must declare Lombok: migrations | unchanged (P5's job) |
| 15 | Logbook DefaultSink must be built with formatter + writer | unchanged |
| 16 | Production/Replit Logbook must use metadata-only WithoutBodyStrategy | unchanged |
| 17 | service source must not import web/security/JWT/servlet APIs | unchanged |
| 18 | check-frontend-ui-rules.sh reported violations | unchanged — carried red, §6 |
| 19 | check-production-static-methods.sh reported violations | unchanged |
| 20 | check-production-current-time.sh reported violations | unchanged (P8's job) |
| 21 | check-service-contract-quality.sh reported violations | unchanged |
| 22 | check-coverage-integrity.sh reported violations | unchanged |

**Result: 22 → 17, not the predicted 22 → 16.** The gap is exactly gate #13,
explained in Step 2: closing it honestly requires an edit to
`backend/pom.xml`, which is outside this phase's Scope. All sixteen gates the
brief expected to stay untouched did stay untouched — confirmed by the
before/after table above, item by item; nothing moved that was not supposed
to. The four carried red assertions (presigned upload = 1,
`check-frontend-ui-rules.sh` = 2222, sidebar fails, `structure-lint`
`changes/0001-usage-events.xml` fails) were reconfirmed unchanged where this
phase's tools touched them (`check-frontend-ui-rules.sh` still 2222 per
`local-verify.sh`'s own run; presigned-upload count reconfirmed at exactly 1
per file by direct grep).

**Build** `mvn -f backend/pom.xml clean verify` → BUILD SUCCESS, 507 tests,
0 failures, 44 skipped (via `local-verify.sh`, identical to P0/P2 baseline).
Also `mvn -f backend/pom.xml -DskipTests install` + `mvn -f
backend/application/pom.xml spring-boot:run` (the exact commands now in
`.replit`) → app started in 7.634s, confirmed serving on `:5000`.
**Test** `docker compose -f docker-compose.yml config` → parses (PASS).
`npm run generate:api` against the live backend → succeeded, 104.5ms.
`npm run dev -- --host 0.0.0.0 --port 5173` → Vite ready in 160ms, SPA served
(curl-confirmed, real `<title>`, not a placeholder).
**Review** `production-code-review` — no blocking findings; one draft-stage
duplicate-row finding caught and fixed pre-commit (see above).
**Verification** `verify-gates.sh`: **22 → 17** (five of the six targeted
gates cleared; gate #13 blocked by a Scope conflict, reported above, not
silently left unexplained). `check-architecture-overview.sh`: 1 → 0.
`check-maven-dependency-analysis.py`: unchanged (still fails, different
message). `verify_replit_file()`: all ten assertions individually confirmed
passing by grep before the full-suite run. Fixture manifest: 80/80,
unchanged throughout. **The Replit deploy path remains unverified by this
migration** — everything above is a local run of the same literal commands,
not an actual Replit boot; whoever deploys next must check it.
**Rollback** `git revert` on this phase's commit. Out-of-repo: none —
`onBoot`'s one local run left no residue (confirmed by `git status
--porcelain` diff), and the Postgres container/volume/network created for the
local run were torn down (`docker compose down -v`) before commit.

### What was declined, and what remains a decision for the technical owner

- **Gate #13 (`check-maven-dependency-analysis.py`) was not forced green.**
  Wiring `maven-dependency-plugin` into `backend/pom.xml` would close it, but
  that file is outside P3's Scope. `backend/DEPENDENCY-ANALYSIS.md` was still
  written, honestly, as the policy the plugin will enforce once a future
  phase (or an explicit Scope widening of this one) wires it.
- **`.env.local.example` was not created.** README's "Run locally" section
  references a file that has never existed in this repository. Creating it is
  a new file outside README.md's Scope grant; flagged for the technical owner
  instead of silently added.
- Everything else the plan's Step 1–6 list asked for was done, including the
  one-time local `onBoot` run and the full local backend+frontend serve,
  which the plan's Verification block asks for but does not make mandatory
  wording ("plus... a local run that serves the SPA").

---

## P4 — `backend/observability` · **COMPLETE**

Completed 2026-08-10 on branch `mig/p04-observability` (from `migration` @ `7924b8a`).

### Environment

Same shims as P0–P3: `python3` resolved to a real 3.14.5 via a copy on `PATH`
ahead of the Windows Store stub; `JAVA_HOME=/c/Users/Admin/.jdks/corretto-21.0.12`
and `/c/Users/Admin/tools/apache-maven-3.9.16/bin` prepended for Maven, both
verified (`python3 -c "import sys; print(sys.version)"` → `3.14.5`; `mvn -v` →
Java `21.0.12`). Fixture manifest re-validated **80/80** before any change and
again immediately before this commit.

### Step 5 checked first, per the brief — already done, verified by reading, not edited

Read `.claude/rules/00-backend-hard-rules.md` and `.claude/rules/10-architecture.md`
in full before touching anything. Both already state the target shape: the
former reads *"Reusable outbound metrics live in `backend/observability`, which
owns `ExternalClientMetricsInterceptor` and `ExternalCallTimer`"*; the latter
reads *"SDK-managed calls use the reusable `ExternalCallTimer` from
`backend/observability`."* Neither file was written to. `git diff` for this
commit touches neither path, confirmed below.

### Steps 1–4 — the module, the move, the dependency edges, the byte-identical registrations

1. Created `backend/observability/pom.xml` (Lombok declared; deps: Lombok,
   `spring-boot-starter`, `spring-web`, `micrometer-core`, test starter) and
   added `<module>observability</module>` plus a `dependencyManagement` entry
   to `backend/pom.xml`, matching the existing pattern for `external-services`.
2. Moved (via `git mv`, history preserved) `ExternalCallTimer.java` and
   `ExternalClientMetricsInterceptor.java` from
   `backend/external-services/.../external/common/http/` to
   `backend/observability/src/main/java/com/aidigital/aionboarding/observability/external/`.
   Package declaration updated in both. `ExternalClientMetricsInterceptor` was
   package-private (`class ExternalClientMetricsInterceptor`,
   `@RequiredArgsConstructor(access = AccessLevel.PACKAGE)`) because its only
   caller, `PooledRestClientFactory`, shared its package; now that the caller
   is in a different module, both the class and its generated constructor
   were widened to `public` (the minimum change needed to compile across the
   module boundary) — no other visibility, field, or method changed.
   `ExternalCallTimer` was already `public` and needed no visibility change.
   `classifyOutcome` stays package-private per `.claude/rules/00-backend-hard-rules.md`'s
   "no private methods" rule (unchanged from before the move).
3. Added the `observability` dependency to `backend/external-services/pom.xml`
   and `backend/application/pom.xml` (comment cites the hard rule). Updated
   imports in the four call sites that reference the moved classes:
   `PooledRestClientFactory.java` (new import for
   `ExternalClientMetricsInterceptor`), `StorageClientImpl.java`,
   `StorageConfig.java`, and the test `StorageClientImplTest.java` (all three
   for `ExternalCallTimer`). Every edit is import-line-only, kept in
   alphabetical position; no method body changed.
4. **Verified the four `PooledRestClientFactory` registration expressions are
   byte-identical**, at their original lines 68–69 and 113–114 (now 69–70 and
   114–115, shifted by exactly the one inserted import line):
   ```
   .requestInterceptor(new ExternalClientMetricsInterceptor(name, meterRegistry))
   .requestInterceptor(new LogbookClientHttpRequestInterceptor(logbook))
   ```
   (both factory methods). `git diff` on that file shows exactly one added
   import line and nothing else.

### Do-not-touch list confirmed undisturbed

Grepped for all six do-not-move classes
(`PerformanceMetricsFilter`, `RequestAuthenticationCacheFilter`,
`IntegrationHealthIndicator`, `ByteCountingResponseWrapper`,
`CountingServletOutputStream`, `MetadataOnlyHttpLogFormatter`): all six remain
under `backend/application`, none touched. `git status --porcelain` for this
commit lists exactly: `backend/pom.xml`, `backend/application/pom.xml`,
`backend/external-services/pom.xml`, `backend/observability/pom.xml` (new),
the two moved files (renamed, tracked by git as `R`), and the four import-only
edits (`PooledRestClientFactory.java`, `StorageClientImpl.java`,
`StorageConfig.java`, `StorageClientImplTest.java`). Nothing under
`.claude/**` or the four managed root files appears in the diff.

### Metric names — confirmed unchanged by reading, not by a live `/actuator/prometheus`

No `/actuator/prometheus` endpoint was reachable in this environment, so the
before/after comparison below is a direct code diff, not a live scrape — recorded
honestly rather than claimed as verified in production. `git diff` on both moved
files shows only the package line, `ExternalClientMetricsInterceptor`'s
visibility, and (for the interceptor) the removed unused `AccessLevel` import —
no change to any string literal. Both emitted timer names, tag keys, and tag
values are identical before → after:

| Class | Timer name | Tags |
|---|---|---|
| `ExternalClientMetricsInterceptor` | `external.client.requests` | `client`, `outcome` (`success`/`client_error`/`server_error`/`io_error`) |
| `ExternalCallTimer` | `external.client.requests` | `client`, `operation`, `outcome` (`success`/`error`) |

**Gap found, not fixed, reported instead of silently absorbed:** neither class
had a dedicated unit test before this phase (their only exercise was indirect —
`PooledRestClientFactoryTest` constructs `ExternalClientMetricsInterceptor` via
`factory.createClient(...)` but never issues a request through it, and
`StorageClientImplTest` mocks `ExternalCallTimer` — so `intercept()`,
`classifyOutcome()`, and `record()` had no line coverage from either test
before or after the move). This phase did not add one — writing new tests
purely to raise coverage is P9/P15's assigned ground (D-C), and inventing an
assertion here to "prove" the metric name would be exactly the
assertion-for-its-own-sake pattern the guardrails forbid. So the claim above is
a code-diff comparison, stated as such, not a test-backed one.

**A related side effect, also found and reported, not fixed:** with zero test
classes under `backend/observability/src/test`, `jacoco:check` printed
*"Skipping JaCoCo execution due to missing execution data file"* for that
module and passed — not because 0.80 LINE was met, but because the JaCoCo
Maven plugin no-ops entirely when no `jacoco.exec` exists. This is a
pre-existing behavior of the plugin, not something this phase introduced, and
the two moved classes had the same effectively-uncovered status inside the
37-class `external-services` bundle before the move (masked by that module's
aggregate 0.8066 LINE ratio, per the P0 baseline). Isolating them into their
own module made the gap visible instead of averaged away, but did not create
it. Filed here for whichever phase next touches coverage (P9/P15); not acted
on, per this phase's Scope.

### Review — `backend-rule-review`

Scope: this phase's diff only. Findings: none blocking. Confirmed against
`.claude/rules/00-backend-hard-rules.md` and `10-architecture.md`: the new
module declares Lombok (hard rule for every submodule); the four registration
literals are untouched; `ExternalClientMetricsInterceptor`/`ExternalCallTimer`
now exist only under `backend/observability` (grep across
`backend/{application,service,domain,external-services}/src/main/java`
returns zero matches for `class\s+(ExternalClientMetricsInterceptor|ExternalCallTimer)\b`);
`backend/observability/pom.xml` declares no dependency on
`application`/`service`/`domain`/`migrations`/`external-services` (confirmed by
grep — it depends only on Spring Boot/Micrometer/Lombok), keeping it a true
leaf module as `structure-lint.sh`'s own reusability assertion requires.

### Verification

**`bash scripts/structure-lint.sh`: 23 → 16.** Exactly the seven predicted
assertions cleared, confirmed item-by-item against the before/after failure
lists:

| # (before) | Assertion | After |
|---|---|---|
| 1 | `backend/observability/` missing | **cleared** |
| 2 | `backend/observability/pom.xml` missing | **cleared** |
| 3 | `backend/pom.xml` must list required module: observability | **cleared** |
| 6 | `application/pom.xml` must attach the reusable external metrics module | **cleared** |
| 7 | observability must own `ExternalClientMetricsInterceptor` | **cleared** |
| 8 | observability must own `ExternalCallTimer` | **cleared** |
| 9 | `ExternalClientMetricsInterceptor`/`ExternalCallTimer` belong only in `backend/observability` | **cleared** |

All sixteen assertions the brief did not target (the `event-logging`/usage-events
carried failure, `ehcache.xml`/cache-management, `migrations` Lombok, the 55
thin-controller violations, the 10 manual-`new *V1()` mapper findings, and the
`Map<String,Object>` service-interface finding) reconfirmed unchanged,
item-by-item, before → after — no unrelated movement in either direction.

**`bash scripts/verify-gates.sh`: 17 → 17, unchanged, as required.** The two
assertions concerning the interceptor registrations
(`grep -Fq 'new ExternalClientMetricsInterceptor(name, meterRegistry)'` and
`grep -Fq 'new LogbookClientHttpRequestInterceptor(logbook)'` against
`PooledRestClientFactory.java`) were passing before this phase and confirmed
still passing after — neither appears in the 17-item failure list, before or
after. All four carried red assertions reconfirmed unchanged: presigned-upload
`await fetch(` count is exactly **1** per file
(`useLessonMutations.ts`, `useMaterialMutations.ts`); the sidebar assertion
still fails; `check-frontend-ui-rules.sh` untouched by this backend-only phase
(not re-run — no frontend file in this diff); `structure-lint`'s
`changes/0001-usage-events.xml` assertion still fails (item 1 in the post-move
16-failure list).

**`mvn -f backend/pom.xml clean verify`: BUILD SUCCESS, 507 tests, 0 failures,
44 skipped** — identical to the P0/P2/P3 baseline (`application` module, the
figure this log has tracked since P0). Per-module reactor results, all green,
none regressed: `domain` 2/0/0, `event-logging-to-db-feature` 48/0/0,
`observability` 0 tests (new, empty — see the coverage-gap note above),
`external-services` **174/0/0** (down from 37 to 35 analyzed classes —
consistent with exactly the two moved classes leaving; `jacoco-check` still
reports "All coverage checks have been met"), `service` 1131/0/0, `application`
507/0/44 (`jacoco-check`: "All coverage checks have been met", 118 classes).
Total wall time 2m35s.

**Fixture manifest:** 80/80 before and after; `git diff` touches neither
`.claude/**` nor any of the four managed root files.

**Build** `mvn -f backend/pom.xml clean install -DskipTests` → BUILD SUCCESS
(compile-only check run first); `mvn -f backend/pom.xml clean verify` → BUILD
SUCCESS, 507 tests, 0 failures, 44 skipped (application module, matching
baseline exactly); no other module regressed (all green, counts above).
**Test** Full backend suite via `clean verify` (above). No
`/actuator/prometheus` scrape was possible in this environment — metric-name
equivalence is a code-diff comparison, stated as such (see above), not a live
verification.
**Review** `backend-rule-review` — no blocking findings; leaf-module dependency
shape and the four registration literals independently confirmed by grep (see
above).
**Verification** `structure-lint.sh`: **23 → 16** (exactly the seven predicted
assertions, confirmed item-by-item). `verify-gates.sh`: **17 → 17**, unchanged,
including the two interceptor-registration assertions, which continue to pass.
Fixture manifest: **80/80**, unchanged throughout. `.claude/rules/00-backend-hard-rules.md`
and `10-architecture.md`: read, confirmed already correct, not edited.
**Rollback** `git revert` on this phase's commit. No out-of-repo action.

### What was declined, and what is reported rather than fixed

- **No unit test was added for `ExternalCallTimer`/`ExternalClientMetricsInterceptor`.**
  Neither had one before this phase; adding one now would be coverage work
  reserved for P9/P15 under D-C, and an assertion added only to justify a
  claim in this log would be the exact "assertion-free/gap-filling" pattern
  `.claude/rules/20-tests.md` and the guardrails forbid. Reported as a gap
  instead (see "Metric names" above), not silently fixed or silently ignored.
- **The `jacoco:check` no-op on a zero-test module was not worked around.**
  It is a pre-existing plugin behavior, not introduced by this phase, and
  "fixing" it (e.g. by adding a throwaway test) would be the same
  assertion-for-its-own-sake problem. Filed for a future coverage phase.
- **Movement was exactly the predicted seven, in both gates.** No unexplained
  over- or under-shoot to account for.

---

## P5 — `backend/migrations` completion · **COMPLETE**

Completed 2026-08-10 on branch `mig/p05-migrations` (from `migration` @ `4789d1d`).

### Environment

Same shims as P0–P4: `python3` resolved to a real 3.14.5 via a copy on `PATH` ahead of the
Windows Store stub (`python3 -c "import sys; print(sys.version)"` → `3.14.5`);
`JAVA_HOME=/c/Users/Admin/.jdks/corretto-21.0.12` and
`/c/Users/Admin/tools/apache-maven-3.9.16/bin` prepended (`mvn -v` → Java `21.0.12`). Fixture
manifest re-validated **80/80** before any change and again immediately before this commit.

### Preconditions re-confirmed, not re-litigated

P0 step 3a's production query (§2.1) already returned `db/changelog/1.0.0/db.version-master.xml`
— the classpath path, carrying neither `backend/db` nor `backend/migrations`. R1 does not exist.
This phase treats that as settled and does no further production access, per D-B. What follows
is the mechanical work §2.1/§2.7 describe as safe.

### Step 1 — Lombok added to `backend/migrations/pom.xml`

Added the managed `org.projectlombok:lombok` dependency, unversioned, matching the exact
pattern already used by `domain` and `event-logging-to-db-feature` (both declare it the same
way, version resolved from the parent's `dependencyManagement`). This was `structure-lint`'s
assertion 3 and `verify-gates`'s assertion 9 (see Verification below) — the one genuine defect
left over from the module rename, exactly as the plan states.

### Step 2 — stale POM header comment fixed

`backend/migrations/pom.xml`'s header comment read *"`application` depends on `db` so
changelogs land on the classpath..."* — corrected to `migrations`, matching the module's actual
`artifactId` (unchanged since before P0).

### Step 3 — `.claude/rules/12-database.md` verified, not written

Read in full before touching anything, per the trap. It already reads
*"New schema changes go through Liquibase under `backend/migrations/src/main/resources/db/changelog`"*
in its rule body, and its own `paths:` frontmatter scopes to `backend/migrations/**/*` — not
`backend/db/**`. The fixture install had already restored the AIAE text, exactly as P4's
equivalent step found for the observability rule files. **Not edited.** `git diff` for this
commit shows zero lines touched in this file.

### Step 4 — warning comment added to the root changelog

Added an XML comment at the top of (the then-still-named) `db.root-master.xml` stating: filename
recorded by Liquibase is the classpath path from `spring.liquibase.change-log`; renaming the
`db/changelog/` **directory** re-runs all 14 changesets against a populated schema; the Maven
module name is not part of that path; this **file** may be renamed safely because it declares
zero changesets. Added before the rename in step 5, per the plan's own order.

### Step 5 — the file renamed, the directory untouched, every reference updated

`git mv backend/migrations/src/main/resources/db/changelog/db.root-master.xml
backend/migrations/src/main/resources/db/changelog/db.changelog-master.xml`. The `db/changelog/`
**directory** was not touched — confirmed by `git status --porcelain`, which shows exactly one
`R` (rename) and no directory-level change. `backend/migrations/src/main/resources/db/changelog/1.0.0/`
and every `sqlFile` path inside it are untouched (confirmed by `git diff` — zero lines in that
subtree).

Grepped `db.root-master` across the whole repository before and after. Two references sit
inside `backend/**` and are load-bearing:

| Reference | Action |
|---|---|
| `backend/application/src/main/resources/application.yml:75` — `spring.liquibase.change-log: classpath:db/changelog/db.root-master.xml` | Updated to `db.changelog-master.xml` |
| `backend/application/src/test/java/.../db/LiquibaseChangelogSmokeTest.java:12` — Javadoc `{@code db.root-master.xml}` | Updated to `db.changelog-master.xml` (comment only, no behavior change) |

**Scope gap found and resolved in favor of correctness, not silently.** P5's own Scope block
lists only `backend/migrations/pom.xml`, `.claude/rules/12-database.md`, and
`backend/migrations/src/main/resources/db/changelog/` (comment only) — it does not name
`application.yml` or the test file. But step 5's own instruction text is explicit and
unconditional: *"update `spring.liquibase.change-log` in
`backend/application/src/main/resources/application.yml:75` to match. Grep for `db.root-master`
across the repository first and update every reference."* Leaving `application.yml` unchanged
would not merely miss a citation — it would break Liquibase resolution outright (verified: see
the first, failed boot attempt below). Both edits were made; this is the same class of
Scope-list omission P3 and P4 each found and reported once. Test-file comment included because
"every reference" does not carve out test sources, and leaving a stale filename in a Javadoc that
exists specifically to describe this changelog would misinform the next reader.

**Left untouched, and reported rather than silently expanded to cover:** four remaining
`db.root-master` references, all in documentation outside P5's Scope grant and each covering a
different kind of drift now introduced by this rename:

| File | Nature |
|---|---|
| `docs/architecture-overview.md:36,212` | States the classpath value as current fact; now stale. P3's file, not P5's; not fixed here. |
| `docs/aiae-migration-plan.md` (multiple) | The plan's own narrative describing the rename it instructs; historical/authoritative text, not this phase's to rewrite. |
| `docs/aiae-audit.md:124` | Historical audit snapshot of the pre-migration state; correctly still describes what was true then. |
| `docs/aiae-template-change-requests.md:356` | CR text already sent upstream describing the same layout; not retroactively edited. |

`docs/architecture-overview.md`'s drift is a real, reportable consequence of this phase and is
flagged here for whoever next opens that file (no phase currently owns re-touching it).

Confirmed separately: none of the 22 runtime scripts or 28 `scripts/lib/` checkers needed any
change — `structure-lint.sh`, `remove-cache-management.py`, `remove-usage-logging.py`,
`strip-scaffold-samples.sh`, and the two `test-*.sh` harnesses already hardcode
`db.changelog-master.xml` (confirmed by grep, none reference `db.root-master`). This is §2.7's
premise made visible: the harness was already written assuming this rename would happen.

### An environment trap found while proving step 5, not caused by it

The first attempt to boot the app against a live Postgres (`mvn -f backend/application/pom.xml
spring-boot:run`, no `-am`) failed with `liquibase.exception.ChangeLogParseException: ERROR:
The file 'classpath:db/changelog/db.changelog-master.xml' was not found.` — not because the
rename was wrong, but because that single-module invocation resolves the `migrations` module
dependency from `~/.m2/repository`, which still held the **pre-rename** jar (`db.root-master.xml`
inside it, timestamped from an earlier phase's `install`). `mvn -f backend/pom.xml clean verify`
(the phase's own Build/Test command) never touches `~/.m2` — it builds the reactor in place —
so this trap is specific to any *out-of-reactor* smoke test, not to the phase's required build.
Fixed by `mvn -f backend/pom.xml clean install -DskipTests` to refresh the local repository with
the renamed module's jar (confirmed by `unzip -l` before/after: `db.root-master.xml` →
`db.changelog-master.xml` inside the cached jar), then re-running the single-module boot. Not a
defect in this phase's change; recorded so the next person proving a resource-path rename this
way does not lose time to a stale `~/.m2` cache.

### Step 6 — verified against the P0 R1 result, plus an independent green-field apply

**Comparison the plan asks for.** P0's production query (§2.1, confirmed 2026-08-10) returned
all 14 rows with `filename = db/changelog/1.0.0/db.version-master.xml`. This phase does not
re-run that query against production (D-B: no deploy, no restore) — it instead booted the
renamed module against a fresh, empty local Postgres (`docker compose -f docker-compose.yml up
-d postgres`, confirmed empty via `psql -c '\dt'` → "Did not find any relations" before the app
started) and read the same table directly:

```
SELECT orderexecuted, id, author, filename, exectype FROM databasechangelog ORDER BY orderexecuted;
```

All 14 rows: `filename = db/changelog/1.0.0/db.version-master.xml`, `exectype = EXECUTED`,
`author = aionboarding`, ids `1.0.0-usage-events` through `1.0.0-pending-uploads` in the same
order as `1.0.0/db.version-master.xml`'s own changeset order. **Byte-identical to the production
value P0 recorded.** Neither `backend/db`, `backend/migrations`, `db.root-master`, nor
`db.changelog-master` appears anywhere in the `filename` column — confirming §2.1/§2.7
empirically on the post-rename module, not merely by re-reading the same production row P0
already read.

**Green-field apply (Build/Test block's Testcontainers requirement, executed a different way and
reported honestly).** `LiquibaseChangelogSmokeTest`'s own Testcontainers instance could not reach
Docker in this sandbox — `DockerClientFactory`/`NpipeSocketClientProviderStrategy` failed with a
400 from the named pipe, even though the Docker CLI itself works fine (`docker ps`, `docker
compose up` both succeed) — so the test **skipped**, not passed (`Tests run: 1, Skipped: 1`, both
in the full `clean verify` run and in an isolated re-run). This is not the same thing as proof,
and is reported as such rather than counted as one. Independent proof was gathered instead: with
the same empty Postgres, `mvn -f backend/application/pom.xml spring-boot:run` (after the `~/.m2`
fix above) produced

```
Run:                          14
Previously run:               0
Total change sets:           14
Liquibase: Update has been successful. Rows affected: 41
```

— a real green-field apply against the renamed classpath resource, from an empty schema, outside
any test framework. A second boot against the now-migrated schema showed the idempotent path:
`Run: 0 / Previously run: 14`, `Started Application in 7.74 seconds`, and `curl` confirmed
`/actuator/health` → 200, `/api/v1/specs/openapi.yaml` → 200, `/swagger-ui/index.html` → 200,
`/` (SPA) → 200. Both Java processes were killed by image name afterward and
`docker compose -f docker-compose.yml down -v` removed the container, network, and volume;
`git status --porcelain` before and after this entire local run is identical except for the
intended file changes (confirmed above) — no residue.

### Step 6 (second item) — the one sensitive changeset, watched, not touched

`1.0.0-jsonb-array-contains-ci-function` (lines 107–118 of `1.0.0/db.version-master.xml`) is the
only changeset among the 14 that (a) uses an `sqlCheck` precondition rather than table-existence,
and (b) carries an explicit `<rollback>`. Confirmed by direct read before and after this phase's
edits: neither its content, its precondition, nor its rollback changed — `git diff` on
`1.0.0/db.version-master.xml` is empty (zero lines), matching the plan's Do-not-touch instruction
exactly.

### Review — `backend-rule-review`

Scope: this phase's diff only. Findings: none blocking. Confirmed: `backend/migrations/pom.xml`
now declares Lombok, matching every other submodule's pattern (no explicit version, resolved from
`dependencyManagement`); the changelog **directory** name is unchanged (`git status` shows only a
file-level rename, `R`, never a directory move); the 14 changesets, their ids, authors, and
`sqlFile` paths are byte-for-byte unchanged (`git diff` on `1.0.0/db.version-master.xml` and
`1.0.0/sql/*.sql` is empty); the `.claude/rules/12-database.md` diff is empty. The two edits
outside the literal Scope list (`application.yml`, the test Javadoc) were reviewed against step
5's own explicit text and against the necessity that the build actually resolve the renamed
resource — both are documented above rather than silently absorbed.

### Verification

**`bash scripts/structure-lint.sh`: 16 → 15.** Exactly the one predicted assertion cleared,
confirmed item-by-item against the before/after failure lists (saved in full): assertion 3,
`backend/migrations/pom.xml must declare Lombok`, **cleared**. All fifteen assertions the brief
did not target (the carried `changes/0001-usage-events.xml` failure, the `ehcache.xml`/
cache-management gap, the 55 thin-controller violations via `check-thin-controllers.py`, the 10
manual-`new *V1()` mapper findings, and the `Map<String,Object>` service-interface finding)
reconfirmed unchanged, item-by-item, before → after — no unrelated movement in either direction.

**`bash scripts/verify-gates.sh`: 17 → 16.** Exactly the one predicted assertion cleared:
`Every Maven submodule must declare Lombok: migrations`, **cleared**. All sixteen remaining
assertions reconfirmed unchanged by direct comparison of the before/after failure lists,
including the four carried red assertions: presigned-upload `await fetch(` count is exactly **1**
per file (`useLessonMutations.ts`, `useMaterialMutations.ts`, reconfirmed by `grep -c`);
`Frontend must not use a left side menu/sidebar` still fails (assertion 4 in the after-list);
`check-frontend-ui-rules.sh` untouched by this backend-only phase (no frontend file in this
diff); `structure-lint`'s `changes/0001-usage-events.xml` assertion still fails (assertion 1 in
the after-list).

**`check-liquibase-preconditions` (`.sh` + `.py`): 0 → 0, unchanged, as required.** Both report
`passed` — all 14 changesets still comply; none was touched.

**`mvn -f backend/pom.xml clean verify`: BUILD SUCCESS, 507 tests, 0 failures, 44 skipped**
(`application` module) — identical to the P0/P2/P3/P4 baseline tracked since P0. Per-module
reactor results, all green: `domain`, `migrations`, `event-logging-to-db-feature`,
`observability`, `external-services`, `service`, `application` all `SUCCESS`; `jacoco-check`
reports "All coverage checks have been met" for `application`. Total reactor time ~2m30s.

**Green-field apply and idempotent re-apply, both independently confirmed** (see Step 6 above):
14/14 changesets applied to an empty schema; 0/14 on the second boot against the same,
now-migrated schema; all 14 recorded filenames byte-identical to P0's production R1 result.

**Fixture manifest:** 80/80 before and after; `git diff` touches neither `.claude/**` nor any of
the four managed root files.

**Build** `mvn -f backend/pom.xml clean verify` → BUILD SUCCESS, 507 tests, 0 failures, 44
skipped (application module, identical to baseline). Also `mvn -f backend/pom.xml clean install
-DskipTests` → BUILD SUCCESS (needed only to refresh `~/.m2` for the out-of-reactor boot test,
see the environment-trap note above; not part of the phase's required build).
**Test** Full backend suite via `clean verify` (above), `LiquibaseChangelogSmokeTest` **skipped**
(Testcontainers/Docker-npipe gap in this sandbox, reported honestly, not counted as a pass — see
above). Independent proof gathered instead: green-field apply (14/0/14) and idempotent re-apply
(0/14/14) against a real local Postgres via `docker compose` + `spring-boot:run`, both verified
by direct `databasechangelog` query and by `curl` against four live endpoints (all 200).
**Review** `backend-rule-review` — no blocking findings; the changelog directory, the 14
changesets, and `12-database.md` all independently confirmed untouched by diff.
**Verification** `structure-lint.sh`: **16 → 15** (exactly the one predicted assertion).
`verify-gates.sh`: **17 → 16** (exactly the one predicted assertion). `check-liquibase-
preconditions`: **0 → 0**, unchanged. The 14 production filenames from P0 step 3a
(`db/changelog/1.0.0/db.version-master.xml`) match the post-rename classpath path exactly —
confirmed against a live local database, not merely re-read from the plan. Fixture manifest:
**80/80**, unchanged throughout.
**Rollback** `git revert` on this phase's commit. No out-of-repo action — nothing was deployed;
the local Postgres container, network, and volume created for the green-field/idempotent checks
were removed (`docker compose down -v`) before this commit, confirmed by `git status --porcelain`
showing no residue. Whoever eventually deploys this must still take a `DATABASECHANGELOG` backup
first, per the plan's own Rollback note — recorded here so it survives to release day.

### What was declined, and what is reported rather than fixed

- **`docs/architecture-overview.md` was not updated**, even though it now states a stale fact
  (`classpath:db/changelog/db.root-master.xml`). Outside P5's Scope grant (P3's file); flagged
  above for whoever next opens it, not silently fixed or silently ignored — the same posture P3
  itself took toward `backend/pom.xml` and `check-maven-dependency-analysis.py`.
- **`docs/aiae-migration-plan.md`, `docs/aiae-audit.md`, and `docs/aiae-template-change-
  requests.md` were not edited.** All three are migration-process documents (the plan's own
  narrative, the pre-migration audit snapshot, and an upstream CR already describing this exact
  layout) rather than facts about the implemented repository; rewriting them retroactively was
  judged out of this phase's authority and is reported, not guessed at.
- **The Testcontainers-based smoke test was not made to pass.** Its Docker-npipe failure is an
  environment limitation of this sandbox, not a code defect this phase could fix without touching
  test infrastructure outside its Scope; independent proof was gathered by another route instead
  (see above) rather than the gap being silently absorbed into a claim that the test "passed."
- **Movement was exactly the predicted one, in both gates.** No unexplained over- or under-shoot
  in either `structure-lint.sh` or `verify-gates.sh`.

---

## P6 — `cache-management` and the second cache · **COMPLETE**

Completed 2026-08-11 on branch `mig/p06-cache-management` (from `migration` @ `e178678`).

### Environment

Same shims as P0–P5: `python3` resolved to a real 3.14.5 via a copy on `PATH` ahead of the
Windows Store stub; `JAVA_HOME=/c/Users/Admin/.jdks/corretto-21.0.12` and
`/c/Users/Admin/tools/apache-maven-3.9.16/bin` prepended for Maven (`mvn -v` → Java `21.0.12`).
Fixture manifest re-validated **80/80** before any change and again immediately before this
commit — P6's Scope never touches a managed fixture, so this was a formality, not a finding.

### Preconditions re-confirmed

`.claude/agent_docs/distributed_cache.md` was installed in P1 and is unchanged since. Read in
full before writing any code, together with `docs/migration-guardrails.md` §2.2's trap and the
plan's P6 block, §2.5's "18 regions" correction, and §6.1 D-E.

### What was built

**`backend/cache-management` (new Maven module).** Ported verbatim from the standard checkout
(`cc64e49`, `templates/generated-project/scaffold/backend/cache-management`), package
`com.aidigital.aionboarding.cachemanagement`, self-contained by its own `pom.xml` comment
(no dependency on `domain`/`service`/`application`): `cache/CacheService(+Impl)`,
`config/CacheManagementProperties`, `event/CacheInvalidationEvent` (record) +
`CacheInvalidationEventService` (interface, `updatesAfter(long, int)` — never `LocalDateTime`),
`registry/CacheNamesByClassRegistry` (interface) + `CacheNamesByClassService(+Impl)`,
`updater/CacheRegistryVerifier`, `CacheUpdaterService(+Impl)`, `ScheduledCacheUpdater`
(`@Scheduled(fixedDelayString/initialDelayString)`, bounded batches, no `parallelStream()`
anywhere). Added `<module>cache-management</module>` and a `dependencyManagement` entry to
`backend/pom.xml`.

**Outbox entity + repository (`backend/domain`).** `domain.cache.entities.CacheInvalidationEventEntity`
(extends `IdAwareEntity`; `trackedClass` TEXT, `createdAt` `Instant`) and
`domain.cache.repositories.CacheInvalidationEventRepository`
(`findByIdGreaterThanOrderByIdAsc(long, Pageable)`; `@Modifying @Query` `deleteCreatedBefore`).

**Application wiring (`backend/service`), one deliberate deviation from the scaffold reference.**
`service.common.cache.JpaCacheInvalidationEventService implements CacheInvalidationEventService`
and `service.common.cache.ApplicationCacheNamesByClassRegistry implements
CacheNamesByClassRegistry` — the two class names `structure-lint` requires by name, both found
under `backend/service`. The scaffold's own reference `JpaCacheInvalidationEventService` injects
`CacheInvalidationEventRepository` directly; this project's `.claude/rules/10-architecture.md`
("Only the paired entity service implementation may inject that entity's repository") and
`00-backend-hard-rules.md` are stricter, so a paired entity service —
`service.common.cache.services.entity.CacheInvalidationEventEntityService`, mirroring
`UserEntityService`'s existing pattern exactly — was inserted between them. `publishUpdateEvent`
is `@Transactional(propagation = MANDATORY)` on **both** `JpaCacheInvalidationEventService` and
the entity service's `save(...)` (defense in depth — found during self-review, see *Review*
below — so a future direct caller of the entity service cannot silently open its own transaction
and defeat the atomicity guarantee the outer method alone would otherwise be the only thing
enforcing).

**Liquibase (`backend/migrations`).** New file
`db/changelog/changes/0003-cache-invalidation.xml` — one changeSet,
`id="0003-create-cache-invalidation-event"`, direct `<preConditions onFail="MARK_RAN">` per
`.claude/rules/12-database.md`, creating `cache_invalidation_event` (`id BIGINT` autoincrement,
`tracked_class TEXT NOT NULL`, `created_at TIMESTAMP WITH TIME ZONE NOT NULL`) plus an index on
`created_at`. Included from `db.changelog-master.xml` (the file P5 renamed for exactly this) via
`<include file="changes/0003-cache-invalidation.xml" relativeToChangelogFile="true"/>`, alongside
the existing `1.0.0/db.version-master.xml` include. New file, no recorded history at risk —
matches §2.7 exactly as the plan predicted.

**M2 — the registry stays empty, and there is a dedicated test proving it.**
`ApplicationCacheNamesByClassRegistry.cacheNamesByClassMap()` returns `Map.of()`. Its own JavaDoc
and `ApplicationCacheNamesByClassRegistryTest` (unit) plus
`CacheInvalidationOutboxIntegrationTest.shouldStartWithAnEmptyApplicationRegistryTest`
(integration, real bean) both pin this. No warm-up code was added (none exists today; none was
needed).

**M3 — `DictionaryLookupService`'s `ConcurrentHashMap` folded into the managed cache.** Removed
the bean-level `Map<String, Long> cache` (was line 38) and the `cacheKey` parameter/prefixes
(`"user_role:" + code`, etc.) from all 8 lookup methods and the shared `lookup(...)` helper.
Every finder passed to `lookup(...)` (`userRoleRepository::findByCode`, etc.) is already
`@QueryHints(HINT_CACHEABLE)`-annotated with a matching `ehcache.xml` `findXByCode` region
(verified region-by-region against `ehcache.xml` before removing the map, not assumed), so this
does not add a database round trip on repeated lookups — it removes the second, unmanaged cache
`distributed_cache.md` calls out by name. `DictionaryLookupServiceTest`'s
`shouldCacheRepeatedLookupsTest` — which asserted `verify(userRoleRepository,
times(1)).findByCode(...)`, i.e. asserted the presence of the map being removed — was replaced
with `shouldDelegateEveryLookupToTheRepositoryWithoutASecondBeanLevelCacheTest`, asserting
`times(2)`: the repository mock is now hit on every call, proving no bean-level memoization
remains reachable from this class. This **is** the "M3 test asserting no second cache instance is
reachable from `DictionaryLookupService`" the plan's Test block asks for.

**The cache-manager situation, and how the second manager was avoided.** Before this phase there
was exactly one `javax.cache.CacheManager` (Hibernate's, from `hibernate.javax.cache.uri:
ehcache.xml`) and zero Spring `CacheManager` beans. The `cache-management` module's
`CacheServiceImpl`/`CacheUpdaterServiceImpl` need Spring `CacheManager` beans (they resolve
regions via `org.springframework.cache.CacheManager`, not `javax.cache` directly) to reach the
Hibernate L2/query regions by name — required application wiring per the scaffold's own
`cache-management/pom.xml` comment ("a bridge CacheManager that exposes the Hibernate
second-level (L2) cache"). **`@EnableCaching` was never added.** Verified by decompiling the
installed `spring-boot-autoconfigure-3.4.0.jar`: `CacheAutoConfiguration` is
class-level-gated `@ConditionalOnBean(CacheAspectSupport.class)`, and that bean is registered
only by `@EnableCaching`'s imported configuration — so without the annotation, Spring Boot's
`JCacheCacheConfiguration` (and its would-be no-URI `getCacheManager()` call) never activates,
full stop, regardless of `spring.cache.type: jcache` already being declared. Instead,
`com.aidigital.aionboarding.config.CacheManagerConfig` (new, `backend/application`) manually
declares three `@Bean`s: the `javax.cache.CacheManager` resolved from the same `ehcache.xml`
classpath resource; a Spring `CacheManager` (`JCacheCacheManager`) wrapping it, for
`cache-management`'s beans to use; and a `HibernatePropertiesCustomizer` that sets
`hibernate.javax.cache.cache_manager` to that exact instance. Decompiled
`hibernate-jcache-6.6.2.Final.jar` (`JCacheRegionFactory.resolveCacheManager`) to confirm
Hibernate checks that property **first** and, when it is already a `javax.cache.CacheManager`
instance, returns it verbatim (`useExplicitCacheManager`) instead of resolving one itself via
`CachingProvider`/URI — so this is not "two managers that happen to share a URI and get
deduplicated by the JSR-107 provider," it is Hibernate reusing the literal object Spring created.
Added the one new dependency this requires, `org.springframework:spring-context-support`
(version-managed by the already-imported `spring-framework-bom`, unversioned in
`backend/application/pom.xml`) — the module that carries `JCacheCacheManager`; verified it is not
already on the classpath under any other artifact before adding it.
`CacheInvalidationOutboxIntegrationTest.shouldShareTheExactJCacheManagerWithSpringAndHibernateTest`
asserts object identity (`isSameAs`) across all three: the `javax.cache.CacheManager` bean, what
`JCacheCacheManager.getCacheManager()` returns, and what the `HibernatePropertiesCustomizer`
places in the properties map — and passed for real (H2, not mocked) in this phase's build.

### Review — self-conducted `backend-rule-review`

Ran the review sequence from `.claude/skills/backend-rule-review` against this diff before
closing the phase. One real finding, fixed in this same commit (see *entity service's `save`*
above): `CacheInvalidationEventEntityService.save(...)` was originally plain `@Transactional`
(REQUIRED) rather than `MANDATORY`, meaning a future caller that bypassed
`JpaCacheInvalidationEventService` and called the entity service directly would silently open
its own transaction instead of failing — undermining the "publication outside a transaction
fails" contract for that one bypass path. Changed to `MANDATORY` on both layers; re-ran
`mvn -f backend/pom.xml -pl cache-management,service -am test` after the fix — green, no
regressions, no test needed updating (all existing tests already call through the outer
service). No other findings: 1 entity = 1 repository = 1 service holds
(`CacheInvalidationEventEntityService` is the only class injecting
`CacheInvalidationEventRepository`, confirmed by `grep`); no controller/orchestrator injects a
repository; no outbound integration was added; `@ConfigurationProperties` used throughout
(`CacheManagementProperties`), no `@Value`; zero `private` methods introduced in any production
class (checked every new/edited file); `CacheInvalidationEvent` is a top-level record; every
handwritten method carries JavaDoc except `@Override`s that inherit their contract; every new
Maven submodule (`cache-management`) declares Lombok; polling batches are explicitly bounded
(`CacheManagementProperties.batchSize`/`maxBatchesPerPoll`, defaults 500/20); no transaction
spans external I/O (none was added); generated OpenAPI sources untouched (no controller changes).

**A lifecycle detail considered and accepted, not a finding.** `CacheManagerConfig.jCacheManager()`
is `@Bean(destroyMethod = "close")`, matching the scaffold's own reference exactly, with no
explicit `@DependsOn` forcing destruction order against the `EntityManagerFactory` bean (the
`CacheAutoConfiguration$CacheManagerEntityManagerFactoryDependsOnPostProcessor` that would add
this automatically is itself gated behind `@EnableCaching`, which this phase does not add).
Considered and accepted rather than fixed: Spring's default reverse-creation-order destruction
already destroys the EMF (created after the customizer that depends on `jCacheManager`) before
`jCacheManager` itself; `javax.cache.CacheManager.close()` is specified idempotent regardless.
515 application tests across this run's many repeated Spring context creations/destructions,
including this bean every time, produced zero shutdown-related failures.

### Verification

**`bash scripts/structure-lint.sh`: 15 → 14.** Exactly the one predicted assertion cleared —
`ehcache.xml remains without cache-management` — confirmed by diffing the full before/after
failure lists line-for-line: identical except that one line. None of the module's own six new
assertions (module `pom.xml`; `<module>cache-management</module>`; `<artifactId>cache-management
</artifactId>` in `service/pom.xml`; `@EnableScheduling`; `JpaCacheInvalidationEventService.java`
+ `ApplicationCacheNamesByClassRegistry.java` under `backend/service`; the changelog + its
include) newly failed, and neither did the `updatesAfter(LocalDateTime` or
`parallelStream()`-under-`*/cache` bans. **Observation, not a defect fixed here:** that
`parallelStream()` ban's glob (`backend/application/src/main/java/*/cache`) is a single-level
wildcard that cannot match this project's multi-segment package path
(`com/aidigital/aionboarding/cache`) — it would not fire even if a violation existed. Out of
Scope (`scripts/lib/**` is P2's), and moot regardless since no `parallelStream()` was added
anywhere; reported so the next person does not mistake silence there for coverage.

**`bash scripts/verify-gates.sh`: 16 → 16, byte-identical failure list**, diffed line-for-line
(not merely counted) — confirmed no unrelated movement in either direction, including the four
carried red assertions (presigned-upload count exactly 1; sidebar assertion; `check-frontend-ui-
rules.sh`; usage-events changelog path) and `check-maven-dependency-analysis.py` (still fails —
`maven-dependency-plugin` is not yet activated in `backend/pom.xml`'s `build/plugins`, a
pre-existing gap this phase's Scope does not touch).

**`bash scripts/lib/check-architecture-overview.sh`: passed (mvp) → passed (mvp).** The
cache-management-specific assertions this gate gained the moment the module directory exists —
exactly one `- Cache status: enabled` line, exactly one `` | `backend/cache-management` | ``
module-table row — both satisfied; verified the exact literal-substring count is **1** for both
before treating the doc as done, not merely that the script exited 0.

**`bash scripts/lib/check-liquibase-preconditions.sh`: 0 → 0, unchanged.** The new changeSet
declares direct `<preConditions>`; all 15 changesets (14 existing + 1 new) still comply.

**`mvn -f backend/pom.xml clean verify`: BUILD SUCCESS.** `application` module:
**515 tests, 0 failures, 0 errors, 45 skipped** (was 507/0/0/44 — **+8 tests, +1 skip**, all in
the new `backend/application/.../cache/` integration tests, explained below). Every module's
`jacoco-check` reports "All coverage checks have been met," including the brand-new
`cache-management` bundle (7 of its 12 classes are graded — `CacheManagementProperties` is
excluded by the existing `**/config/**` rule — at effectively full coverage from the ported
scaffold test suite) and `service` (169 classes, was 166 — the three new classes are covered by
dedicated unit tests). Per-module reactor results, all `SUCCESS`: `domain`, `migrations`,
`event-logging-to-db-feature`, `observability`, `external-services`, **`cache-management`
(new)**, `service`, `application`.

**New tests, by module** (36 total, 35 run + 1 skipped, 0 failures):
- `cache-management` (19, all new, all pass): `CacheServiceImplTest` (1),
  `CacheManagementPropertiesTest` (5), `CacheInvalidationEventServiceTest` (1),
  `CacheInvalidationEventTest` (1), `CacheNamesByClassServiceImplTest` (1),
  `CacheRegistryVerifierTest` (3), `CacheUpdaterServiceImplTest` (3),
  `ScheduledCacheUpdaterTest` (4) — ported from the standard checkout, package-adjusted only,
  covering: registry verification (disabled/missing-region/present-region), a mutation-source
  with no registered regions failing loudly, monotonic-sequence rejection, cursor-hold-on-failure
  retry, and bounded-batch backlog behavior.
- `service` (9, all new, all pass): `CacheInvalidationEventEntityServiceTest` (3),
  `JpaCacheInvalidationEventServiceTest` (5), `ApplicationCacheNamesByClassRegistryTest` (1) —
  plus `DictionaryLookupServiceTest`'s one replaced test (M3, above; count unchanged at 12).
- `application` (8: 7 pass, 1 skipped): `CacheInvalidationOutboxIntegrationTest` (7, `@SpringBootTest`
  + H2, all pass for real) — commit publishes exactly one event; rollback publishes none;
  publication outside a transaction throws `IllegalTransactionStateException`; Spring/Hibernate
  share the exact `javax.cache.CacheManager` instance (object identity, not type-only); the
  `hibernate-cache.*.UserRole` region is reachable through Spring's `CacheManager` abstraction;
  the real registry starts empty; registry verification against the real empty registry with
  `verify-registry=true` does not throw. `CacheInvalidationRemoteEvictionIntegrationTest` (1,
  **skipped** — `@Testcontainers(disabledWithoutDocker = true)`, the same Docker-npipe gap
  `DictionaryCacheIntegrationTest`/`LiquibaseChangelogSmokeTest` already carry in this sandbox,
  not a new gap and not counted as a pass) asserts `CacheUpdaterService.clearCache(...)` —
  `ScheduledCacheUpdater`'s exact call for a remote invalidation event — causes the next
  `findByCode` to miss the Hibernate query cache and repopulate from the database. This is the
  "real L2 integration test proves remote invalidation causes the next read to hit the database"
  item `distributed_cache.md`'s Required Verification list asks for; it is written and correct,
  and simply cannot execute in this sandbox for the same reason two pre-existing tests can't.

**Independent proof gathered the way P5 did, because the integration test above cannot run
here.** `docker compose -f docker-compose.yml up -d postgres` against an empty database
(confirmed via `psql \dt` → no relations), then `mvn -f backend/application/pom.xml
spring-boot:run -Dskip.frontend=true` (after `mvn clean install -DskipTests` to refresh
`~/.m2`, same trap P5 documented). Liquibase applied **15** changesets green-field (`Run: 15,
Previously run: 0`, `Table cache_invalidation_event created`, `Index
idx_cache_invalidation_event_created_at created`), then **0** on a second boot against the
now-migrated schema (`Run: 0, Previously run: 15`). Read `databasechangelog` directly:
14 rows carry `filename = db/changelog/1.0.0/db.version-master.xml` (byte-identical to every
prior phase's proof) and the 15th carries `filename =
db/changelog/changes/0003-cache-invalidation.xml`, `id = 0003-create-cache-invalidation-event`.
`\d cache_invalidation_event` confirmed the exact column set (`id BIGINT` identity,
`tracked_class TEXT NOT NULL`, `created_at TIMESTAMP WITH TIME ZONE NOT NULL`) and both indexes.
Both boots reached the Clerk-configuration validation step and stopped there (`CLERK_PUBLISHABLE_KEY`
was intentionally not supplied) — an environment-config requirement unrelated to this phase, not
a defect; the schema/migration proof above does not depend on the app reaching a listening port.
`docker compose down -v` removed the container/network/volume afterward;
`git status --porcelain` before and after this local run is identical except this phase's
intended file changes.

**Ehcache region count: 18 before, 18 after — unchanged, as required.** `ehcache.xml` was not
edited in this phase (confirmed by empty `git diff` on that file). 8 entity regions + 8
`findXByCode` query regions + 2 infrastructure regions
(`hibernate-cache.default-query-results-region`, `hibernate-cache.default-update-timestamps-region`,
required by `missing_cache_strategy: fail`) — counted directly against the file, not assumed from
either source document (both of which say "16," corrected already in the plan's own §2.5).

**`docs/architecture-overview.md` updated per trap 5, and two adjacent pre-existing staleness
items fixed while in the same paragraphs.** `- Cache status: enabled` (was `disabled`) and one
`` | `backend/cache-management` | `` *Backend modules* row now satisfy
`check-architecture-overview.sh`'s unconditional cache-management block; a matching
`` | `backend/observability` | `` row was added at the same time (see below). Rewrote *Caching
and consistency* to describe the outbox/registry/poller mechanism, the shared-manager identity
proof, and M3; struck the "Distributed cache" entry from *Adopted standards the code has not
caught up to yet* (closed by this phase, per the plan's own §3 step 1a table). **Found and fixed
as a byproduct, not part of this phase's mandate:** the *Repository and module boundaries*
section still said "`backend/pom.xml` declares exactly six top-level Maven modules" and omitted
`backend/observability` from both that sentence and the *Backend modules* table — stale since P4
landed (confirmed: `ExternalClientMetricsInterceptor`/`ExternalCallTimer` are already at
`backend/observability/.../external/`, not `backend/external-services` as the doc still claimed
in two places, including its own "Adopted standards" bullet for the same thing). Both fixed here
because this phase's edit sits in the exact same paragraphs and leaving them wrong beside a
correct edit would misinform the next reader — the same posture P5 took for the stale
`db.root-master` references. Verified the two module-table rows this phase added are each the
**exactly-one** literal-substring match `check-architecture-overview.sh` requires (`grep -Fc`),
not merely "present somewhere" — an early draft accidentally added a second matching row in a
different table and was caught and fixed before this commit.

**Build** `mvn -f backend/pom.xml clean verify` → BUILD SUCCESS, `application` module
515/0/0/45 (was 507/0/0/44). All modules' `jacoco-check`: "All coverage checks have been met,"
including the new `cache-management` bundle.
**Test** Full backend suite via `clean verify` (above) plus the independent Liquibase green-field
(15/0/15) and idempotent re-apply (0/15/15) proof against a real local Postgres via `docker
compose` + `spring-boot:run`, `databasechangelog` read directly, table/index structure confirmed
by `\d`. `CacheInvalidationRemoteEvictionIntegrationTest` **skipped** (Testcontainers/Docker-npipe
gap in this sandbox, same as two pre-existing tests; reported honestly, not counted as a pass).
**Review** Self-conducted `backend-rule-review` per `.claude/skills/backend-rule-review` — one
real finding (entity-service `save(...)` propagation), fixed in this commit; no other findings.
**Verification** `structure-lint.sh`: **15 → 14** (exactly the one predicted assertion, diffed
line-for-line). `verify-gates.sh`: **16 → 16**, byte-identical failure list. `check-architecture-
overview.sh`: passed (mvp) → passed (mvp), with the cache-management-specific assertions now
satisfied at exactly one occurrence each. `check-liquibase-preconditions.sh`: **0 → 0**,
unchanged. Ehcache regions: **18 → 18**, unchanged (file not edited). Fixture manifest: **80/80**,
unchanged throughout.
**Rollback** `git revert` on this phase's commit. The `0003-create-cache-invalidation-event`
changeSet was exercised in rehearsal (green-field apply, confirmed above) but never against a
populated schema with real invalidation traffic — if this phase is reverted after having reached
a real deployment, the table drop belongs in a follow-up changeSet, not a schema edit, per
`.claude/rules/12-database.md`'s "do not rewrite existing applied changelogs." No out-of-repo
action: nothing was deployed; the local Postgres container/network/volume were removed
(`docker compose down -v`) before this commit, confirmed by `git status --porcelain` showing no
residue.

### What was declined, and what is reported rather than fixed

- **No domain-level `@DataJpaTest` was added for `CacheInvalidationEventRepository`.** Its two
  custom methods are exercised indirectly — `findByIdGreaterThanOrderByIdAsc`-adjacent behavior
  through `CacheInvalidationOutboxIntegrationTest`'s real H2-backed `count()`/`deleteAll()`
  calls, `deleteCreatedBefore`'s JPQL syntax validated at every `@SpringBootTest` context boot
  that registers the repository (515 of them in this run) — and at the unit level in
  `CacheInvalidationEventEntityServiceTest`. Adding a dedicated repository-level test would have
  required adding H2 as a new test dependency to `backend/domain`, which has none today and no
  existing precedent for one; judged not worth the new dependency for a two-method repository
  already covered three other ways.
- **The JSR-107 "same URI, same manager" provider-dedup path was considered and not used.**
  Relying on `CachingProvider.getCacheManager(uri, classLoader)` returning an identical instance
  for equal arguments would have worked but depends on exact URI/classloader equality between two
  independently-written call sites (Hibernate's internal resolution and a hypothetical Spring-side
  duplicate). Reusing Hibernate's own `hibernate.javax.cache.cache_manager` override property
  instead — verified by decompiling `hibernate-jcache-6.6.2.Final.jar` — removes that dependency
  entirely: there is structurally one call that creates the manager and one property that hands
  Hibernate the exact same object, not two independent resolutions expected to agree.
- **Nothing in the plan's own P6 block was found wrong.** Unlike P4/P5, this phase's own written
  block (`docs/aiae-migration-plan.md`, "P6 — `cache-management` and the second cache") already
  carries the corrected 18-region count and the correct changelog path from §2.7 — the "16
  regions" and "move `CacheConfig`" defects both source *documents* describe were already fixed
  in the plan text itself before this phase started; there was nothing left to disprove here.
  This phase's own brief's two adjacent documentation defects (the "six modules"/`observability`
  location staleness in `docs/architecture-overview.md`) are reported above as found-and-fixed,
  not as plan defects.
- **Movement was exactly the predicted one in `structure-lint.sh`, and exactly zero (byte-
  identical) in `verify-gates.sh` and `check-architecture-overview.sh`.** No unexplained over- or
  under-shoot in any of the three gates this phase's Verification block names.

---

## P7 — Coordinate node-affine work · **COMPLETE**

Completed 2026-08-11 on branch `mig/p07-concurrency` (from `migration` @ `8f186bf`, merged forward to
include `c6f1f0d` — a docs-only CR-11 commit that touches no file this phase modifies).

### Environment

Same shims as P0–P6: `python3` resolved to a real 3.14.5 via a copy on `PATH` ahead of the
Windows Store stub; `JAVA_HOME=/c/Users/Admin/.jdks/corretto-21.0.12` and
`/c/Users/Admin/tools/apache-maven-3.9.16/bin` prepended for Maven. Fixture manifest
re-validated **80/80** before any change and again immediately before this commit — P7's Scope
never touches a managed fixture. `mvn -f backend/pom.xml clean install -DskipTests` was needed
once after a stray concurrent build left `target/classes` with a truncated class file
(`LessonApiMapper.class`); a clean rebuild cleared it, matching the environment note about a
stale build biting after an odd boot/interrupt.

### Preparation, not remediation — confirmed against the working tree before writing anything

One node runs today; nothing is currently duplicated. Verified before any edit: `grep -rn
"FOR UPDATE SKIP LOCKED\|pg_advisory" backend` — zero matches anywhere in the repository.
`MaterialYoutubeBackfillJob` was `@Scheduled(fixedDelay = 300_000)` **and carried `@Transactional`
at the job level**, wrapping the whole batch — including every YouTube call — in one database
transaction. That is the exact violation `.claude/rules/14-performance.md` forbids, and it
predates this phase; it is fixed here as part of the same change, not filed separately, because
fixing the concurrency shape and fixing the transaction-boundary violation are the same edit.
`AbandonedUploadCleanupJob` (`fixedDelay = 900_000`) already had no `@Transactional` at the job
level and its S3 delete already ran after commit via `TransactionSynchronizationManager` — that
shape was already correct and needed no transaction-boundary fix, only the claim.

### What changed, and exactly where each transaction begins and ends

**1. `MaterialYoutubeBackfillJob` / `MaterialYoutubeServiceImpl.backfillMissingYoutubeMetadata()`.**
`@Transactional` removed from the job method entirely — the job now carries no transaction
annotation at all. The three-phase shape lives in the service, split across two beans so
Spring's `@Transactional` proxy actually applies (a method calling a sibling method on `this`
is not intercepted; the claim and the save had to move to a different bean, not just a
different method, to get real transaction boundaries):
- **Phase 1 (short transaction, opens and commits inside
  `MaterialYoutubeUrlEntityService.claimMissingMetadataBatch`):** a new repository method,
  `MaterialYoutubeUrlRepository.claimMissingMetadataBatch(int limit)`, native SQL,
  `SELECT ... WHERE title = '' AND thumbnail_url = '' AND metadata_error = '' ORDER BY id ASC
  LIMIT :limit FOR UPDATE SKIP LOCKED`. Transaction opens when the entity-service method is
  entered and commits when it returns — before any external call.
- **Phase 2 (no transaction):** `MaterialYoutubeServiceImpl.backfillMissingYoutubeMetadata()`
  loops over the claimed (now detached) rows and calls `youtubeClient.fetchOembed(row.getUrl())`
  for each — no transaction is open anywhere in this loop.
- **Phase 3 (one short transaction per row):** each fetched result is written back via
  `MaterialYoutubeUrlEntityService.save(row)` — a new transaction per row, so a fetch failure on
  row *N* does not roll back rows already saved for rows 1..N-1 in the same tick (a real,
  incidental improvement over the previous all-or-nothing behaviour, not a stated goal).
- **New collaborator required and not named in Scope's literal file list:**
  `backend/service/.../material/services/entity/MaterialYoutubeUrlEntityService.java`.
  `MaterialYoutubeServiceImpl` was the sole injector of `MaterialYoutubeUrlRepository` before
  this phase — a pre-existing, narrow divergence from "1 entity = 1 repository = 1 service"
  (every other entity in this codebase already has a dedicated `.../services/entity/` class).
  Splitting the claim from the orchestrator required a second bean regardless of that rule, and
  the natural, rule-compliant place for it is exactly that missing entity service — so this
  phase adds it and moves every repository call in `MaterialYoutubeServiceImpl` through it,
  closing the divergence as a side effect. Documented here rather than silently absorbed, the
  same posture P1/P2/P6 took for their own "necessary beyond the literal file list" additions.
- **Residual gap, disclosed rather than hidden:** the claim commits without writing a persisted
  "in progress" marker (no schema column exists for one, and this phase's Scope does not include
  `backend/migrations`; repurposing a user-visible column such as `metadataError` as a sentinel
  was considered and rejected — it would leak a placeholder into a field the UI can read, and a
  crash between claim and save would leave it stuck marked forever with no recovery path). This
  means the window this phase closes is the *concurrent-invocation* one — two nodes' ticks
  overlapping in real time claim disjoint rows, proven below — not a *sequential-reclaim* window
  a few milliseconds to a couple of seconds wide between one node's claim-commit and its
  save-commit, during which a second node's *later, non-overlapping* claim could still reclaim
  and re-fetch the same rows. Given `fixedDelay = 300_000` per node and YouTube oEmbed latency
  in the sub-second-to-low-single-digit-second range, that window is roughly three orders of
  magnitude smaller than the tick interval. A duplicate fetch in that narrow window wastes one
  extra YouTube call per affected row, never corrupts data (the second save simply overwrites
  with equivalent freshly-fetched content) — the same "wasteful, never incorrect" character the
  plan itself accepts for the abandoned-upload sweep's duplicate S3 deletes.

**2. `AbandonedUploadCleanupJob` / `StorageService.cleanupAbandonedUploads()`.**
Simpler: the claim and the delete now happen in the **same** transaction (matching the shape
already there before this phase — `cleanupAbandonedUploads()` keeps its class-level
`@Transactional`, and the S3 delete was already deferred to `afterCommit()`, outside that
transaction). `PendingUploadRepository.findByConfirmedFalseAndExpiresAtBefore` (a plain,
unlocked `SELECT`) is replaced by `claimExpiredUnconfirmed(LocalDateTime cutoff, int limit)` —
native SQL, `SELECT ... WHERE confirmed = false AND expires_at < :cutoff ORDER BY expires_at ASC
LIMIT :limit FOR UPDATE SKIP LOCKED`. Because the claimed rows are deleted before this
transaction commits, there is no reclaim window at all here, unlike case 1 above — once claimed,
a row is gone before any other transaction could see it again.

**3. `TeacherVideoRefreshServiceImpl.refreshTeacherVideoIfNeeded`.** Different shape, as the plan
predicted: read-triggered, not scheduled. **The no-transaction-across-HeyGen invariant already
existed before this phase** — `LessonServiceImpl.getLesson()` carries no `@Transactional`
specifically so the HeyGen call inside the refresh chain never runs inside a transaction (see
that method's own JavaDoc, unchanged by this phase); `LessonEntityService.getReference` /
`findByIdWithFetches` / `save` each open and close their own short transaction. This phase adds
a **claim gate in front of the HeyGen call**, reusing the lesson's existing `@Version` column
instead of a new "refresh timestamp" column (Scope does not include `backend/migrations`, and no
dedicated timestamp field exists on `Lesson` outside the JSON-nested, non-atomically-addressable
`generationMetadata.teacherVideo.checkedAt`):
- New repository method `LessonRepository.claimForTeacherVideoRefresh(Long id, Long
  expectedVersion)` — JPQL bulk update, `UPDATE Lesson l SET l.version = l.version + 1 WHERE
  l.id = :id AND l.version = :expectedVersion`, returning the row count. Bulk updates are exempt
  from JPA's automatic optimistic-lock enforcement, so the version predicate and increment are
  written out explicitly rather than relying on Hibernate's usual versioned-`UPDATE` machinery —
  this does not weaken `@Version`'s normal protection for content edits: any concurrent `save()`
  that read the pre-claim version still fails its own version check once this claim commits,
  exactly as it would against any other concurrent writer.
- New pass-through `LessonEntityService.claimForTeacherVideoRefresh(Long, Long)` — a short
  transaction of its own, called from `TeacherVideoRefreshServiceImpl` (a different bean, so
  the call is genuinely proxied and transactional; `TeacherVideoRefreshServiceImpl` already
  injects `LessonEntityService` rather than `LessonRepository` directly, per
  `.claude/rules/10-architecture.md`, so no new architecture exception was needed here, unlike
  case 1).
- **Transaction boundary:** the claim opens and commits **before** `heyGenClient.getVideoStatus`
  is called. On a lost claim, the method returns `new RefreshResult(lesson, teacherVideo)`
  unchanged immediately — no HeyGen call, no `save()`, no exception. On a won claim, the
  in-memory `lesson.setVersion(lesson.getVersion() + 1)` mirrors the already-committed DB state
  before the HeyGen call and the later `lessonEntityService.save(lesson)` (its own short
  transaction, after the HeyGen call returns), so the winner's own save targets the version it
  actually holds rather than the stale pre-claim value.
- **Accepted cost:** a claim that wins but then hits `HeyGenExternalException` leaves the
  lesson's version incremented with no corresponding content change (the version bump already
  committed before the HeyGen call could fail). This is a normal, bounded cost of "claim before
  external call" — the alternative (claiming and saving atomically) is exactly the shape that
  would hold a transaction across the HeyGen call, which is forbidden.
- **Existing `@Version` behaviour, unbroken:** `LessonServiceImplTest`,
  `LessonEntityServiceTest`'s existing (non-`ClaimForTeacherVideoRefresh`) tests, and every other
  caller of `lessonEntityService.save(...)` continue to exercise Hibernate's normal versioned
  update path untouched — this phase adds one new bulk-update method beside it, and does not
  modify `Lesson.java`, `save()`, or any existing optimistic-lock test.

### What was tested, and against which database

**Unit tests (Mockito, no database, run everywhere):**
- `MaterialYoutubeServiceImplTest` — rewritten to mock `MaterialYoutubeUrlEntityService` instead
  of the repository directly; asserts the claimed batch is fetched-and-saved per row, and that
  zero claimed rows means zero YouTube calls and zero saves.
- `MaterialYoutubeUrlEntityServiceTest` (new) — the new entity service's five methods delegate
  correctly to the repository.
- `PendingUploadEntityServiceTest` / `StorageServiceTest` — renamed
  `findExpiredUnconfirmed`/`claimExpiredUnconfirmed` call sites; behaviour otherwise unchanged.
- `TeacherVideoRefreshServiceImplTest` — the two existing tests now stub
  `claimForTeacherVideoRefresh(...) → true`; a new test asserts that a **lost** claim
  (`→ false`) returns the exact input `Lesson`/`TeacherVideoRecord` objects unchanged, never
  calls `heyGenClient`, and never calls `save`.
- `LessonEntityServiceTest` — new nested `ClaimForTeacherVideoRefresh` class, two tests (1 row
  updated → `true`; 0 rows updated → `false`).

**Real-database integration tests, `@SpringBootTest @ActiveProfiles("test")`, H2 in PostgreSQL
compatibility mode (`application-test.yml`, `MODE=PostgreSQL`, `ddl-auto: create-drop`, no
Testcontainers/Docker needed):**
- `TeacherVideoRefreshClaimConcurrencyIntegrationTest` — **a genuine concurrency proof.** Two
  real threads, each in its own Spring-managed transaction (`TransactionTemplate`), both call
  `LessonRepository.claimForTeacherVideoRefresh` for the same lesson id/version, started together
  via a `CountDownLatch`. Asserts exactly one of the two returns `1` and the other `0`, and that
  the lesson's version advanced by exactly `1`, not `2`. This claim is a plain `UPDATE ... WHERE
  version = ?` — no PostgreSQL-specific syntax — so H2 is a full proof here, not a best-effort
  one. **First version of this test deadlocked**: it held both transactions open across a
  `CyclicBarrier` (mirroring the SKIP LOCKED tests' shape), but an `UPDATE` correctly *blocks* on
  a locked row instead of skipping it, so the loser's `UPDATE` never returned to reach the
  barrier the winner was waiting on. Rewritten to let the two `UPDATE`s serialize naturally
  (no barrier across the call) — the blocking-then-correctly-failing behaviour *is* the
  atomicity guarantee, not something to work around.
- `MaterialYoutubeUrlClaimIntegrationTest`, `AbandonedUploadClaimIntegrationTest` — **correctness
  proofs only, single-threaded, deliberately not concurrency tests.** Each proves its claim
  query's `WHERE`/`ORDER BY`/`LIMIT` shape: only genuinely-missing/genuinely-expired rows are
  returned, bounded to the requested limit, never an already-fetched or not-yet-expired row.

**Real PostgreSQL, `docker compose` (Testcontainers cannot reach Docker in this sandbox — the
documented npipe failure — but `docker compose` itself works here, as it did for P5/P6):** a
genuinely concurrent, two-JDBC-connection proof, run directly against `docker-compose.yml`'s
`postgres:16-alpine` service after booting the real application once (`mvn -f
backend/application/pom.xml spring-boot:run`, same pattern as P6) to apply all 15 changesets and
seed dictionary data, then seeding 6 rows each into `material_youtube_urls` and `pending_uploads`
and 1 row into `lessons` by direct SQL. A small standalone Java program
(`org.postgresql:postgresql:42.7.4` on the classpath, no Spring, no Hibernate — the literal SQL
this phase's repository methods issue) ran two threads per claim, synchronized with a
`CyclicBarrier`/`CountDownLatch` so both transactions were open together:
- `material_youtube_urls` claim: node A claimed `{1,2,3}`, node B claimed `{4,5,6}` — disjoint,
  both non-empty. Repeated after resetting the rows: node A `{4,5,6}`, node B `{1,2,3}` —
  disjoint again, order not fixed, correctness is.
- `pending_uploads` claim: same shape, same result — disjoint, both non-empty, both runs.
- `lessons` version claim: exactly one of two concurrent `UPDATE`s matched a row both times run
  (sum of rows-updated across both threads `= 1`), final version `= observed + 1`, never `+2`.

This is the authoritative concurrency proof for the two `SKIP LOCKED` claims. It is not part of
the automated Maven test suite — it is a manual, recorded proof in the same spirit as P5's
`databasechangelog` read and P6's green-field Liquibase apply, both also run once, by hand,
against a real `docker compose` Postgres and recorded here rather than left as an unrepeatable
claim.

**What could not be tested here, and why — read before trusting an H2-only run of the two
`SKIP LOCKED` claims.** A genuinely concurrent version of `MaterialYoutubeUrlClaimIntegrationTest`
and an equivalent for `AbandonedUploadClaimIntegrationTest` were written first, using the same
two-threads-plus-barrier shape that works for the plain-`UPDATE` lesson claim. Run repeatedly
against this project's H2 test datasource (`MODE=PostgreSQL`), they were **flaky**: on a fresh
table with six unlocked, matching rows and no prior claim anywhere in the test JVM, one caller's
`SELECT ... FOR UPDATE SKIP LOCKED` sometimes came back completely empty while the other claimed
every row — not the disjoint-non-empty-both split the same query reliably produces on real
PostgreSQL (confirmed above) and even on plain H2 outside PostgreSQL compatibility mode (checked
separately with raw JDBC, no Spring, no Hibernate: two connections, one holds a row lock
uncommitted, the other's `SKIP LOCKED` correctly excludes only that row). Inserting extra
"warm-up" queries before the real claim sometimes made the flaky run pass, which is itself a
sign of a first-execution-sensitive quirk rather than a fixable test bug. This matches the
`docs/migration-guardrails.md` warning precisely: *"`SELECT … FOR UPDATE SKIP LOCKED` is
Postgres-specific and H2 will not exercise it."* Measured here more precisely: H2 **parses and
partially honours** the syntax (it is not a no-op), but its row-locking behaviour under genuine
concurrent access, at least in `MODE=PostgreSQL` through this project's Hikari-pooled
`@SpringBootTest` context, is not reliable enough to assert on — a flaky green is worse than an
honest gap, so no concurrency claim is made from H2 for these two, and the two H2 tests that
remain are deliberately scoped down to single-threaded correctness only, with the gap and the
real-Postgres proof both documented in their own JavaDoc.

A second, unrelated H2-only gap found and worked around, not fixed: loading a `Lesson` entity a
**second** time in a fresh Hibernate session (e.g. `lessonRepository.findById` after an earlier
`save()` in a different transaction) throws a Jackson deserialize error against this H2
datasource's JSON column handling — reproduced even with an **empty** `generationMetadata` map,
so it is not specific to this phase's content. `TeacherVideoRefreshClaimConcurrencyIntegrationTest`
avoids it by reading the lesson's version from the `save()` return value and from a scalar JPQL
`SELECT l.version FROM Lesson l WHERE l.id = :id` instead of a second full-entity load. Not
reproduced against real PostgreSQL (the `docker compose` proof above reads `lessons.version`
directly via SQL, and the existing, unrelated `MyLessonsSummaryRepositoryIntegrationTest` reloads
`Lesson` rows with nested JSON metadata via Testcontainers-Postgres without issue — skipped here
for the same Docker-npipe reason as always, but not the datasource this bug is specific to).
Recorded here because the next person hitting a `MismatchedInputException` on a `Lesson` reload
under this test profile should not spend an hour re-deriving that it is environment-specific.

### Review — self-conducted `backend-rule-review`

One real finding, fixed before this commit: the first `MaterialYoutubeUrlClaimIntegrationTest`
and `AbandonedUploadClaimIntegrationTest` shared no test-to-test isolation and could see rows
left behind by an earlier test method in the same `@SpringBootTest` context (this H2 database's
`DB_CLOSE_DELAY=-1` keeps it alive across every test class in the same Surefire JVM). Fixed by
adding class-level `@Transactional` to both (auto-rollback per test); confirmed
`TeacherVideoRefreshClaimConcurrencyIntegrationTest` deliberately does **not** carry it, since
wrapping the test method in one outer transaction would defeat the two independent
`TransactionTemplate` transactions it depends on. No other findings: no `@Transactional` on any
job class; every new/changed repository method used explicitly named parameters (`@Param`), not
positional binding; `MaterialYoutubeUrlEntityService` is the sole injector of
`MaterialYoutubeUrlRepository`, closing the pre-existing 1:1:1 divergence rather than adding a
second one; no private methods introduced; every handwritten method carries JavaDoc; no magic
values (the two `LIMIT`/batch-size constants — `YOUTUBE_METADATA_BACKFILL_LIMIT`,
`CLEANUP_BATCH_LIMIT` — already existed as named constants before this phase and are unchanged).

### Verification

**`bash scripts/structure-lint.sh`: 14 → 14, byte-identical failure list.** P7's Scope files are
not named by any structure-lint assertion; confirmed by diffing the full before/after failure
lists — identical.

**`bash scripts/verify-gates.sh`: 16 → 16, byte-identical failure list** (diffed line-for-line,
not merely counted): `frontend/package.json` vitest pin, missing `lint` script, missing
`eslint.config.js`, sidebar assertion, `check-openapi-documentation.sh`,
`check-openapi-input-constraints.py`, `check-api-validation-tests.py`,
`check-maven-dependency-analysis.py`, Logbook `DefaultSink`, Logbook `WithoutBodyStrategy`,
service-source web/security/JWT import ban, `check-frontend-ui-rules.sh`,
`check-production-static-methods.sh`, `check-production-current-time.sh`,
`check-service-contract-quality.sh`, `check-coverage-integrity.sh`. The presigned-upload
assertion (CR-1, must read exactly **1**) is not in the failure list — confirmed passing, count
unchanged at 1 per exempted file. None of the 16 are new; none of the four carried assertions
(presigned upload, `check-frontend-ui-rules.sh`, sidebar, `structure-lint`'s usage-events path)
moved.

**`bash scripts/lib/check-architecture-overview.sh`: passed (mvp) → passed (mvp).** The two
scheduled-job paragraphs and the "single-node today" bullet were rewritten to describe the new
claim shapes accurately (per this phase's own instruction: if the claim mechanism changes, the
document describing it must stay true) — verified by re-reading the checker's `required_sections`
list and confirming every section it requires is still present and none of the literal-substring
assertions (cache-management row count, etc.) were touched.

**Fixture manifest: 80/80**, validated before any change and immediately before this commit.

**`mvn -f backend/pom.xml clean verify`: BUILD SUCCESS.** `application` module:
**520 tests, 0 failures, 0 errors, 45 skipped** (was 515/0/0/45 — **+5 tests, same 45 skipped**,
all five new tests are the three concurrency/correctness integration tests above, all passing,
none skipped). Every module's `jacoco-check`: "All coverage checks have been met" —
`domain`, `migrations`, `event-logging-to-db-feature`, `observability`, `external-services`,
`cache-management`, `service`, `application`, all `SUCCESS`. **+14 new tests total**, all
passing: `application` +5 (the three integration test classes); `service` +9
(`MaterialYoutubeServiceImplTest` +1, `MaterialYoutubeUrlEntityServiceTest` +5 new,
`TeacherVideoRefreshServiceImplTest` +1, `LessonEntityServiceTest` +2). Skipped count unchanged
at 45 (this phase adds no test gated on Docker/Testcontainers — the two Docker-dependent gaps it
touches, the SKIP LOCKED concurrency proofs, are covered by the manual `docker compose` proof
instead of a skipped automated test).

**Build** `mvn -f backend/pom.xml clean verify` → BUILD SUCCESS, `application` 520/0/0/45 (was
515/0/0/45, +5, all passing). All modules' `jacoco-check`: coverage checks met.
**Test** Unit tests above (Mockito, all modules); H2 `@SpringBootTest` integration tests above
(claim correctness for the two `SKIP LOCKED` queries; a genuine two-thread concurrency proof for
the plain-`UPDATE` lesson claim); a manual two-JDBC-connection concurrency proof against real
PostgreSQL via `docker compose` for both `SKIP LOCKED` claims and the version claim (see above) —
not part of the automated suite, recorded here as P5/P6 recorded their own manual DB proofs.
**Review** Self-conducted `backend-rule-review` — one finding (test isolation), fixed in this
commit; no other findings.
**Verification** `structure-lint.sh`: **14 → 14**, byte-identical. `verify-gates.sh`: **16 → 16**,
byte-identical. `check-architecture-overview.sh`: passed → passed. Fixture manifest: **80/80**,
unchanged throughout. No transaction is held open across the HeyGen or YouTube call in any of
the three components (see the transaction-boundary walkthrough above).
**Rollback** `git revert` on this phase's commit. No out-of-repo action: the `docker compose`
Postgres container used for the manual concurrency proof was removed (`docker compose down -v`)
before this commit; `git status --porcelain` before and after that local run showed no residue
outside this phase's intended file changes.

### What was declined, and what is reported rather than fixed

- **No persisted "claimed" marker for the two `SKIP LOCKED` batch jobs.** Explained above under
  "residual gap" — would require either a new schema column (outside this phase's Scope, which
  does not include `backend/migrations`) or repurposing a user-visible column as a sentinel
  (rejected: it would leak placeholder content and has no clean recovery path on a crash between
  claim and save). The window this leaves open is bounded to roughly the external-call duration
  per row, not the five-or-fifteen-minute tick interval, and is wasteful, never incorrect.
- **No automated H2 concurrency test for either `SKIP LOCKED` claim.** Written, found flaky
  against this project's H2 datasource, and deliberately not kept — see "What could not be
  tested here" above. The real proof is the manual `docker compose` run against PostgreSQL,
  recorded above rather than automated, because Testcontainers cannot reach Docker in this
  sandbox.
- **`MaterialYoutubeUrlEntityService` was added beyond Scope's literal file list.** Necessary to
  give the claim and the save their own transaction boundaries (Spring's `@Transactional` proxy
  does not intercept same-class self-invocation), and it closes a pre-existing "1 entity = 1
  repository = 1 service" divergence as a side effect. Documented rather than silently absorbed,
  matching how P1/P2/P6 handled their own necessary-but-unlisted additions.
- **Nothing in the plan's own P7 block was found wrong.** The plan's "preparation, not
  remediation" framing, its call to prefer claiming over advisory locks, and its identification
  of `TeacherVideoRefreshService` as a differently-shaped problem all held exactly as written.
  One thing the plan did not mention and this phase found by reading the code first (per the
  plan's own instruction): `MaterialYoutubeBackfillJob` already violated the
  transaction-across-external-call rule before this phase, independent of any concurrency
  concern — fixed in the same edit rather than filed separately, since the fix for one is the
  fix for the other.

---

## Carried red assertions

Every phase's evidence must show these unchanged. A count that moves without a decision
recorded here is a defect.

| # | Assertion | Expected | Why |
|---|---|---|---|
| 1 | `verify-gates.sh` presigned upload | exactly **1** | CR-1 |
| 2 | `check-frontend-ui-rules.sh` | **2222** | §6 — no frontend visual work |
| 3 | `verify-gates.sh` sidebar | fails | §6 / CR-2 — navigation preserved |
| 4 | `structure-lint` `changes/0001-usage-events.xml` | fails | §2.7 / CR-8 — telemetry kept |
