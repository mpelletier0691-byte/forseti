#!/usr/bin/env bash
# Create a tarball of the Forseti tree for USB / offline transfer to a laptop.
# Excludes build outputs and local SDK paths.
#
#   bash scripts/pack_for_laptop.sh
#   bash scripts/pack_for_laptop.sh /path/to/output-dir
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="${1:-$(dirname "$ROOT")}"
STAMP="$(date +%Y%m%d)"
ARCHIVE="$OUT_DIR/forseti-laptop-${STAMP}.tar.gz"
NAME="$(basename "$ROOT")"

mkdir -p "$OUT_DIR"

echo "Packing $ROOT -> $ARCHIVE"
tar -czf "$ARCHIVE" -C "$(dirname "$ROOT")" \
  --exclude='.git/objects/pack/*.pack' \
  --exclude='.gradle' \
  --exclude='build' \
  --exclude='app/build' \
  --exclude='.idea' \
  --exclude='local.properties' \
  --exclude='*.iml' \
  --exclude='.DS_Store' \
  "$NAME"

ls -lh "$ARCHIVE"
echo ""
echo "On the laptop:"
echo "  tar -xzf $(basename "$ARCHIVE") -C ~/Desktop/Projects/"
echo "  cd ~/Desktop/Projects/$NAME"
echo "  bash scripts/laptop_sync.sh --skip-pull --assets --fonts"
