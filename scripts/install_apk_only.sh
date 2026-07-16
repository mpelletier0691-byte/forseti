#!/usr/bin/env bash
# Install a already-built debug APK (skips Gradle installDebug session overhead).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

if [ ! -f "$APK" ]; then
  echo "APK not found. Build first:"
  echo "  cd $ROOT && ./gradlew :app:assembleDebug"
  exit 1
fi

if ! adb devices 2>/dev/null | grep -qE 'emulator-[0-9]+\s+device|device$'; then
  echo "No device/emulator connected."
  exit 1
fi

echo "Installing $APK …"
adb uninstall com.forseti.debug 2>/dev/null || true
if adb install -r -d -t "$APK"; then
  echo "Installed com.forseti.debug"
  adb shell dumpsys package com.forseti.debug | grep versionName || true
  adb shell am start -n com.forseti.debug/com.forseti.MainActivity
else
  echo ""
  echo "Install failed (usually emulator /data full). Run:"
  echo "  bash scripts/emulator_free_space.sh --aggressive"
  echo "Or wipe and reinstall:"
  echo "  bash scripts/wipe_emulator_and_install.sh"
  exit 1
fi
