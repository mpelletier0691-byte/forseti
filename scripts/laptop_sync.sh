#!/usr/bin/env bash
# Pull cloud/agent work onto your laptop and optionally refresh bundled assets.
#
# Run on your real machine (not the Cursor cloud sandbox):
#   bash scripts/laptop_sync.sh
#   bash scripts/laptop_sync.sh --assets --fonts
#   bash scripts/laptop_sync.sh cursor/my-branch-68c3 --assets
#   bash scripts/laptop_sync.sh --skip-pull --assets   # tree already unpacked from USB
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

WITH_ASSETS=0
WITH_FONTS=0
SKIP_PULL=0
BRANCH=""

usage() {
  sed -n '2,12p' "$0" | sed 's/^# \?//'
  exit "${1:-0}"
}

while [ $# -gt 0 ]; do
  case "$1" in
    -h|--help) usage 0 ;;
    --assets) WITH_ASSETS=1 ;;
    --fonts) WITH_FONTS=1 ;;
    --skip-pull) SKIP_PULL=1 ;;
    --) shift; break ;;
    -*) echo "Unknown option: $1" >&2; usage 1 ;;
    *)
      if [ -z "$BRANCH" ]; then
        BRANCH="$1"
      else
        echo "Unexpected argument: $1" >&2
        usage 1
      fi
      ;;
  esac
  shift
done

log() { printf '\n== %s\n' "$*"; }

if [ "$SKIP_PULL" -eq 0 ]; then
  if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "Not a git repository: $ROOT" >&2
    echo "Clone first: git clone https://github.com/mpelletier0691-byte/forseti.git" >&2
    exit 1
  fi
  log "Fetching from origin"
  git fetch origin
  if [ -n "$BRANCH" ]; then
    log "Checking out $BRANCH"
    git checkout "$BRANCH"
    git pull origin "$BRANCH"
  else
    log "Pulling current branch ($(git branch --show-current))"
    git pull --ff-only
  fi
  printf '  at %s\n' "$(git rev-parse --short HEAD) — $(git log -1 --format=%s)"
else
  log "Skipping git pull (--skip-pull)"
fi

if [ "$WITH_ASSETS" -eq 1 ]; then
  log "Bundling FRCP PDF and forms (fetch_assets.sh)"
  bash "$ROOT/scripts/fetch_assets.sh"
fi

if [ "$WITH_FONTS" -eq 1 ]; then
  log "Bundling fonts (fetch_fonts.sh)"
  bash "$ROOT/scripts/fetch_fonts.sh"
fi

log "Next steps on this laptop"
echo "  Docs:  docs/CLOUD_WORKSPACE.md"
echo ""
if [ ! -f "$ROOT/local.properties" ]; then
  echo "  First-time SDK + emulator:"
  echo "    bash bootstrap.sh"
  echo ""
  echo "  Or SDK already installed, just run the app:"
  echo "    bash bootstrap.sh --launch-only"
else
  echo "  Rebuild and install on emulator/device:"
  echo "    bash bootstrap.sh --launch-only"
  echo "    # or: ./gradlew :app:installDebug"
fi
echo ""
echo "  Portable emulator on USB / second PC:"
echo "    bash scripts/copy_emulator_to_usb.sh"
echo ""
echo "  Demo walkthrough:"
echo "    docs/QA_CHECKLIST.md"
