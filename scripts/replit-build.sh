#!/usr/bin/env bash
#
# replit-build.sh — invoked by .replit [deployment].build.

set -euo pipefail

cd "$(dirname "$0")/.."

# Swap in the published-app values before Maven runs: the SPA bakes
# CLERK_PUBLISHABLE_KEY into the bundle at build time, so a runtime-only
# override would be too late. Must precede replit-env.sh, which derives
# VITE_CLERK_PUBLISHABLE_KEY from CLERK_PUBLISHABLE_KEY.
if [ -f scripts/lib/deploy-env.sh ]; then
  # shellcheck source=lib/deploy-env.sh
  . scripts/lib/deploy-env.sh
fi

source scripts/replit-env.sh

cd frontend
if [ -f package-lock.json ]; then
  npm ci
else
  npm install
fi
npm run generate:api
test -n "${VITE_CLERK_PUBLISHABLE_KEY:-}" || {
  echo "ERROR: VITE_CLERK_PUBLISHABLE_KEY or CLERK_PUBLISHABLE_KEY must be available during frontend build." >&2
  exit 1
}
npm run build
cd ..

STATIC_DIR="backend/application/src/main/resources/static"

# Vite now builds to frontend/dist so GitHub Actions can upload a standalone SPA
# artifact to S3/CloudFront on AWS. Replit still serves the SPA from inside the
# Spring jar, so stage the same output into the static directory here. Removed
# together with the rest of the Replit deployment path.
rm -rf "${STATIC_DIR}"
mkdir -p "${STATIC_DIR}"
cp -R frontend/dist/. "${STATIC_DIR}/"

test -f "${STATIC_DIR}/index.html" || {
  echo "ERROR: frontend build did not produce ${STATIC_DIR}/index.html." >&2
  exit 1
}

mvn -f backend/pom.xml -B -DskipTests -Dskip.frontend=true package

JAR="$(find backend/application/target -maxdepth 1 -name '*.jar' ! -name '*.original' | head -n 1)"
if [ -z "${JAR}" ]; then
  echo "ERROR: no Spring Boot jar produced in backend/application/target." >&2
  exit 1
fi

rm -rf backend/application/target/extracted
java -Djarmode=tools -jar "${JAR}" extract --destination backend/application/target/extracted || true
