#!/usr/bin/env bash
#
# local-verify.sh — the one command engineering runs before any push.
#
# ============================================================================
# AIAE CONVERGENCE (P15 step 4) — now blocking. History below.
# ============================================================================
# P2 step 5 installed this file in a deliberate, temporary REPORT-ONLY form
# (every step recorded but the script always exited 0), because there was no
# CI service to fall back on and no shared baseline yet for any of the 28
# checkers. That phase is over. This step restores a real exit code.
#
# Two things this file still does NOT do, both deliberate and both explained
# in docs/aiae-migration-plan.md's P15 step 4:
#
# 1. It does not abort on the first failing step. structure-lint.sh and
#    verify-gates.sh are *expected* to report a fixed, known set of failures
#    forever — the five-row carried table in docs/aiae-migration-plan.md
#    (sidebar navigation, check-frontend-ui-rules.sh, and the two Logbook
#    literal-string assertions from verify-gates.sh; the usage-events
#    changelog-path false negative from structure-lint.sh). Aborting there
#    under `set -e` (the upstream scaffold's literal shape) would mean the
#    backend build, the frontend build, and the compose check never run
#    again — the opposite of "end to end, no skipped step" (P15 step 5).
#    So every step still runs and is recorded, exactly as under report-only.
# 2. It does not special-case the carried five. There is no allow-list
#    mechanism in verify-gates.sh/structure-lint.sh (no annotations, no
#    exemptions) — confirming the reported failure list equals the carried
#    set exactly is a manual comparison against docs/aiae-migration-plan.md,
#    done by whoever reads this script's output, until CR-4 lands upstream.
#
# What changed from report-only: the final exit code is now REAL — it is
# non-zero whenever any recorded step failed, computed from the same
# STEP_RESULTS this file already collected under report-only. A run with
# only the five carried failures below still exits non-zero; that is
# correct and expected, and it is why the comparison above is manual rather
# than automated.
#
# One step is excluded from that pass/fail rollup on purpose: frontend lint.
# `.husky/pre-commit` made lint report-only, not blocking, back in P12 (339
# ESLint errors, 184 of them demanding a UI restructuring this migration
# explicitly does not do — see docs/migration-guardrails.md and the P12
# correction in docs/aiae-migration-log.md). That was never a blocking gate
# in this project, so promoting it to one here — silently, as a side effect
# of this step alone — would be a new decision dressed up as mechanical
# convergence. Lint still runs and is reported every time; it just does not
# turn the exit code non-zero on its own. `npm test` and `npm run build`,
# which used to be chained after it with `&&` (so a lint failure silently
# skipped them under report-only, unnoticed because nothing enforced the
# exit code anyway), now run as their own step regardless of the lint result.
#
# Upstream steps (all preserved):
#   1. structure-lint.sh + verify-gates.sh (28 checkers under scripts/lib/,
#      itemized separately below since both scripts fail-fast on their first
#      violation and would otherwise hide every count after the first one)
#   2. Backend: `mvn -f backend/pom.xml clean verify`
#   3. Frontend: lint (report-only) + `npm test && npm run build` (blocking)
#   4. docker-compose syntax check (does NOT run containers)

set -uo pipefail
# NOTE: still no `-e` — see point 1 above. Every step must run regardless of
# an earlier one's result; the real exit code is computed at the end instead.

cd "$(dirname "$0")/.."
REPO_ROOT="$(pwd)"

STEP_NAMES=()
STEP_RESULTS=()
# Step names present in this array are excluded from the final pass/fail
# rollup (still recorded and printed, never counted) — currently only
# frontend lint. See the header comment.
NON_BLOCKING_STEPS=("Frontend: lint (report-only)")

record_step() {
  STEP_NAMES+=("$1")
  STEP_RESULTS+=("$2")
}

run_step() {
  # Runs "$2..." (a command), records a PASS/FAIL line for the final summary,
  # and lets the caller inspect $? itself if it needs to (e.g. to chain a
  # dependent step). Never lets the command's exit status abort this script.
  local label="$1"
  shift
  echo "==> ${label}"
  if "$@"; then
    record_step "${label}" "PASS"
  else
    local rc=$?
    record_step "${label}" "FAIL (exit ${rc})"
  fi
}

echo "############################################################"
echo "# local-verify.sh — every step runs; exit code is real       #"
echo "############################################################"
echo

JAVA_VERSION="$(java -version 2>&1 | awk -F'[\".]' '/version/ {print $2; exit}')"
case "$JAVA_VERSION" in 21|22|23|24|25)
  record_step "Java version (21+)" "PASS (${JAVA_VERSION})"
  ;;
*)
  record_step "Java version (21+)" "FAIL (found '${JAVA_VERSION:-unavailable}')"
  ;;
esac

