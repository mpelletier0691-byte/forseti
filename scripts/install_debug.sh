#!/usr/bin/env bash
# Build and install Forseti debug APK on a running emulator/device.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

cd "$ROOT"
./gradlew :app:installDebug

echo "Installed com.forseti.debug — launching…"
adb shell am start -n com.forseti.debug/com.forseti.MainActivity
