#!/usr/bin/env bash
# Launch emulator from a portable AndroidPortable folder (USB or ~/AndroidPortable).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PORTABLE_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

export ANDROID_HOME="$PORTABLE_ROOT/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_AVD_HOME="$PORTABLE_ROOT/avd"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"

AVD="${1:-Pixel_9_API_35}"
EMULATOR="$ANDROID_HOME/emulator/emulator"

if [ ! -x "$EMULATOR" ]; then
  notify-send "Android Emulator" "Missing: $EMULATOR" 2>/dev/null || echo "Missing: $EMULATOR" >&2
  exit 1
fi

if adb devices 2>/dev/null | grep -q "emulator-.*device"; then
  notify-send "Android Emulator" "Already running." 2>/dev/null || true
  exit 0
fi

nohup "$EMULATOR" -avd "$AVD" -gpu auto >>"$HOME/.android/emulator-launch.log" 2>&1 &
disown
notify-send "Android Emulator" "Starting $AVD…" 2>/dev/null || true
