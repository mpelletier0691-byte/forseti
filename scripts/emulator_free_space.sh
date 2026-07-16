#!/usr/bin/env bash
# Diagnose and free space on the Android emulator before installing Forseti.
set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

AVD="${1:-Pixel_9_API_35}"
AGGRESSIVE=false
if [ "${1:-}" = "--aggressive" ]; then
  AGGRESSIVE=true
  AVD="${2:-Pixel_9_API_35}"
elif [ "${2:-}" = "--aggressive" ]; then
  AGGRESSIVE=true
fi

AVD_DIR="$HOME/.android/avd/${AVD}.avd"
CONFIG="$AVD_DIR/config.ini"
SNAP_DIR="$AVD_DIR/snapshots"

echo "=== Emulator storage ==="
if ! adb devices 2>/dev/null | grep -qE 'emulator-[0-9]+\s+device'; then
  echo "No emulator connected. Start Pixel 9 first, then re-run this script."
  exit 1
fi

adb shell df -h /data /data/user/0 /sdcard 2>/dev/null || adb shell df -h

echo ""
echo "=== Forseti apps ==="
adb shell dumpsys package com.forseti.debug 2>/dev/null | grep versionName || echo "com.forseti.debug: (not installed)"
adb shell dumpsys package com.forseti 2>/dev/null | grep versionName || echo "com.forseti: (not installed)"
adb shell du -sh /data/data/com.forseti.debug 2>/dev/null || true
adb shell du -sh /data/data/com.forseti 2>/dev/null || true
adb shell du -sh /sdcard/Android/data/com.forseti.debug 2>/dev/null || true

echo ""
echo "=== Freeing space ==="
adb shell pm uninstall com.forseti.debug 2>/dev/null || true
adb shell pm uninstall com.forseti 2>/dev/null || true
adb shell pm trim-caches 999999M 2>/dev/null || true
adb shell rm -rf /data/local/tmp/* 2>/dev/null || true
adb shell rm -rf /sdcard/Download/* 2>/dev/null || true
adb shell rm -rf /sdcard/Android/data/com.forseti.debug 2>/dev/null || true
adb shell rm -rf /sdcard/Android/data/com.forseti 2>/dev/null || true

if $AGGRESSIVE; then
  echo "Aggressive: clearing dalvik-cache temp and stale staging…"
  adb shell rm -rf /data/dalvik-cache/arm64/* 2>/dev/null || true
  adb shell rm -rf /data/app/*/tmp* 2>/dev/null || true
fi

echo ""
echo "=== After cleanup ==="
adb shell df -h /data 2>/dev/null || adb shell df -h

FREE_KB=$(adb shell df /data 2>/dev/null | tail -1 | awk '{print $4}' | tr -d '\r')
MIN_KB=800000
if $AGGRESSIVE; then MIN_KB=400000; fi

if [ -n "$FREE_KB" ] && [ "$FREE_KB" -lt "$MIN_KB" ] 2>/dev/null; then
  echo ""
  echo "WARNING: Still only ~$((FREE_KB / 1024)) MB free on /data (need ~$((MIN_KB / 1024)) MB to install reliably)."
  echo ""
  echo "Recommended — wipe emulator (fastest fix):"
  echo "  bash scripts/wipe_emulator_and_install.sh"
  echo ""
  echo "Or manually:"
  echo "  1. Close emulator"
  echo "  2. Android Studio → Device Manager → $AVD → Wipe Data"
  echo "  3. Edit AVD → Internal Storage → 8192 MB (or larger)"
  if [ -d "$SNAP_DIR" ] && [ "$(ls -A "$SNAP_DIR" 2>/dev/null | wc -l)" -gt 0 ]; then
    echo "  4. Emulator stopped? Delete heavy snapshots: rm -rf \"$SNAP_DIR\"/*"
  fi
  if [ -f "$CONFIG" ]; then
    echo ""
    echo "Current AVD config:"
    grep -E 'disk.dataPartition.size|hw.ramSize|disk.cachePartition.size' "$CONFIG" 2>/dev/null || true
  fi
  exit 2
fi

echo ""
echo "Enough space to install. Options:"
echo "  bash scripts/install_apk_only.sh          # if already built"
echo "  ./gradlew :app:assembleDebug && bash scripts/install_apk_only.sh"
