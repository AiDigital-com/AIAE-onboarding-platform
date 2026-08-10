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

## Carried red assertions

Every phase's evidence must show these unchanged. A count that moves without a decision
recorded here is a defect.

| # | Assertion | Expected | Why |
|---|---|---|---|
| 1 | `verify-gates.sh` presigned upload | exactly **1** | CR-1 |
| 2 | `check-frontend-ui-rules.sh` | **2222** | §6 — no frontend visual work |
| 3 | `verify-gates.sh` sidebar | fails | §6 / CR-2 — navigation preserved |
| 4 | `structure-lint` `changes/0001-usage-events.xml` | fails | §2.7 / CR-8 — telemetry kept |
