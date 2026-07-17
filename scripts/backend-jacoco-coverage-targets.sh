#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  JAVA_21_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  if [ -n "${JAVA_21_HOME}" ]; then
    export JAVA_HOME="${JAVA_21_HOME}"
    export PATH="${JAVA_HOME}/bin:${PATH}"
  fi
fi

if ! java -version 2>&1 | head -n 1 | grep -Eq 'version "21([.]|\")'; then
  echo "Java 21 is required for JaCoCo coverage analysis." >&2
  exit 1
fi

echo "==> Running backend tests and generating JaCoCo reports"
mvn -f backend/pom.xml -Dskip.frontend=true test

echo
echo "==> Classes below 80% line coverage, sorted by missed lines"
echo "module,line_pct,missed_lines,covered_lines,class"

find backend -path '*/target/site/jacoco/jacoco.csv' -print0 \
  | xargs -0 awk -F, '
    FNR == 1 { next }
    {
      module = FILENAME
      sub(/^backend\//, "", module)
      sub(/\/target\/site\/jacoco\/jacoco.csv$/, "", module)

      missed = $4 + 0
      covered = $5 + 0
      total = missed + covered
      if (total == 0) {
        next
      }

      pct = covered / total
      if (pct < 0.80) {
        class_name = $2 "." $3
        rows[++count] = module "," sprintf("%.2f", pct * 100) "," missed "," covered "," class_name
        missed_lines[count] = missed
      }
    }
    END {
      for (i = 1; i <= count; i++) {
        for (j = i + 1; j <= count; j++) {
          if (missed_lines[j] > missed_lines[i]) {
            tmp = rows[i]; rows[i] = rows[j]; rows[j] = tmp
            tmp_missed = missed_lines[i]; missed_lines[i] = missed_lines[j]; missed_lines[j] = tmp_missed
          }
        }
      }
      for (i = 1; i <= count; i++) {
        print rows[i]
      }
    }'

echo
echo "==> Strict handoff gate"
echo "Run this after adding tests:"
echo "JAVA_HOME=\"${JAVA_HOME:-}\" mvn -f backend/pom.xml -Dskip.frontend=true -Phandoff test"
