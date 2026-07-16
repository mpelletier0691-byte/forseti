#!/usr/bin/env bash
# Detached emulator launch for .desktop shortcuts (double-click from Desktop).
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"

AVD="${1:-Pixel_9_API_35}"
EMULATOR="$ANDROID_HOME/emulator/emulator"
LOG="$HOME/.android/emulator-launch.log"
LOCK="$HOME/.android/avd/${AVD}.avd/multiinstance.lock"

notify() {
  notify-send "Forseti — Android Emulator" "$1" 2>/dev/null || true
}

if [ ! -x "$EMULATOR" ]; then
  notify "SDK emulator not found at:\n$EMULATOR\n\nInstall Android SDK or set ANDROID_HOME."
  exit 1
fi

if ! "$EMULATOR" -list-avds 2>/dev/null | grep -qx "$AVD"; then
  LIST=$("$EMULATOR" -list-avds  2>/dev/null | tr '\n' ' ')
  notify "AVD not found: $AVD\n\nInstalled: ${LIST:-none}\nCreate Pixel_9_API_35 in Android Studio → Device Manager."
  exit 1
fi

# Refresh ADB and detect a healthy emulator.
adb start-server >/dev/null 2>&1 || true
if adb devices 2>/dev/null | grep -qE 'emulator-[0-9]+\s+device'; then
  notify "Emulator already running.\nUse Forseti — Install on Emulator next."
  exit 0
fi

# Offline/stuck ADB entries — reset daemon before starting a new AVD.
if adb devices 2>/dev/null | grep -qE 'emulator-[0-9]+\s+offline'; then
  adb kill-server >/dev/null 2>&1 || true
  adb start-server >/dev/null 2>&1 || true
fi

# Stale lock with no qemu process — safe to remove.
if [ -f "$LOCK" ] && ! pgrep -f "qemu-system.*-avd $AVD" >/dev/null 2>&1; then
  rm -f "$LOCK"
fi

if [ ! -r /dev/kvm ] || [ ! -w /dev/kvm ]; then
  notify "KVM not available (/dev/kvm).\nEnable VT-x in BIOS and:\n  sudo usermod -aG kvm \$USER\nThen log out and back in.\n\nEmulator may fail to start."
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
notify "Starting $AVD…\nWait for the home screen, then run\nForseti — Install on Emulator.\n\nLog: $LOG"
