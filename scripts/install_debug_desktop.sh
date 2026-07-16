#!/usr/bin/env bash
set -euo pipefail
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

wait_for_device() {
  local i=0
  adb start-server >/dev/null 2>&1 || true
  while [ "$i" -lt 90 ]; do
    if adb devices 2>/dev/null | grep -qE 'emulator-[0-9]+\s+device'; then
      return 0
    fi
    if adb devices 2>/dev/null | grep -qE 'emulator-[0-9]+\s+offline'; then
      adb kill-server >/dev/null 2>&1 || true
      adb start-server >/dev/null 2>&1 || true
    fi
    sleep 2
    i=$((i + 1))
  done
  return 1
}

echo "Waiting for emulator (up to 3 min)…"
if ! wait_for_device; then
  echo "No emulator detected."
  echo "1. Double-click: Forseti — Android Emulator (Pixel 9)"
  echo "2. Wait for the Android home screen"
  echo "3. Run this shortcut again"
  read -r -p "Press Enter to close…"
  exit 1
fi

./gradlew :app:installDebug
adb shell am start -n com.forseti.debug/com.forseti.MainActivity
echo "Installed com.forseti.debug"
read -r -p "Press Enter to close…"
