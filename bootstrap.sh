#!/usr/bin/env bash
#
# bootstrap.sh - one-shot Linux setup for the Forseti project.
#
# Idempotent. You can re-run any time; each step skips work that is already
# done. Run with sudo for the apt/snap parts; the rest happens as your user.
#
#   bash bootstrap.sh                  # full setup + first build
#   bash bootstrap.sh --skip-launch    # everything but the emulator launch
#   bash bootstrap.sh --launch-only    # assume setup is done, just build + run
#
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
AVD_NAME="Pixel_9_API_35"
SYSIMG="system-images;android-35;google_apis;x86_64"
GRADLE_VERSION="8.10.2"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

SKIP_LAUNCH=0
LAUNCH_ONLY=0
for arg in "$@"; do
    case "$arg" in
        --skip-launch) SKIP_LAUNCH=1 ;;
        --launch-only) LAUNCH_ONLY=1 ;;
    esac
done

log()  { printf "\n\033[1;33m== %s\033[0m\n" "$*"; }
ok()   { printf "  \033[1;32m+\033[0m %s\n" "$*"; }
warn() { printf "  \033[1;31m!\033[0m %s\n" "$*"; }

require_sudo() {
    if [[ $EUID -ne 0 ]] && ! sudo -n true 2>/dev/null; then
        log "This step needs sudo (apt/snap). You'll be prompted for your password."
        sudo -v
    fi
}

ensure_jdk17() {
    log "Ensuring JDK 17"
    local jdk17_path=/usr/lib/jvm/java-17-openjdk-amd64
    if [[ ! -d "$jdk17_path" ]]; then
        require_sudo
        sudo apt-get update -qq
        sudo apt-get install -y openjdk-17-jdk
        sudo update-alternatives --set java  "$jdk17_path/bin/java"
        sudo update-alternatives --set javac "$jdk17_path/bin/javac"
        ok "JDK 17 installed"
    fi
    export JAVA_HOME="$jdk17_path"
    export PATH="$JAVA_HOME/bin:$PATH"
    ok "JAVA_HOME = $JAVA_HOME"
}

ensure_studio() {
    log "Ensuring Android Studio"
    if command -v android-studio >/dev/null 2>&1 || [[ -d /snap/android-studio ]]; then
        ok "Android Studio already installed"
        return 0
    fi
    require_sudo
    sudo snap install android-studio --classic
    ok "Android Studio installed via snap"
}

ensure_kvm() {
    log "Checking KVM"
    if [[ -e /dev/kvm ]]; then
        ok "/dev/kvm present"
    else
        warn "/dev/kvm missing - emulator will be slow. Enable VT-x in BIOS and"
        warn "  sudo apt install -y qemu-kvm libvirt-daemon-system"
        warn "  sudo adduser \$USER kvm"
        warn "Re-login after that, then re-run this script."
    fi
}

ensure_cmdline_tools() {
    log "Ensuring Android command-line tools"
    if [[ -d "$SDK_ROOT/cmdline-tools/latest/bin" ]]; then
        ok "cmdline-tools already installed at $SDK_ROOT"
    else
        mkdir -p "$SDK_ROOT/cmdline-tools"
        local tmpdir
        tmpdir="$(mktemp -d)"
        log "Downloading cmdline-tools (~150 MB)"
        curl -fL --retry 3 -o "$tmpdir/cmdline.zip" "$CMDLINE_TOOLS_URL"
        unzip -q "$tmpdir/cmdline.zip" -d "$tmpdir"
        mkdir -p "$SDK_ROOT/cmdline-tools/latest"
        mv "$tmpdir/cmdline-tools/"* "$SDK_ROOT/cmdline-tools/latest/"
        rm -rf "$tmpdir"
        ok "cmdline-tools installed"
    fi
    export ANDROID_SDK_ROOT="$SDK_ROOT"
    export ANDROID_HOME="$SDK_ROOT"
    export PATH="$SDK_ROOT/cmdline-tools/latest/bin:$SDK_ROOT/platform-tools:$SDK_ROOT/emulator:$PATH"
}

install_sdk_packages() {
    log "Accepting Android SDK licenses"
    # `yes | sdkmanager` always trips pipefail because `yes` gets SIGPIPE
    # when sdkmanager closes stdin. Run in a subshell with pipefail off.
    (
        set +o pipefail
        yes | sdkmanager --licenses >/dev/null
    )
    ok "Licenses accepted"

    log "Installing SDK packages (~3 GB on first run, then cached)"
    sdkmanager --install \
        "platforms;android-35" \
        "build-tools;35.0.0" \
        "platform-tools" \
        "emulator" \
        "$SYSIMG"
    ok "SDK 35, build-tools, platform-tools, emulator, Pixel system image installed"
}

