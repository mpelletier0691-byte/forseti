# Drive Forseti on a Pixel 9 Emulator

If you were editing in a **cloud agent** session, pull that work onto this
machine first: `bash scripts/laptop_sync.sh --assets` (see
[CLOUD_WORKSPACE.md](CLOUD_WORKSPACE.md)).

You're on Ubuntu 24.04 with VT-x and 62GB of RAM, so the emulator will fly
once the toolchain is in place. Two paths below; pick one.

---

## Path A - one-shot script (fastest)

Open a terminal **on your real desktop** (not Cursor's sandbox - the sandbox
blocks downloads from Google's CDN and uscourts.gov):

```bash
cd ~/Desktop/Projects/Forseti
bash bootstrap.sh
```

The script:

1. Installs JDK 17 (via apt) and makes it the default `java`.
2. Installs Android Studio (via snap, classic confinement).
3. Verifies `/dev/kvm` is exposed; warns and exits if not.
4. Downloads Android command-line tools into `~/Android/Sdk/cmdline-tools/latest`.
5. Accepts SDK licenses and installs:
   - `platforms;android-35`
   - `build-tools;35.0.0`
   - `platform-tools` (gives you `adb`)
   - `emulator`
   - `system-images;android-35;google_apis;x86_64` (the Pixel 9 image)
6. Creates an AVD called `Pixel_9_API_35` tuned for host-GPU + 4GB guest RAM.
7. Writes `local.properties` pointing at the SDK.
8. Generates `gradle/wrapper/gradle-wrapper.jar` (one-shot via system gradle).
9. Runs `scripts/fetch_assets.sh` to bundle the FRCP PDF and Pro Se forms.
10. Boots the emulator, installs the debug APK, launches Forseti.

First run: ~25 minutes (~3GB of downloads). Subsequent runs: ~30 seconds.

If you already have most of this installed and just want to rebuild + relaunch:

```bash
bash bootstrap.sh --launch-only
```

If you want every prerequisite but **no** emulator launch (e.g. you're going
to use Studio's UI instead):

```bash
bash bootstrap.sh --skip-launch
```

---

## Path B - Android Studio UI (if you prefer clicks)

1. Install Android Studio:
   ```bash
   sudo snap install android-studio --classic
   android-studio
   ```
2. Studio's first-run wizard installs the SDK and asks you to accept the
   Android, Intel HAXM (skipped on AMD/Linux KVM), and Google Play licenses.
   Accept them all.
3. **More Actions - SDK Manager - SDK Platforms** -> check **Android 15.0
   ("VanillaIceCream") API 35**, click Apply.
4. **SDK Manager - SDK Tools** -> check **Android Emulator**, **Android SDK
   Build-Tools 35.0.0**, **Android SDK Platform-Tools**. Apply.
5. **More Actions - Virtual Device Manager - + Create Virtual Device** ->
   choose **Pixel 9** -> next -> select the API 35 image (download if
   needed) -> Finish.
6. **File - Open** -> `~/Desktop/Projects/Forseti`.
7. Studio prompts to sync Gradle. Accept. (Pulls AGP 8.7, Compose, Hilt,
   Pdfium, ML Kit; ~5-15 min.)
8. From a terminal, populate the rule PDFs:
   ```bash
   cd ~/Desktop/Projects/Forseti && bash scripts/fetch_assets.sh
   ```
9. In Studio, pick **Pixel 9 API 35** in the device dropdown. Click **Run**
   (`Shift+F10`).

---

## What you should see

1. Splash: black background, raven-on-scales mark, held ~5 seconds.
2. First-launch disclaimer overlay - tap "I understand".
3. Sidebar (medium dark grey) on the left with 8 entries (Quick Jump,
   Drafts, Guides, States, Deadlines, Glossary, Notes, Settings).
4. Main pane shows page 1 of FRCP 2024.
5. Tap the book icon in the top bar -> Quick Jump panel slides down.
6. Pinch to zoom in the PDF.

Walk through every screen using
[QA_CHECKLIST.md](QA_CHECKLIST.md).

---

## Common snags

| Symptom | Fix |
| --- | --- |
| `/dev/kvm: Permission denied` | `sudo adduser $USER kvm`; log out and back in. |
| `/dev/kvm` does not exist on your real host | Enable VT-x in BIOS, then `sudo apt install -y qemu-kvm libvirt-daemon-system`. |
| Emulator window black or laggy | Make sure host GPU mode is set: `~/.android/avd/Pixel_9_API_35.avd/config.ini` should contain `hw.gpu.mode=host`. NVIDIA proprietary driver (you have it) helps a lot. |
| Gradle sync fails: "Could not find pdfium-android" | We pull it via JitPack. Check `settings.gradle.kts` has `maven("https://jitpack.io")` (it does). |
| Gradle sync fails: "JAVA_HOME points at JDK 8" | `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` and re-sync. The bootstrap script handles this. |
| App opens to "FRCP PDF not bundled" | You haven't run `scripts/fetch_assets.sh` yet, or your network blocked uscourts.gov. Quick Jump still works against the bundled outline. |
| OCR camera preview is blank | In the emulator's three-dot menu -> Camera -> set back camera to "VirtualScene" or "Webcam0". |
| App crashes on launch with "Hilt not initialized" | Almost always a stale build; `./gradlew clean installDebug`. |

---

## Iterating quickly without restarting the app

In Studio, **Settings -> Editor -> Live Edit -> Push edits manually (or
Automatic)**. Compose changes hot-swap into the running app within ~1 second.
This makes UI tweaks (theme colors, padding, copy) feel instant.

For deeper changes, **Build -> Apply Changes and Restart Activity** is
faster than a full reinstall.