if [ -f scripts/structure-lint.sh ]; then
  run_step "structure-lint.sh" bash scripts/structure-lint.sh
else
  record_step "structure-lint.sh" "SKIP (not present)"
fi

run_step "verify-gates.sh" bash scripts/verify-gates.sh

echo
echo "==> scripts/lib/ — itemized count per file (28 files)"
echo "    structure-lint.sh/verify-gates.sh above report every failing"
echo "    assertion, but a checker file run standalone still fail-fasts on"
echo "    its own first violation. This section runs every checker file"
echo "    independently, with the same source-root arguments the two"
echo "    orchestrators above use, so every count is visible even when one"
echo "    of them is red."
echo

python3 - "${REPO_ROOT}" <<'PY'
"""AIAE convergence (P2 step 5, kept in P15): itemized, non-fatal report over
every file in scripts/lib/. This is new orchestration written for this
migration; it exists because neither structure-lint.sh nor verify-gates.sh
prints a running count per checker on its own when a checker is run alone
(each calls fail() -> exit 1 on its first violation in that mode). It
intentionally does not replace either script -- it is a report layer next
to them, invoking each checker file exactly the way structure-lint.sh /
verify-gates.sh already do (same explicit source roots), so the numbers
match. Diagnostic only: nothing here affects this script's exit code.
"""
from __future__ import annotations

import re
import shutil
import subprocess
import sys
from pathlib import Path

root = Path(sys.argv[1])

# Windows-only trap, found running this exact script: Python's subprocess
# resolves a bare "bash" through Windows' own CreateProcess search order
# (which can hit the WSL launcher stub at C:\Windows\System32\bash.exe)
# instead of the PATH order bash itself would use, and does so inconsistently
# across calls. Resolve once, explicitly, with a pure-Python PATH walk
# (shutil.which), and always invoke that absolute path -- never a bare name.
BASH_EXE = shutil.which("bash") or "bash"
PYTHON3_EXE = shutil.which("python3") or "python3"


def resolve_argv(argv: list[str]) -> list[str]:
    if argv[0] == "bash":
        return [BASH_EXE, *argv[1:]]
    if argv[0] == "python3":
        return [PYTHON3_EXE, *argv[1:]]
    return argv

# (report name, argv, kind) -- argv paths are relative to the repo root.
# kind selects which regex(es) to try when turning the checker's own message
# into a number; "bool" means "no printed count, exit code is the signal".
CHECKS = [
    ("check-agent-surfaces.sh", ["bash", "scripts/lib/check-agent-surfaces.sh", "."], "mode"),
    ("check-api-client-paths.sh", [
        "bash", "scripts/lib/check-api-client-paths.sh",
        "backend/application/src/main/resources/api/v1/specs/openapi.yaml", "frontend/src",
    ], "bool"),
    ("check-api-validation-tests.py", ["python3", "scripts/lib/check-api-validation-tests.py", "."], "validation-pair"),
    ("check-architecture-overview.sh", ["bash", "scripts/lib/check-architecture-overview.sh"], "bool"),
    ("check-coverage-integrity.sh", ["bash", "scripts/lib/check-coverage-integrity.sh"], "problems"),
    ("check-frontend-ui-rules.sh", ["bash", "scripts/lib/check-frontend-ui-rules.sh", "frontend/src"], "violations"),
    ("check-installed-documentation-links.py", ["python3", "scripts/lib/check-installed-documentation-links.py", "."], "broken-ref"),
    ("check-liquibase-preconditions (.sh+.py)", ["bash", "scripts/lib/check-liquibase-preconditions.sh"], "bool"),
    ("check-maven-dependency-analysis.py", ["python3", "scripts/lib/check-maven-dependency-analysis.py", "backend"], "bool"),
    ("check-openapi-documentation.sh", [
        "bash", "scripts/lib/check-openapi-documentation.sh",
        "backend/application/src/main/resources/api/v1/specs/openapi.yaml",
    ], "violations"),
    ("check-openapi-enums.sh", [
        "bash", "scripts/lib/check-openapi-enums.sh",
        "backend/application/src/main/resources/api/v1/specs/openapi.yaml",
    ], "violations"),
    ("check-openapi-input-constraints.py", [
        "python3", "scripts/lib/check-openapi-input-constraints.py",
        "backend/application/src/main/resources/api/v1/specs/openapi.yaml",
    ], "violations"),
    ("check-openapi-strict-schemas.sh", [
        "bash", "scripts/lib/check-openapi-strict-schemas.sh",
        "backend/application/src/main/resources/api/v1/specs/openapi.yaml",
        "frontend/src/shared/api/generated/schema.d.ts",
    ], "bool"),
    ("check-production-current-time.sh", ["bash", "scripts/lib/check-production-current-time.sh"], "violations"),
    ("check-production-magic-values.sh", ["bash", "scripts/lib/check-production-magic-values.sh"], "violations"),
    ("check-production-manual-mapping.sh", ["bash", "scripts/lib/check-production-manual-mapping.sh"], "violations"),
    ("check-production-static-methods.sh", ["bash", "scripts/lib/check-production-static-methods.sh"], "violations"),
    ("check-service-contract-quality (.sh+.py)", ["bash", "scripts/lib/check-service-contract-quality.sh"], "violations"),
    ("check-thin-controllers.py", ["python3", "scripts/lib/check-thin-controllers.py", "backend/application/src/main/java"], "violations"),
]

