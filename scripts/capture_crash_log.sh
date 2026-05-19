#!/usr/bin/env bash
# Save a short crash log from a connected device/emulator.
set -euo pipefail
OUT="${1:-/tmp/forseti-crash.log}"
adb wait-for-device
adb logcat -c
echo "Log cleared. Reproduce the crash, then press Enter."
read -r _
adb logcat -d > "$OUT"
grep -E "FATAL|AndroidRuntime|com\\.forseti|ANR" "$OUT" | tail -120 || true
echo "Full log: $OUT"
