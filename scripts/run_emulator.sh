#!/usr/bin/env bash
# Start the Android emulator for Forseti development.
set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

AVD_NAME="${1:-Pixel_9_API_35}"

if ! command -v emulator >/dev/null; then
  echo "emulator not found. Set ANDROID_HOME=$ANDROID_HOME" >&2
  exit 1
fi

if ! avdmanager list avd 2>/dev/null | grep -q "Name: $AVD_NAME"; then
  echo "AVD '$AVD_NAME' not found. Create one in Android Studio Device Manager, or:" >&2
  echo "  avdmanager create avd -n Pixel_9_API_35 -k \"system-images;android-35;google_apis;x86_64\" -d pixel_9 --force" >&2
  exit 1
fi

echo "Starting emulator: $AVD_NAME"
echo "Tip: for faster emulation, run: sudo usermod -aG kvm \$USER  (then log out/in)"
exec emulator -avd "$AVD_NAME" -gpu auto
