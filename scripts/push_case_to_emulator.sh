#!/usr/bin/env bash
# Push case files/folders to the emulator for Forseti ingest testing.
# Drag-and-drop in the emulator UI often fails when /data is full or paths are invalid.
set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

DEST="/sdcard/Download/Case_Files"
SRC="${1:-}"

if [ -z "$SRC" ]; then
  echo "Usage: $0 <file-or-folder-on-host>"
  echo "Example:"
  echo "  $0 \"$HOME/Desktop/Pelletier_Cases/Pelletier-v-Anderson_Cases/Pelletier_VS_Anderson_RI_Defimation\""
  exit 1
fi

if [ ! -e "$SRC" ]; then
  echo "Not found: $SRC"
  exit 1
fi

if ! adb devices 2>/dev/null | grep -qE 'emulator-[0-9]+\s+device'; then
  echo "No emulator connected. Start Pixel 9 first:"
  echo "  bash scripts/launch_emulator_desktop.sh"
  exit 1
fi

echo "=== Storage before push ==="
adb shell df -h /data /sdcard 2>/dev/null || adb shell df -h

FREE_KB=$(adb shell df /data 2>/dev/null | tail -1 | awk '{print $4}' | tr -d '\r' || echo 0)
if [ -n "$FREE_KB" ] && [ "$FREE_KB" -lt 200000 ] 2>/dev/null; then
  echo ""
  echo "ERROR: Emulator /data is nearly full (${FREE_KB} KB free)."
  echo "Run: bash scripts/emulator_free_space.sh"
  echo "Or wipe AVD: Device Manager → Pixel_9_API_35 → Wipe Data"
  exit 2
fi

adb shell mkdir -p "$DEST"

BASENAME=$(basename "$SRC")
REMOTE="$DEST/$BASENAME"

echo ""
echo "Pushing to emulator: $REMOTE"
if [ -d "$SRC" ]; then
  adb push "$SRC" "$REMOTE"
else
  adb push "$SRC" "$DEST/"
fi

echo ""
echo "Done. In Forseti: Edit case → Ingest folder → pick from Download/Case_Files"
adb shell ls -la "$DEST" 2>/dev/null | tail -10