pick_device_profile() {
    # cmdline-tools ships a finite list of device profiles; the Pixel 9 may not
    # be there yet on older SDK bundles. Pick the closest available match.
    local candidates=(pixel_9_pro pixel_9 pixel_8_pro pixel_8 pixel_7_pro pixel_7 pixel_6_pro pixel_6 pixel_5 pixel_4 pixel)
    local devices
    devices="$(avdmanager list device 2>/dev/null | awk -F'"' '/^    id:/ {print $2}')"
    for c in "${candidates[@]}"; do
        if grep -qx "$c" <<<"$devices"; then
            printf '%s' "$c"
            return 0
        fi
    done
    printf 'pixel'
}

create_avd() {
    log "Creating Pixel-class AVD"
    if avdmanager list avd 2>/dev/null | grep -q "Name: $AVD_NAME"; then
        ok "$AVD_NAME already exists"
        return 0
    fi
    local device
    device="$(pick_device_profile)"
    ok "Using device profile: $device"
    echo "no" | avdmanager create avd \
        --name "$AVD_NAME" \
        --package "$SYSIMG" \
        --device "$device" \
        --force
    # Tune for desktop GPU + plenty of RAM
    local cfg="$HOME/.android/avd/${AVD_NAME}.avd/config.ini"
    if [[ -f "$cfg" ]]; then
        sed -i \
            -e 's/^hw.gpu.enabled=.*/hw.gpu.enabled=yes/' \
            -e 's/^hw.gpu.mode=.*/hw.gpu.mode=host/' \
            -e 's/^hw.ramSize=.*/hw.ramSize=4096/' \
            "$cfg"
        grep -q '^hw.gpu.enabled' "$cfg" || echo 'hw.gpu.enabled=yes' >> "$cfg"
        grep -q '^hw.gpu.mode' "$cfg"    || echo 'hw.gpu.mode=host'    >> "$cfg"
    fi
    ok "Created $AVD_NAME (4GB RAM, host GPU)"
}

write_local_props() {
    log "Writing local.properties"
    cat > "$PROJECT_ROOT/local.properties" <<EOF
# Auto-generated by bootstrap.sh; safe to commit-ignore (see .gitignore).
sdk.dir=$SDK_ROOT
EOF
    ok "$PROJECT_ROOT/local.properties points at $SDK_ROOT"
}

ensure_gradle_wrapper() {
    log "Ensuring Gradle wrapper jar"
    if [[ -f "$PROJECT_ROOT/gradle/wrapper/gradle-wrapper.jar" ]]; then
        ok "gradle-wrapper.jar present"
        return 0
    fi
    require_sudo
    sudo apt-get install -y gradle
    cd "$PROJECT_ROOT"
    gradle wrapper --gradle-version "$GRADLE_VERSION" --distribution-type bin
    ok "Generated gradle/wrapper/gradle-wrapper.jar"
}

fetch_assets() {
    log "Fetching public-domain rule PDFs and pro se forms"
    bash "$PROJECT_ROOT/scripts/fetch_assets.sh"
    ok "Assets in place"
}

fetch_fonts() {
    log "Optional: refresh Cinzel + Inter TTFs from google/fonts (bundled copies used if offline)"
    if bash "$PROJECT_ROOT/scripts/fetch_fonts.sh"; then
        ok "Fonts refreshed from google/fonts"
    else
        log "Keeping existing res/font/*.ttf (network or mirror unavailable)"
    fi
}

build_app() {
    log "Building debug APK (./gradlew installDebug)"
    cd "$PROJECT_ROOT"
    ./gradlew --no-daemon installDebug
    ok "Forseti debug installed on the running device/emulator"
}

launch_emulator() {
    log "Launching Pixel 9 emulator"
    if pgrep -f "emulator.*$AVD_NAME" >/dev/null; then
        ok "Emulator already running"
        return 0
    fi
    nohup emulator -avd "$AVD_NAME" -no-snapshot-save -gpu host >/tmp/forseti_emu.log 2>&1 &
    ok "Emulator started (logs: /tmp/forseti_emu.log)"
    log "Waiting for device boot (this can take 90 sec on first launch)"
    adb wait-for-device
    until [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
        sleep 2
    done
    ok "Emulator ready"
}

start_app() {
    log "Launching Forseti"
    adb shell am start -n com.forseti.debug/com.forseti.MainActivity || \
    adb shell am start -n com.forseti/.MainActivity
    ok "App launched. Watch the emulator window."
}

main() {
    if [[ $LAUNCH_ONLY -eq 0 ]]; then
        ensure_jdk17
        ensure_studio
        ensure_kvm
        ensure_cmdline_tools
        install_sdk_packages
        create_avd
        write_local_props
        ensure_gradle_wrapper
        fetch_assets
        fetch_fonts
    else
        ensure_cmdline_tools  # restores PATH
        write_local_props
    fi

    if [[ $SKIP_LAUNCH -eq 1 ]]; then
        log "Setup complete. Skipping launch as requested."
        echo
        echo "Next:  bash bootstrap.sh --launch-only"
        return 0
    fi

    launch_emulator
    build_app
    start_app
    log "Done. The Forseti splash should now be on the emulator."
}

main "$@"
