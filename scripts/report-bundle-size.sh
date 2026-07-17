#!/usr/bin/env bash
# Reports raw + gzip size of every emitted JS/CSS asset after `npm run build`.
# No bundle-analyzer dependency — this is the same measurement PERFORMANCE-AUDIT.md
# used for its baseline (one entry chunk: 1,777,761 bytes raw / 536,566 bytes gzip).
# Run this after `npm run build` (from `frontend/`) to compare against that baseline
# or against a prior run of this script.
set -euo pipefail

cd "$(dirname "$0")/.."

ASSETS_DIR="backend/application/src/main/resources/static/assets"

if [ ! -d "$ASSETS_DIR" ]; then
  echo "No build output at $ASSETS_DIR — run 'npm run build' in frontend/ first." >&2
  exit 1
fi

echo "== All static assets, largest first =="
find "$ASSETS_DIR/.." -maxdepth 3 -type f -print0 | xargs -0 ls -lhS

echo
echo "== JS/CSS raw vs gzip size =="
find "$ASSETS_DIR" -type f \( -name '*.js' -o -name '*.css' \) -print0 |
while IFS= read -r -d '' file; do
  raw_bytes=$(wc -c < "$file" | tr -d ' ')
  gzip_bytes=$(gzip -c "$file" | wc -c | tr -d ' ')
  printf '%s\traw=%s bytes\tgzip=%s bytes\n' "$file" "$raw_bytes" "$gzip_bytes"
done
