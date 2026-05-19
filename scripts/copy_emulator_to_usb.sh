#!/usr/bin/env bash
# Copy a portable Android emulator (Pixel_9_API_35) to a flash drive for another Linux PC.
#
# Usage:
#   ./scripts/copy_emulator_to_usb.sh                    # default: /media/user/MUNINNSHADE
#   ./scripts/copy_emulator_to_usb.sh /media/user/MYUSB
#
# Needs ~8 GB free on the destination. Excludes boot snapshots to save ~2 GB.
set -euo pipefail

USB="${1:-/media/user/MUNINNSHADE}"
SRC_SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
DEST="$USB/AndroidPortable"
AVD_NAME="Pixel_9_API_35"

if [ ! -d "$USB" ]; then
  echo "USB not mounted at: $USB" >&2
  echo "Plug in the drive and pass the mount path as the first argument." >&2
  exit 1
fi

AVAIL="$(df -BM "$USB" | awk 'NR==2 {gsub(/M/,"",$4); print $4}')"
if [ "${AVAIL:-0}" -lt 8500 ]; then
  echo "Warning: only ${AVAIL} MB free on $USB — need about 8500 MB for the portable emulator." >&2
  read -r -p "Continue anyway? [y/N] " ans
  [[ "${ans,,}" == "y" ]] || exit 1
fi

echo "Copying portable emulator to $DEST (this takes several minutes)…"

mkdir -p "$DEST/Sdk" "$DEST/avd" "$DEST/scripts"

rsync -a --info=progress2 \
  "$SRC_SDK/emulator/" \
  "$DEST/Sdk/emulator/"

rsync -a --info=progress2 \
  "$SRC_SDK/platform-tools/" \
  "$DEST/Sdk/platform-tools/"

rsync -a --info=progress2 \
  "$SRC_SDK/system-images/" \
  "$DEST/Sdk/system-images/"

# AVD data (skip heavy snapshots — they are recreated on first boot)
rsync -a --info=progress2 \
  --exclude 'snapshots/' \
  --exclude 'cache.img.qcow2' \
  "$HOME/.android/avd/${AVD_NAME}.avd/" \
  "$DEST/avd/${AVD_NAME}.avd/"

# Portable .ini (relative path — works with ANDROID_AVD_HOME)
cat > "$DEST/avd/${AVD_NAME}.ini" <<EOF
avd.ini.encoding=UTF-8
path=${AVD_NAME}.avd
path.rel=${AVD_NAME}.avd
target=android-35
EOF

# Launcher scripts
cp "$(dirname "$0")/launch_emulator_portable.sh" "$DEST/scripts/"
cp "$(dirname "$0")/setup_portable_emulator_on_pc.sh" "$DEST/scripts/"
chmod +x "$DEST/scripts/"*.sh

# Optional: Forseti project is separate (run copy separately or already on USB)
if [ -d "$USB/Forseti" ]; then
  cp "$(dirname "$0")/install_debug_desktop.sh" "$DEST/scripts/" 2>/dev/null || true
fi

cat > "$DEST/SETUP_OTHER_PC.txt" <<'EOF'
Portable Android emulator — other Linux PC
==========================================

Requirements on the OTHER computer:
  - Linux x86_64 (Intel/AMD). Not for Windows/Mac without a different SDK.
  - ~10 GB free if you copy AndroidPortable to the local disk (recommended).
  - KVM optional but faster: sudo usermod -aG kvm $USER  (log out/in)

SETUP (once per PC)
-------------------
1. Copy the whole "AndroidPortable" folder from this USB to your home folder:
     cp -a /path/to/USB/AndroidPortable ~/AndroidPortable

   Working directly off the USB is possible but slow.

2. Run the setup script:
     ~/AndroidPortable/scripts/setup_portable_emulator_on_pc.sh

   This installs Desktop shortcuts and checks KVM.

3. Double-click "Android Emulator (Pixel 9)" on the Desktop.

RUN WITHOUT COPYING (slow, from USB)
------------------------------------
  /path/to/USB/AndroidPortable/scripts/launch_emulator_portable.sh

TEST FORSETI (if Forseti project is on this USB under Forseti/)
---------------------------------------------------------------
  cd /path/to/USB/Forseti
  export ANDROID_HOME=~/AndroidPortable/Sdk   # or USB path
  ./gradlew :app:installDebug

TROUBLESHOOTING
---------------
  Log: ~/.android/emulator-launch.log
  List AVDs: ANDROID_AVD_HOME=~/AndroidPortable/avd \
             ANDROID_HOME=~/AndroidPortable/Sdk \
             ~/AndroidPortable/Sdk/emulator/emulator -list-avds
EOF

echo ""
echo "Done. Portable emulator is at: $DEST"
echo "On the other PC, read: $DEST/SETUP_OTHER_PC.txt"
du -sh "$DEST"
