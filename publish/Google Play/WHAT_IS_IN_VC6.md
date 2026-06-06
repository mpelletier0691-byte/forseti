# Forseti 0.1.3 — versionCode 6 (what you upload)

**Upload this file only for production:**

```text
/home/user/Desktop/Publish_Projects/Forseti/Google Play/Forseti-0.1.3-vc6.aab
```


| Field                   | Value                                                              |
| ----------------------- | ------------------------------------------------------------------ |
| Package                 | `com.forseti`                                                      |
| versionName             | **0.1.3**                                                          |
| versionCode             | **6**                                                              |
| Size                    | 35,257,074 bytes (~33.6 MB)                                        |
| SHA256                  | `8109bdd8df4a12f660c3786a3c7b259ac8e29340d54e284735777ced36aabe4c` |
| Git commit (app source) | `4fbd715` + legal `8a6c1af` + features `7fa39f0`                   |
| Staged                  | See `Publish_Projects/Forseti/VERSION.txt`                         |


**Also upload to Play (same release, version 6):**

```text
/home/user/Desktop/Publish_Projects/Forseti/Google Play/mapping/mapping-vc6.txt
```

(App bundle explorer → version **6** → Deobfuscation / mapping file)

---

## Inside this AAB (user-visible)

- First launch: 2 pre-language notices → permanent language (EN / ES / zh-CN) → legal disclaimer → tutorial
- Translation + liability language in disclaimer and settings
- References: Federal Judicial Center (FJC) links
- Guides / glossary localized (ES, zh); **court PDFs and federal forms stay English**
- Dashboard + sidebar title layout fixes
- 3-day free trial + $4.99 Play unlock (`forseti_unlock`)
- Target SDK 35, min SDK 26

**Not inside the APK:** GitHub privacy/Terms pages (URLs only, in Play Console + Wix).

**Debug-only (NOT in this AAB):** `com.forseti.debug`, 30-day dev trial — separate install via `./scripts/install_debug.sh`.

---

## Do NOT upload


| File                                                 | Why               |
| ---------------------------------------------------- | ----------------- |
| `Forseti-0.1.2-vc4.aab`                              | Old               |
| `Forseti-0.1.3-vc5.aab`                              | Superseded by vc6 |
| `app-debug.apk`                                      | Debug package     |
| Anything from `archive/` unless you know you need it |                   |