# Files under scripts/lib/ that are shared libraries or mutation tools, not
# independent gates. Listed so the 28-file inventory is complete; NEVER
# executed here -- remove-cache-management.py / remove-usage-logging.py are
# destructive (they delete a module) and coverage-phase.sh / scan-production-
# java.py / liquibase_dependency_guard.py / removal_transaction.py /
# rewrite-installed-documentation-paths.py are libraries invoked BY the
# checkers above, not standalone gates.
NOT_INDEPENDENT_GATES = [
    "coverage-phase.sh",
    "liquibase_dependency_guard.py",
    "removal_transaction.py",
    "remove-cache-management.py",
    "remove-usage-logging.py",
    "rewrite-installed-documentation-paths.py",
    "scan-production-java.py",
]

VIOLATION_RE = re.compile(r"(\d+)\s+violation\(s\)")
PROBLEM_RE = re.compile(r"(\d+)\s+problem\(s\)")
BROKEN_REF_RE = re.compile(r"(\d+)\s+broken reference\(s\)")
VALIDATION_PAIR_RE = re.compile(
    r"(\d+)\s+constrained operation\(s\),\s*but only\s+(\d+)"
)


def extract_count(kind: str, combined: str, returncode: int) -> str:
    if kind == "mode":
        text = combined.strip().splitlines()
        return text[-1].strip() if text else f"exit {returncode}"
    if kind == "bool":
        return "0 (pass)" if returncode == 0 else "1 (fail — see message)"
    if kind == "problems":
        m = PROBLEM_RE.search(combined)
        if m:
            return m.group(1)
        return "0" if returncode == 0 else "? (unparsed FAIL)"
    if kind == "broken-ref":
        m = BROKEN_REF_RE.search(combined)
        if m:
            return m.group(1)
        return "0" if returncode == 0 else "? (unparsed FAIL)"
    if kind == "validation-pair":
        m = VALIDATION_PAIR_RE.search(combined)
        if m:
            return f"{m.group(1)} constrained op(s), {m.group(2)} isBadRequest()"
        if returncode == 0:
            return "passed (0 constrained ops needing isBadRequest, or no API test surface)"
        return "? (unparsed FAIL)"
    # "violations" (the common check-*.py / scan-production-java.py idiom)
    m = VIOLATION_RE.search(combined)
    if m:
        return m.group(1)
    return "0" if returncode == 0 else "? (unparsed FAIL)"


results = []
for name, argv, kind in CHECKS:
    try:
        proc = subprocess.run(
            resolve_argv(argv), cwd=root, capture_output=True, text=True, timeout=120,
        )
        combined = (proc.stdout or "") + (proc.stderr or "")
        count = extract_count(kind, combined, proc.returncode)
        results.append({"name": name, "count": count, "returncode": proc.returncode})
    except Exception as exc:  # noqa: BLE001 - diagnostic only, must never abort the run
        results.append({"name": name, "count": f"ERROR ({exc})", "returncode": -1})

for name in NOT_INDEPENDENT_GATES:
    results.append({"name": name, "count": "n/a (library/utility — not an independent gate, not executed)", "returncode": None})

width = max(len(r["name"]) for r in results)
for r in results:
    print(f"  {r['name']:<{width}}  {r['count']}")
print(
    f"\n  {len(results)} rows cover all 28 files under scripts/lib/: "
    "check-liquibase-preconditions and check-service-contract-quality each "
    "combine a .sh wrapper + .py implementation into one row (matching the "
    "P0 baseline table's convention in docs/aiae-migration-log.md), so 19 "
    "checker rows account for 21 files, plus the 7 library/utility rows "
    "above = 28 files total."
)
PY

echo
echo "==> Coverage phase"
COVERAGE_PHASE="engineering"
COVERAGE_MAVEN_ARGS=()
if [ -f scripts/lib/coverage-phase.sh ]; then
  # shellcheck source=./lib/coverage-phase.sh
  . scripts/lib/coverage-phase.sh
  COVERAGE_PHASE="$(coverage_phase_read . || echo engineering)"
  coverage_phase_announce "${COVERAGE_PHASE}"
  phase_args="$(coverage_phase_maven_args "${COVERAGE_PHASE}")"
  [ -n "${phase_args}" ] && COVERAGE_MAVEN_ARGS=("${phase_args}")
