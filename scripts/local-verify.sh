#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -f .env.local ]; then
  set -a
  # shellcheck disable=SC1091
  source .env.local
  set +a
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local}"

if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  JAVA_21_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  if [ -n "${JAVA_21_HOME}" ]; then
    export JAVA_HOME="${JAVA_21_HOME}"
    export PATH="${JAVA_HOME}/bin:${PATH}"
  fi
fi

if ! java -version 2>&1 | head -n 1 | grep -Eq 'version "21([.]|\")'; then
  echo "Java 21 is required for local verification." >&2
  exit 1
fi

mvn -f backend/pom.xml -Dskip.frontend=true test

cd frontend
if [ -f package-lock.json ]; then
  npm ci
else
  npm install
fi
npm run generate:api
npm run typecheck
npm test
npm run build
