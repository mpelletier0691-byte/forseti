#!/usr/bin/env bash
# Build and install Forseti debug APK on a running emulator/device.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

cd "$ROOT"

if adb devices 2>/dev/null | grep -qE 'emulator-[0-9]+\s+device'; then
  FREE_KB=$(adb shell df /data 2>/dev/null | tail -1 | awk '{print $4}' | tr -d '\r' || echo 0)
  if [ -n "$FREE_KB" ] && [ "$FREE_KB" -lt 300000 ] 2>/dev/null; then
    echo "ERROR: Emulator /data is nearly full (${FREE_KB} KB free)."
    echo "Run: bash scripts/wipe_emulator_and_install.sh"
    echo "Or:  bash scripts/emulator_free_space.sh"
    exit 2
  fi
fi

./gradlew :app:installDebug

echo "Installed com.forseti.debug (30-day dev trial, separate from Play com.forseti)."
echo "Fresh reset: adb shell pm clear com.forseti.debug && $0"
echo "Launching…"
adb shell am start -n com.forseti.debug/com.forseti.MainActivity
