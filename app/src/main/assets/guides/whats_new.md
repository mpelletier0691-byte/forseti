# What's New in This Version

Forseti helps **pro se** litigants keep federal civil case documents organized on your phone. This release (since Play production **0.1.4**) adds **Brokkr Forge case ingestion**, **background sorting with notifications**, **smarter on-device classification**, and **Android 15 (API 35) compatibility** — while keeping the same privacy model: everything stays on your device until you share a file.

---

## Since production 0.1.4 — at a glance

| Area | What changed |
|------|----------------|
| **Case ingestion** | Brokkr Forge Process + Image Ingestion from Case Profile |
| **Background work** | Folder ingest runs while you use other apps; completion notification |
| **Sorting** | Path/filename keywords, multi-page PDF OCR, late-binding classification, DOCX text |
| **Confidence tiers** | 85%+ auto-file · 70–84% Inbox + File Here · below 70% manual |
| **Inbox tools** | Duplicate skip badges · Delete all in 99_Inbox |
| **Audit** | `INGEST_AUDIT_latest.txt` with confidence, routing, alternates |
| **UI** | Scrollable edit-case dialog · edge-to-edge on Android 13–15 |
| **Play / Google** | Target SDK 35 · notification permission on Android 13+ · foreground data-sync for long ingest · no advertising ID |

Everything below still applies to **plaintiff or defendant** — sorting is by document type, not party.

---

## Background Brokkr Forge (leave the app)

When you tap **Brokkr Forge Process**, Forseti sorts your folder **in the background**:

1. Pick your source folder with Android’s system folder picker (SAF).
2. Forseti saves the case (if needed) and shows: *Brokkr Forge started — you’ll get a notification when sorting finishes.*
3. A low-priority ongoing notification appears: **Brokkr Forge sorting…**
4. When done: **Brokkr Forge complete, head back to forseti to finalize your case.**
5. Open **Case Profile → your case** → review **99_Inbox** and **File Here** chips.

Multi-file **Image Ingestion** (2+ files) also runs in the background. A single file still finishes inline.

> Use **Brokkr Forge Process** via SAF — not `adb push`. On emulators, wipe data if storage is full before large tests.

---

## Confidence routing (three tiers)

| Confidence | What happens | What you see |
|------------|--------------|--------------|
| **85%–100%** | Auto-filed to the predicted Brokkr-Forge folder | Gold **Auto-filed · 0.xx** chip |
| **70%–84%** | Stays in **99_Inbox** | Amber **File Here →** chip (one tap) |
| **Below 70%** | Stays in **99_Inbox** | **classify manually** — move or rename yourself |

Forseti uses **folder path + filename** from your source tree, **multi-page PDF OCR sampling**, and **late-binding** re-classification — not filename alone.

**Example** (~317 organized files): Pleadings 17, Motions 5, Correspondence 5, Evidence 1, Exhibits 1, Hearings 1, Deadlines 1, Case Overview 1, Inbox 6.

---

## Inbox & duplicates

- **Duplicate detection** — same normalized name + size in one batch or already in the workspace is skipped (badge in Case Profile).
- **Delete all** — clear every file in **99_Inbox** when you need a fresh triage pass.
- **DOCX** — Word documents are text-extracted and classified like PDFs.

---

## Timing for large folders

On-device OCR is accurate but slow.

| Approx. files | Expect |
|---------------|--------|
| Under 50 | A few minutes |
| ~300 well-named | **15–30 minutes** typical |
| 1,300+ full corpus | **Hours** — batch by subfolder |

Rename before ingest when you can (`2024-03-12_motion_to_dismiss.pdf`).

---

## Audit log (reversible)

Every bulk ingest writes **`INGEST_AUDIT_latest.txt`** at the case workspace root: original name, saved name, folder, confidence, routing tier, OCR trace fields, top two alternate guesses.

Move, rename, or delete any file from Case Profile — auto-file is not permanent.

---

## Android 15 & Play compatibility (unchanged promises)

- **Min SDK 26** · **Target SDK 35** (Google Play requirement)
- **Edge-to-edge on Android 13–15** — `enableEdgeToEdge()` on every activity; scaffolds and overlays respect status bar, navigation bar, and display cutout insets
- **POST_NOTIFICATIONS** requested when you use deadlines or Brokkr Forge
- **Foreground data-sync** only while sorting runs (WorkManager)
- **No advertising ID** — AD_ID stripped from merged manifest
- **Local-only case data** — no cloud upload of your files
- **Trial + unlock** — same 3-day trial and Play billing as production

---

## Where to find help

**Settings → Help → What's New in This Version** (offline).

**Guides → Ingesting an Existing Case Into Forseti** — step-by-step workflow.
