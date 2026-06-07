#!/usr/bin/env bash
# Sync static Forseti site from landing/ → docs/ (forseti repo Pages)
# and optionally → ../forseti_landing/ (forseti_landing repo Pages).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/landing"
DOCS="$ROOT/docs"
ALT="${FORSETI_LANDING_REPO:-$ROOT/../forseti_landing}"

FILES=(index.html privacy-policy.html terms-of-use.html privacy-policy.md terms-of-use.md styles.css)

echo "==> Syncing landing → docs/"
for f in "${FILES[@]}"; do
  cp -f "$SRC/$f" "$DOCS/$f"
  echo "  $f"
done

if [[ -d "$ALT" ]]; then
  echo "==> Syncing landing → $ALT/"
  for f in "${FILES[@]}"; do
    cp -f "$SRC/$f" "$ALT/$f"
    echo "  $f"
  done
  echo "Done. Push forseti_landing repo to update https://mpelletier0691-byte.github.io/forseti_landing/"
else
  echo "==> Skip forseti_landing (clone to $ALT or set FORSETI_LANDING_REPO)"
fi

echo "Done. Push forseti main to update https://mpelletier0691-byte.github.io/forseti/"
