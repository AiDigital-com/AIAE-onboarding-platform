#!/usr/bin/env bash
#
# check-carried-assertions.sh — compare the gate failure list against the set
# this project carries red by decision.
#
# WHY THIS EXISTS
#
# `structure-lint.sh` and `verify-gates.sh` exit non-zero when anything fails,
# and this repository fails a small, fixed set of assertions on purpose — each
# one a recorded product or engineering decision, listed in
# `scripts/carried-assertions.txt` and explained in the P15 section of
# `docs/aiae-migration-plan.md`.
#
# So the exit code of those scripts is not a usable CI signal: it is non-zero
# on a healthy tree and stays non-zero after a real regression. The usable
# signal is the failure LIST. This script compares it against the recorded set
# and fails only on a difference:
#
#   list matches the carried set     -> pass
#   a failure appears that is not in it -> fail (regression)
#   a carried failure disappears     -> fail (fixed, but the list wasn't updated)
#
# The second direction matters as much as the first. `verify-gates.sh` has no
# allow-list, no annotations and no exemptions (see CR-4), so without this
# comparison a silently disappearing assertion looks identical to one that was
# never checked.
#
# Run it locally exactly as CI does:  bash scripts/check-carried-assertions.sh

set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "${ROOT}" || exit 1

EXPECTED_FILE="scripts/carried-assertions.txt"
if [ ! -f "${EXPECTED_FILE}" ]; then
	echo "check-carried-assertions: ${EXPECTED_FILE} is missing" >&2
	exit 1
fi

TMP="$(mktemp -d)"
trap 'rm -rf "${TMP}"' EXIT

# Deliberately not `set -e` and deliberately no `||` guard: both gates are
# expected to exit non-zero here, and both must run to completion. Running
# them under `set -e` is what made CI abort on the first script and never
# execute the second.
bash scripts/structure-lint.sh >"${TMP}/structure-lint.log" 2>&1
bash scripts/verify-gates.sh >"${TMP}/verify-gates.log" 2>&1

cat "${TMP}/structure-lint.log" "${TMP}/verify-gates.log"

# Both scripts end with:
#     ==> <name>: N assertion(s) failed
#       1. <assertion text>
# Read only the numbered lines belonging to that trailing block, so unrelated
# indented output (for example check-frontend-ui-rules' per-file violations)
# cannot be mistaken for an assertion.
extract_failures() {
	awk '
		/^==> .*assertion\(s\) failed$/ { in_block = 1; next }
		in_block && /^  [0-9]+\. /      { sub(/^  [0-9]+\. /, ""); print; next }
		in_block                        { in_block = 0 }
	' "$1"
}

{
	extract_failures "${TMP}/structure-lint.log"
	extract_failures "${TMP}/verify-gates.log"
} | sed 's/[[:space:]]*$//' | sort >"${TMP}/actual"

grep -vE '^[[:space:]]*(#|$)' "${EXPECTED_FILE}" \
	| sed 's/[[:space:]]*$//' | sort >"${TMP}/expected"

new_failures="$(comm -13 "${TMP}/expected" "${TMP}/actual")"
gone_failures="$(comm -23 "${TMP}/expected" "${TMP}/actual")"

if [ -z "${new_failures}" ] && [ -z "${gone_failures}" ]; then
	echo ""
	echo "check-carried-assertions: OK — $(wc -l <"${TMP}/actual" | tr -d ' ') carried assertion(s), nothing new"
	exit 0
fi

echo "" >&2
echo "==> check-carried-assertions: FAIL — the gate failure list does not match the carried set" >&2
echo "    recorded in ${EXPECTED_FILE}; rationale in docs/aiae-migration-plan.md (P15)" >&2
echo "" >&2

if [ -n "${new_failures}" ]; then
	printf '%s\n' "${new_failures}" | sed 's/^/  NEW FAILURE          /' >&2
	echo "" >&2
	echo "  A new failure is a regression. Fix it — do not add it to the list." >&2
	echo "  Adding it is only correct when it records a deliberate, documented decision." >&2
fi

if [ -n "${gone_failures}" ]; then
	[ -n "${new_failures}" ] && echo "" >&2
	printf '%s\n' "${gone_failures}" | sed 's/^/  NO LONGER FAILING    /' >&2
	echo "" >&2
	echo "  A carried assertion started passing. That is good news, but the list must" >&2
	echo "  be updated in the same commit, or the next real regression hides behind it." >&2
fi

echo "" >&2
exit 1