fi

if command -v mvn >/dev/null 2>&1; then
  echo "==> Backend: mvn clean verify"
  # ${arr[@]+...} because macOS ships bash 3.2, where "${arr[@]}" on an empty
  # array is an "unbound variable" error under set -u.
  if mvn -f backend/pom.xml -B ${COVERAGE_MAVEN_ARGS[@]+"${COVERAGE_MAVEN_ARGS[@]}"} clean verify; then
    record_step "Backend: mvn clean verify" "PASS"
  else
    record_step "Backend: mvn clean verify" "FAIL (exit $?)"
  fi
else
  record_step "Backend: mvn clean verify" "SKIP (mvn not on PATH)"
fi

if [ -f frontend/package.json ]; then
  echo "==> Frontend: install"
  NPM_BIN="$(pwd)/backend/application/target/frontend-toolchain/node/npm"
  if [ -x "${NPM_BIN}" ]; then
    export PATH="$(dirname "${NPM_BIN}"):${PATH}"
  else
    NPM_BIN="npm"
  fi
  if ( cd frontend && \
      { [ -f package-lock.json ] && "${NPM_BIN}" ci --no-audit --no-fund || "${NPM_BIN}" install --no-audit --no-fund; } ); then
    record_step "Frontend: install" "PASS"

    echo "==> Frontend: lint (report-only — see header comment)"
    if ( cd frontend && "${NPM_BIN}" run lint ); then
      record_step "Frontend: lint (report-only)" "PASS"
    else
      record_step "Frontend: lint (report-only)" "FAIL (exit $? — known backlog, does not block; see docs/aiae-migration-log.md P12)"
    fi

    echo "==> Frontend: test + build"
    if ( cd frontend && "${NPM_BIN}" test && "${NPM_BIN}" run build ); then
      record_step "Frontend: test + build" "PASS"
    else
      record_step "Frontend: test + build" "FAIL (exit $?)"
    fi
  else
    record_step "Frontend: install" "FAIL (exit $?)"
    record_step "Frontend: lint (report-only)" "SKIP (install failed)"
    record_step "Frontend: test + build" "SKIP (install failed)"
  fi
else
  record_step "Frontend: install" "SKIP (no frontend/package.json)"
  record_step "Frontend: lint (report-only)" "SKIP (no frontend/package.json)"
  record_step "Frontend: test + build" "SKIP (no frontend/package.json)"
fi

if [ -f docker-compose.yml ]; then
  echo "==> docker compose config (syntax check, no run)"
  if docker compose --profile local config >/dev/null; then
    record_step "docker-compose.yml config" "PASS"
  else
    record_step "docker-compose.yml config" "FAIL (exit $?)"
  fi
else
  # P3 step 4 renames docker-compose.yaml -> docker-compose.yml; until then
  # this is an expected, tracked gap, not a new P2 defect.
  record_step "docker-compose.yml config" "SKIP (docker-compose.yml absent — docker-compose.yaml exists; P3 step 4 renames it)"
fi

echo
echo "############################################################"
echo "# local-verify.sh — summary                                  #"
echo "############################################################"
for i in "${!STEP_NAMES[@]}"; do
  printf '  %-42s %s\n' "${STEP_NAMES[$i]}" "${STEP_RESULTS[$i]}"
done
echo
echo "See scripts/lib/ report above for all 28 checker files."

# Real exit code: non-zero if any step outside NON_BLOCKING_STEPS recorded a
# FAIL. structure-lint.sh/verify-gates.sh are expected to report the
# five-row carried set every time (docs/aiae-migration-plan.md P15) — that
# is not compared against here (no allow-list mechanism exists to do it
# mechanically); confirm it by reading the "verify-gates.sh"/
# "structure-lint.sh" lines' printed failure lists against the plan.
OVERALL_RC=0
for i in "${!STEP_NAMES[@]}"; do
  name="${STEP_NAMES[$i]}"
  result="${STEP_RESULTS[$i]}"
  is_non_blocking=0
  for nb in "${NON_BLOCKING_STEPS[@]}"; do
    [ "${name}" = "${nb}" ] && is_non_blocking=1 && break
  done
  if [ "${is_non_blocking}" -eq 0 ] && [[ "${result}" == FAIL* ]]; then
    OVERALL_RC=1
  fi
done

if [ "${OVERALL_RC}" -eq 0 ]; then
  echo "==> local-verify.sh: all blocking checks passed"
else
  echo "==> local-verify.sh: at least one blocking check failed — see summary above"
fi
exit "${OVERALL_RC}"
