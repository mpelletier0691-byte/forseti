# Forseti 0.1.4 — versionCode 7 (what you upload)

**Upload this file only for production:**

```text
~/Desktop/Publish_Projects/Forseti/Google Play/Forseti-0.1.4-vc7.aab
```

| Field | Value |
|-------|--------|
| Package | `com.forseti` |
| versionName | **0.1.4** |
| versionCode | **7** |
| Size | 35,257,070 bytes (~33.6 MB) |
| SHA256 | `a43917d206e77436b5858224475c01975251739553e8b0773e6850cee58002b3` |
| Staged | 2026-06-06 (see `Publish_Projects/Forseti/VERSION.txt` after re-staging) |

**Also upload to Play (same release, version 7):**

```text
~/Desktop/Publish_Projects/Forseti/Google Play/mapping/mapping-vc7.txt
```

(App bundle explorer → version **7** → Deobfuscation / mapping file)

---

## Inside this AAB (user-visible)

Same feature set as 0.1.3 (vc6) — this is a **new version code** after Play consumed vc6:

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

| File | Why |
|------|-----|
| `Forseti-0.1.3-vc6.aab` | Version code 6 already registered on Play |
| `Forseti-0.1.3-vc5.aab` | Superseded |
| `Forseti-0.1.2-vc4.aab` | Old |
| `app-debug.apk` | Debug package |
| Anything from `archive/` unless you know you need it |
