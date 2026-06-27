# AGENTS.md

## Cursor Cloud specific instructions

Forseti is a **native Android app** (Kotlin + Jetpack Compose, Hilt, Room, single Gradle module `:app`). The repo also contains a static marketing/policy **landing site** (`landing/`, mirrored to `docs/` for GitHub Pages). There is no backend service.

### Toolchain (already installed in the VM snapshot)
- **JDK 17** at `/usr/lib/jvm/java-17-openjdk-amd64`. The build requires JDK 17 — the image also has JDK 21, so make sure `JAVA_HOME` points at 17. `~/.bashrc` already exports `JAVA_HOME`, `ANDROID_HOME`/`ANDROID_SDK_ROOT` (`~/Android/Sdk`), and the SDK on `PATH`, so a normal login shell is ready to build.
- **Android SDK**: `platform-tools`, `build-tools;35.0.0`, `platforms;android-35` (compileSdk/targetSdk = 35, minSdk 26).
- `local.properties` is gitignored; the startup update script regenerates it pointing at `~/Android/Sdk`.

### Build / lint / test (run from repo root)
- Build debug APK: `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` (package `com.forseti.debug`, launcher `com.forseti.MainActivity`).
- Unit tests (JVM, fast): `./gradlew :app:testDebugUnitTest`. Core domain logic such as `com.forseti.deadlines.Rule6` is pure Kotlin (kotlinx-datetime) and runs here without a device.
- Lint: `./gradlew :app:lintDebug`. NOTE: lint currently **fails** with pre-existing `MissingTranslation` errors (untranslated `pre_install_*` strings) because `abortOnError` is on. This is a project content issue, not an environment problem — the lint engine itself runs fine and writes a report under `app/build/intermediates/lint_intermediate_text_report/`.
- Use `--no-daemon` for one-shot CI-style runs; the configuration cache is intentionally disabled (see `gradle.properties`).

### Running the app — emulator limitation (important)
The Android **emulator cannot run in the Cursor Cloud VM**: there is no `/dev/kvm` and the CPU exposes no `vmx`/`svm` flags, and the x86_64 system image strictly requires KVM. Do not spend time installing/booting an AVD here — verify app behavior via `assembleDebug` + JVM unit tests, and push device/emulator testing (`./scripts/install_debug.sh`, `bootstrap.sh`) to a machine with KVM. See `docs/RUN_LOCALLY.md` and `docs/DEBUG_TESTING.md`.

### Landing site (web surface that DOES run here)
Serve for manual/browser testing: `python3 -m http.server 8000 --directory landing` then open `http://localhost:8000/index.html`. `docs/` is a synced copy (`scripts/sync_landing_site.sh`).

### Assets
FRCP PDF and pro-se forms are committed under `app/src/main/assets/`. `scripts/fetch_assets.sh` (idempotent, network-resilient) only re-downloads if missing; the app degrades gracefully when assets are absent.
