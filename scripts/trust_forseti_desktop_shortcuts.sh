#!/usr/bin/env bash
# One-time: allow double-click launch on Ubuntu/GNOME for Desktop shortcuts.
set -euo pipefail
DESKTOP_DIR="${1:-$HOME/Desktop}"
for name in Forseti-Android-Emulator.desktop Forseti-Install-Debug.desktop; do
  f="$DESKTOP_DIR/$name"
  if [ -f "$f" ]; then
    chmod +x "$f"
    gio set "$f" metadata::trusted true 2>/dev/null || true
    echo "Trusted: $f"
  fi
done
chmod +x "$(dirname "$0")/launch_emulator_desktop.sh"
chmod +x "$(dirname "$0")/install_debug_desktop.sh"
echo "Done. Double-click the icons on your Desktop."
