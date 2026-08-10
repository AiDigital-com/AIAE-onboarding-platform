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

## Carried red assertions

Every phase's evidence must show these unchanged. A count that moves without a decision
recorded here is a defect.

| # | Assertion | Expected | Why |
|---|---|---|---|
| 1 | `verify-gates.sh` presigned upload | exactly **1** | CR-1 |
| 2 | `check-frontend-ui-rules.sh` | **2222** | §6 — no frontend visual work |
| 3 | `verify-gates.sh` sidebar | fails | §6 / CR-2 — navigation preserved |
| 4 | `structure-lint` `changes/0001-usage-events.xml` | fails | §2.7 / CR-8 — telemetry kept |
