#!/usr/bin/env bash
# Sync static Forseti site from landing/ → docs/ (forseti repo Pages)
# and optionally → ../forseti_landing/ (forseti_landing repo Pages).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/landing"
DOCS="$ROOT/docs"
ALT="${FORSETI_LANDING_REPO:-$ROOT/../forseti_landing}"

FILES=(index.html privacy-policy.html terms-of-use.html privacy-policy.md terms-of-use.md styles.css fgs-demo.html)

sync_tree() {
  local dest="$1"
  mkdir -p "$dest"
  for f in "${FILES[@]}"; do
    cp -f "$SRC/$f" "$dest/$f"
    echo "  $f"
  done
  if [[ -d "$SRC/media" ]]; then
    mkdir -p "$dest/media"
    cp -f "$SRC/media/"* "$dest/media/"
    echo "  media/*"
  fi
}

echo "==> Syncing landing → docs/"
sync_tree "$DOCS"

if [[ -d "$ALT" ]]; then
  echo "==> Syncing landing → $ALT/"
  sync_tree "$ALT"
  echo "Done. Push forseti_landing repo to update https://mpelletier0691-byte.github.io/forseti_landing/"
else
  echo "==> Skip forseti_landing (clone to $ALT or set FORSETI_LANDING_REPO)"
fi

echo "Done. Push forseti main to update https://mpelletier0691-byte.github.io/forseti/"
