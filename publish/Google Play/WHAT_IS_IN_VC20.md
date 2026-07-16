# Forseti 0.1.17 — versionCode 20 (production candidate)

Upload after `./scripts/stage_play_publish.sh` updates paths.

| Field | Value |
|-------|--------|
| Package | `com.forseti` |
| versionName | **0.1.17** |
| versionCode | **20** |
| Replaces Play | **0.1.4 (vc7)** |

## Same production baseline as 0.1.4

- Min SDK **26**, Target SDK **35**, Compile SDK **35**
- No advertising ID (`AD_ID` stripped from merged manifest)
- 3-day trial + Play unlock (`forseti_unlock`)
- First launch: pre-language gates → permanent language → disclaimer → tutorial
- Court PDFs and federal forms stay **English**; UI guides localized (EN / ES / zh-CN)
- Privacy: local case storage; no cloud upload of case files

## New since 0.1.4 (this release)
- **Family court guide** — *Staying Organized in Family Court* in Guides (English body; localized titles in ES/zh-CN index)


- **Brokkr Forge** — background folder ingest with completion notification and progress
- **Smarter sorting** — path/filename hints, multi-page PDF OCR, late-binding, DOCX
- **Confidence tiers** — 85%+ auto-file, 70–84% Inbox + File Here, below 70% manual
- **Inbox tools** — duplicate skip, Delete all in 99_Inbox, ingest audit log
- **Edge-to-edge (SDK 35)** — `enableEdgeToEdge()` on all activities; inset-safe scaffolds and overlays
- **Foreground data-sync** — WorkManager only while Brokkr Forge sorts (Play-declarable FGS type)

## Play Console declarations

| Topic | Answer |
|-------|--------|
| Target API | 35 |
| Advertising ID | **No** |
| Foreground service | **Data sync** — case document sorting only |
| POST_NOTIFICATIONS | Deadlines + Brokkr Forge completion |
| Camera | Optional (scanner) |

## Do NOT upload

- `com.forseti.debug` APK/AAB
- Any bundle with versionCode ≤ 7 (already consumed on Play)
