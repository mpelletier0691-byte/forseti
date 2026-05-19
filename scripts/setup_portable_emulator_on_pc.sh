#!/usr/bin/env bash
# Run ONCE on a new Linux PC after copying AndroidPortable from USB to ~/AndroidPortable
set -euo pipefail

PORTABLE="${1:-$HOME/AndroidPortable}"
if [ ! -d "$PORTABLE/Sdk/emulator" ]; then
  echo "Expected $PORTABLE/Sdk/emulator — copy AndroidPortable from the USB first." >&2
  exit 1
fi

LAUNCHER="$PORTABLE/scripts/launch_emulator_portable.sh"
DESKTOP="$HOME/Desktop/Forseti-Android-Emulator-Portable.desktop"

cat > "$DESKTOP" <<EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=Android Emulator (Pixel 9 portable)
Comment=Portable Pixel_9_API_35 from AndroidPortable
Exec=$LAUNCHER
Icon=phone
Terminal=false
Categories=Development;
EOF

chmod +x "$DESKTOP" "$LAUNCHER"
cp "$DESKTOP" "$HOME/.local/share/applications/" 2>/dev/null || true
update-desktop-database "$HOME/.local/share/applications" 2>/dev/null || true

echo "Installed desktop shortcut: $DESKTOP"
echo ""
echo "ANDROID_HOME=$PORTABLE/Sdk"
echo "ANDROID_AVD_HOME=$PORTABLE/avd"
echo ""
"$PORTABLE/Sdk/emulator/emulator" -list-avds || true
echo ""
if groups | grep -q kvm; then
  echo "KVM: yes (hardware acceleration available)"
else
  echo "KVM: not in group — optional: sudo usermod -aG kvm \$USER  then log out/in"
fi
echo ""
echo "Right-click the desktop icon → Allow Launching, then double-click to start."
