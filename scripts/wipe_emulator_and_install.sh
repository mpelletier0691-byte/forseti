#!/usr/bin/env bash
# Wipe Pixel 9 AVD data (frees emulator disk after large Forseti ingests), then install debug APK.
set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"

AVD="${1:-Pixel_9_API_35}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "Stopping emulator…"
adb emu kill 2>/dev/null || true
pkill -f "qemu-system.*-avd $AVD" 2>/dev/null || true
sleep 2

echo "Wiping AVD data: $AVD (this erases ALL apps/files on the emulator)"
read -r -p "Continue? [y/N] " ans
if [ "${ans,,}" != "y" ]; then
  echo "Cancelled."
  exit 0
fi

echo "Cold boot with -wipe-data (first boot may take 2–3 min)…"
# -no-snapshot-save prevents a huge default_boot snapshot from refilling disk.
"$ANDROID_HOME/emulator/emulator" -avd "$AVD" -wipe-data -no-snapshot-load -no-snapshot-save &
EMU_PID=$!

echo "Waiting for device…"
adb wait-for-device
for i in $(seq 1 90); do
  if adb shell getprop sys.boot_completed 2>/dev/null | grep -q 1; then
    break
  fi
  sleep 2
done

echo "Boot complete. Installing Forseti…"
cd "$ROOT"
./gradlew :app:assembleDebug :app:installDebug
adb shell dumpsys package com.forseti.debug | grep versionName
adb shell am start -n com.forseti.debug/com.forseti.MainActivity
echo "Done. Emulator PID $EMU_PID (leave running)."
