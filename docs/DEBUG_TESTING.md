# Forseti — debug APK (dev team clean slate)

The **debug** build is a **separate app** from Play **`com.forseti`**. Use it when you need a fresh trial, first-run flow, or day-to-day QA without touching closed-testers’ data.

| | Play / release | Debug |
|--|----------------|--------|
| Package | `com.forseti` | `com.forseti.debug` |
| Name on device | Forseti | Forseti (debug) |
| Trial length | 3 days | **30 days** |
| Install | Play closed testing | `adb` / `install_debug.sh` |
| Shares data with Play build? | **No** | **No** |

Uninstall protection for **production** (backup + device fingerprint) is unchanged. Debug prefs live under the `.debug` package only.

---

## Install on emulator or USB device

```bash
cd ~/Desktop/Projects/Forseti
./scripts/launch_emulator_desktop.sh   # if needed
adb devices                            # must show "device"
./scripts/install_debug.sh
```

Launches `com.forseti.debug`.

---

## Full reset (new trial + first-run)

Debug is already isolated from Play. To wipe **only** the debug app:

```bash
adb shell pm clear com.forseti.debug
./scripts/install_debug.sh
```

You get: pre-language gates → language → disclaimer → tutorial → **new 30-day trial**.

**Do not** clear `com.forseti` unless you intend to reset a Play tester install on that device.

---

## Side-by-side on one phone

You can have **both** installed:

- **Forseti** — Play closed test (`com.forseti`)
- **Forseti (debug)** — dev build (`com.forseti.debug`)

Different icons/labels may look similar; check package in Settings → Apps.

---

## Build debug APK without installing

```bash
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

Share with a dev who has USB debugging:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## When to use debug vs Play build

| Use debug when… | Use Play build when… |
|-----------------|----------------------|
| Testing onboarding / language lock | Matching what testers get |
| Need a clean trial without new release | Verifying 0.1.3 AAB / billing on Play |
| Gradle / UI work on emulator | Closed-test opt-in link flow |

---

## Optional: desktop shortcut

```bash
./scripts/trust_forseti_desktop_shortcuts.sh
```

Uses **Forseti — Install on Emulator** (debug install script).
