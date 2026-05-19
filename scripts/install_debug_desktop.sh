#!/usr/bin/env bash
set -euo pipefail
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if ! adb devices | grep -qE 'emulator-[0-9]+\s+device'; then
  echo "No emulator detected. Start 'Android Emulator (Pixel 9)' first, wait for home screen, then run this again."
  read -r -p "Press Enter to close…"
  exit 1
fi

./gradlew :app:installDebug
adb shell am start -n com.forseti.debug/com.forseti.MainActivity
echo "Done."
read -r -p "Press Enter to close…"
