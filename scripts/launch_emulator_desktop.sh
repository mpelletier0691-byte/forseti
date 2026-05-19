#!/usr/bin/env bash
# Detached emulator launch for .desktop shortcuts (double-click from Desktop).
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"

AVD="${1:-Pixel_9_API_35}"
EMULATOR="$ANDROID_HOME/emulator/emulator"
LOG="$HOME/.android/emulator-launch.log"

notify() {
  notify-send "Forseti — Android Emulator" "$1" 2>/dev/null || true
}

if [ ! -x "$EMULATOR" ]; then
  notify "SDK emulator not found at:\n$EMULATOR\n\nInstall Android SDK or set ANDROID_HOME."
  exit 1
fi

if ! "$EMULATOR" -list-avds 2>/dev/null | grep -qx "$AVD"; then
  LIST=$("$EMULATOR" -list-avds 2>/dev/null | tr '\n' ' ')
  notify "AVD not found: $AVD\n\nInstalled: ${LIST:-none}\nCreate Pixel_9_API_35 in Android Studio → Device Manager."
  exit 1
fi

if adb devices 2>/dev/null | grep -qE 'emulator-[0-9]+\s+device'; then
  notify "An emulator is already running.\nUse Forseti — Install on Emulator, or run adb devices."
  exit 0
fi

mkdir -p "$HOME/.android"
GPU_FLAGS=(-gpu auto)
if [ -r /dev/kvm ] && [ -w /dev/kvm ]; then
  GPU_FLAGS=(-accel on -gpu host)
fi

{
  echo "=== $(date -Iseconds) starting $AVD ==="
  nohup "$EMULATOR" -avd "$AVD" "${GPU_FLAGS[@]}"
} >>"$LOG" 2>&1 &

disown
notify "Starting $AVD…\nLog: $LOG"
