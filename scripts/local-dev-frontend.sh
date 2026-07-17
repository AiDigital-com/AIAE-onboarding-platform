#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -f .env.local ]; then
  set -a
  # shellcheck disable=SC1091
  source .env.local
  set +a
fi

export BACKEND_DEV_PORT="${BACKEND_DEV_PORT:-5000}"
export VITE_CLERK_JWT_TEMPLATE="${VITE_CLERK_JWT_TEMPLATE:-aidigital-api}"
export VITE_CLERK_PUBLISHABLE_KEY="${VITE_CLERK_PUBLISHABLE_KEY:-${CLERK_PUBLISHABLE_KEY:-}}"

cd frontend
if [ -f package-lock.json ]; then
  npm ci
else
  npm install
fi
npm run generate:api
exec npm run dev -- --host 0.0.0.0 --port 5173
