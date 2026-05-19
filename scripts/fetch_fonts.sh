#!/usr/bin/env bash
# Downloads Cinzel + Inter from the google/fonts GitHub mirror into
# app/src/main/res/font/. Both are SIL/OFL licensed.
# Run from repo root:  bash scripts/fetch_fonts.sh
#
# Why the URLs look weird: google/fonts ships these as VARIABLE fonts whose
# filenames contain literal '[' and ']' characters that must be percent-
# encoded for curl. Variable fonts work fine with Compose Font(R.font.xxx) —
# Android picks the requested weight axis automatically.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FONT="$ROOT/app/src/main/res/font"
mkdir -p "$FONT"

BASE="https://raw.githubusercontent.com/google/fonts/main/ofl"

# Each family has a primary URL (variable font, always present) and a
# fallback URL (static cut, present for some families). We try both before
# giving up so a single 404 doesn't fail the whole script.
download() {
  local out="$1"; shift
  local tmp; tmp="$(mktemp)"
  for url in "$@"; do
    if curl -fsSL --retry 2 -o "$tmp" "$url" && [ -s "$tmp" ]; then
      mv "$tmp" "$out"
      echo "  ok  $(basename "$out")  <-  $url"
      return 0
    fi
  done
  rm -f "$tmp"
  echo "  FAIL $(basename "$out") — all sources 404'd:" >&2
  for url in "$@"; do echo "        $url" >&2; done
  return 1
}

echo "Fetching display font (Cinzel SemiBold)..."
download "$FONT/cinzel_semibold.ttf" \
  "$BASE/cinzel/Cinzel%5Bwght%5D.ttf" \
  "$BASE/cinzel/static/Cinzel-SemiBold.ttf"

echo "Fetching body font (Inter Regular)..."
download "$FONT/inter_regular.ttf" \
  "$BASE/inter/Inter%5Bopsz%2Cwght%5D.ttf" \
  "$BASE/inter/static/Inter_18pt-Regular.ttf" \
  "$BASE/inter/static/Inter-Regular.ttf"

echo
echo "Done. Sizes:"
echo "  cinzel_semibold.ttf : $(wc -c <"$FONT/cinzel_semibold.ttf") bytes"
echo "  inter_regular.ttf   : $(wc -c <"$FONT/inter_regular.ttf") bytes"
