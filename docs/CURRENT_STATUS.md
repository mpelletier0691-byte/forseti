# Forseti — current status handoff (cloud agents)

**Last updated:** 2026-07-16  
**Repo:** https://github.com/mpelletier0691-byte/forseti  
**Package:** `com.forseti` (release) / `com.forseti.debug` (debug)  
**Prepared version:** **0.1.17 / versionCode 20**

This file is the **session handoff** so cloud agents can continue without the desktop chat history.

---

## Do NOT request or commit secrets

Cloud agents must **never** ask for, store, or commit:

- Google / Play Console passwords or 2FA codes  
- `keystore.properties`, `*.jks`, upload key passwords  
- Billing / merchant / bank credentials  
- API keys not already in the public repo  

**“Allow any access to accounts” is refused.** Agents work only via the GitHub repo the user connects to Cursor. Signing and Play Console stay on the owner’s PC (or GitHub Actions secrets configured by the owner — not pasted into chat).

---

## Product decisions (locked in)

### Guides — Model B (purchase gate)

Full detail: **`docs/GUIDES_PREMIUM_GATING.md`**

- **Free (trial + purchased):** Forseti how-to / feature guides only  
  (`whats_new`, `forseti_shortcuts`, `using_forseti_as_pro_se`, `drafts_federal_vs_local`, `case_ingestion`, `ocr_capture_workflow`, `digital_case_folder`)
- **Purchase-only:** All other existing procedure guides + **every new guide** by default  
- Trial does **not** unlock premium guides  
- After trial expiry: whole app still locked (`TrialGate`) until purchase  

**Implementation status:** Documented; **code gating may still be pending** — check `GuideMeta.premium` and `GuidesScreen` lock UI.

### Family court guide

- File: `app/src/main/assets/guides/family_court_organization.md`  
- Indexed in EN/ES/zh `00_index.json`  
- **Premium** (purchase-only) under Model B  

### Brokkr Forge / background ingest

- WorkManager + FGS `dataSync` (`CaseIngestWorker`)  
- Completion notification only when fully processed  
- Play FGS demo page: `https://mpelletier0691-byte.github.io/forseti/fgs-demo.html`  
- Local video: `docs/media/brokkr-forge-demo.mp4` / `landing/media/`  

### Edge-to-edge (Android 15 / targetSdk 35)

- `setupForsetiEdgeToEdge()` in activities  
- Scaffold / top bar insets on major screens  
- Addresses Play “edge-to-edge may not display” recommendation once vc20 is live  

---

## Play release status (owner PC)

| Item | Status |
|------|--------|
| AAB staged | `~/Desktop/Publish_Projects/Forseti/Google Play/Forseti-0.1.17-vc20.aab` (**not in git**) |
| Upload cert SHA256 | `CA:16:3E:BE:90:B6:20:5A:BA:36:4D:98:1C:7A:DA:F9:73:2D:01:C3:EB:80:F6:F8:9C:84:9F:71:1F:F5:37:79` |
| FGS declaration | Data sync → **Importing, exporting** + demo video link |
| Production | Owner was on **Create production release** / Save → Send for review for **20 (0.1.17)** |
| Live production before this | **0.1.4 / vc7** |

Cloud agents: update `publish/Google Play/*` text only. Do **not** expect the AAB in the clone.

---

## July 2026 Play policy mail

Forseti **not impacted** by anonymous-chat / SMS-call-log / EWA changes. Confirm in Console: app registered, content rating complete, Data safety accurate. `targetSdk 35` already meets Aug 2026 API reminder.

---

## Next work for cloud agents (priority)

1. **Implement guide premium gating** per `docs/GUIDES_PREMIUM_GATING.md` (schema + indexes + GuidesScreen Buy/Restore + trial copy).  
2. Optionally write **new premium guides** from the suggested list in that doc (owner picks titles).  
3. Keep edge-to-edge / Brokkr Forge / FGS docs accurate.  
4. Open a **PR**; owner merges, then builds AAB on PC.

---

## Key paths

| Topic | Path |
|-------|------|
| Cloud how-to | `docs/CLOUD_AGENT_WORK.md` |
| Guide lock policy | `docs/GUIDES_PREMIUM_GATING.md` |
| Project brief | `docs/FORSETI_PROJECT_BRIEF.md` |
| Implementation notes | `docs/Forseti_Implementation_Notes.md` |
| Play checklist | `publish/Google Play/PLAY_UPLOAD_CHECKLIST.md` |
| VC20 changelog | `publish/Google Play/WHAT_IS_IN_VC20.md` |
| FGS demo page | `docs/fgs-demo.html` |
| Version | `app/build.gradle.kts` → 20 / 0.1.17 |

---

## Constraints (do not break)

- Legal routing / privileged content rules  
- Brokkr Forge folder layout semantics  
- No ads / no AD_ID  
- On-device case data; no server that receives case files  
- Not legal advice disclaimers  

---

## Starter prompt (paste into cloud agent)

```text
You are working on Forseti (mpelletier0691-byte/forseti), branch main.

1. Read docs/CURRENT_STATUS.md and docs/GUIDES_PREMIUM_GATING.md and docs/CLOUD_AGENT_WORK.md.
2. Never ask for passwords, keystores, or Play Console login.
3. Implement purchase-gated guides (Model B) if not already in code: premium flag, lock UI, Buy/Restore; free list = Forseti how-to guides only.
4. Open a PR with a clear summary when done.
```
