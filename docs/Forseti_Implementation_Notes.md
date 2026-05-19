# Forseti — Implementation Notes (Cursor-ready)

Native Android app: **Kotlin**, **Jetpack Compose**, **Room**, assets under `app/src/main/assets/`.  
This document merges product intent with engineering reality. **Do not use Flutter/`pubspec.yaml` terminology** — guides are Markdown in `assets/guides/`, not PDFs in the Guides tab.

---

## Naming

- **Do not** ship the separate product name “Brokkr Forge” inside Forseti UI or strings (reserved for another app).  
- Use **“case workspace”**, **“organized folders”**, or **“Forseti folder layout”** for the same idea (numbered phases, drafts/filed/exhibits subfolders).

---

## Status overview

| Area | Status | Notes |
|------|--------|--------|
| Guides tab stability | **Crash-fixed** | Replaced `multiplatform-markdown-renderer-m3` with in-house `SafeMarkdown`. Handles headings, lists, blockquotes, fenced code, inline emphasis, and converts markdown tables (which crashed the old renderer on `discovery_basics.md`) into bullet rows. |
| Motion Drafts / PDF generation | **Improved** | `DraftsViewModel.materialize` now returns `DraftMaterialization.Success/Error`; missing bundled forms or generator failures show a snackbar instead of looking like a silent crash. Also logged to `Log.e("DraftsViewModel", ...)`. |
| Deadlines + case workspace folders | **Implemented** | `CaseFolderService` creates folders under app-private storage when a deadline is saved or a case is touched; the Deadlines card now exposes a "Folder" action that surfaces the path. |
| Splash | **Fixed** | `MainActivity` now drops the system splash on the first composed frame so the Compose `SplashOverlay` is actually visible for the full hold (was previously stacked under the system splash window). Logo enlarged (~65% of shorter dim, capped at 440 dp) and the column is `verticalScroll`-able with version, tagline, and the disclaimer line. |
| Quick Jump PDF reader | **Stable** | Per-page pinch zoom + single-finger vertical scroll in `PdfViewer.kt`. |
| State Rules | **Curated** | `StateRules.kt` rewritten to use stable `.gov` index pages for every state plus federal rules. Each entry carries `lastVerified` and `isIndexPage`. New "Official .gov sources" banner explains the strategy and surfaces a "Check for updates" button that re-downloads everything in the local cache via `StateCacheManager.refreshAllCached`. |
| Disclaimer | **Implemented** | `disclaimer_title` / `disclaimer_body` rewritten; splash carries the bold "Information only. Not legal advice." line; Settings has a dedicated disclaimer section. |
| Document Scanner **tab** | **Scrollable** | `ScannerScreen` body below the case picker now lives inside a `verticalScroll` Column with a fixed-height (320 dp) camera pane so the page strip + Save button are always reachable. |
| Backup **tab** | **Implemented (baseline)** | `BackupScreen` + `BackupService` produce atomic ZIPs of the case workspace under app cache and surface them for share/delete. |
| Case Profile **tab** | **In-app browser** | `CasesScreen` opens a `CaseDetailScreen` on tap with an expandable phase tree, per-file rename / share / delete, and SAF import into any subfolder. The "Edit" pencil keeps the metadata dialog. |
| Full-case export (ZIP / binder) | **Partially via Backup** | The Backup tab ZIPs the entire case workspace; trial-binder export is still future work. |
| Quick Jump notes pop-out | **New** | A sticky-note icon next to Quick Jump's menu-book opens an overlay with a `MeadAmber` notepad surface. **Save** persists into the existing Notes tab anchored to `frcp.p<page>.<ts>`; **Done** discards. The pop-up always opens blank — it's a quick capture surface, not an editor for old notes. |
| Splash + Disclaimer motto | **New** | `splash_motto` string carries the "AI might be powerful, but nothing is more powerful than the human spirit and drive — let this help guide you in your saga." line. Shown on the splash overlay below subtitle, in the disclaimer dialog body, and in the Settings disclaimer card. |
| Scanner auto-routing | **New** | `ScannerService.savePdf` now defers to `CaseFolderService.classifyForCase`, which inspects the label/filename for keywords (motion, discovery, answer, order, exhibit, etc.) and routes the PDF into `<phase>/Drafts/`. Falls back to `98_Scans` if nothing matches. The scanner UI explains the keyword convention and the snackbar reports the destination folder. |
| Uploaded Rules tab | **New** | `Destination.Imports` → `UploadedRulesScreen` + `UploadedRulesService`. Lets the user import their own rule PDFs (state sites that 404, paywalled HTML, annotated copies). Files live in `<files>/uploaded_rules/` with a `manifest.json` sidecar. Auto-filing logic deliberately skips this folder. |
| Case Studies tab | **New** | `Destination.CaseStudies` → `CaseStudiesScreen`. Search box that builds Google Scholar Case Law URLs + curated quick links (Scholar, Supreme Court, Circuits, PACER, CourtListener, Justia, LII Wex). Opens externally in the user's default browser to satisfy Play Store policy. |
| References tab | **New** | `Destination.References` → `ReferencesScreen`. Full bibliography of every external URL the app cites + license text + Play Store-ready disclaimer about link drift. Pulls state and circuit URLs directly from `StateRulesCatalog` so it stays in sync. |
| Share intent filter | **New** | `ShareReceiverActivity` registered for `ACTION_SEND` / `ACTION_VIEW` on `application/pdf`. Other apps can now share PDFs into Forseti; a chooser routes them to **Uploaded Rules** or any case workspace (with auto-routing by label keywords). |
| State-rule URL refresh | **Hardened** | `StateRule` gained an optional `fallbackUrl` (state judiciary homepage). MA, AL, AR, AZ, CO, IL, KY, FL, MI, NC, NM, NV, SC, UT URLs were swept to better-verified `.gov` paths. The States screen now shows a courthouse icon next to each row that jumps to the fallback if the deep link 404s. |
| **3-day trial + Play Billing** | **New** | `BillingService` (Billing v7 ktx) drives the `forseti_unlock` non-consumable. `TrialPrefs` stamps the start time into `forseti_prefs.xml` (which is in `backup_rules.xml` / `data_extraction_rules.xml` so reinstalls on the same Google account restore it via Auto Backup) and adds a hashed-Android-ID fingerprint as anti-tamper. `EntitlementManager` combines purchase + trial state into `Trial / TrialEndingSoon / Expired / Purchased / Loading`. `MainActivity.TrialGate` shows a welcome dialog on first launch, a soft <24 h reminder (once), a confirmation modal after purchase, and a full-screen `TrialExpiredScreen` lock when the trial ran out. Settings now leads with a `TrialBanner` carrying Buy ($4.99) / Restore actions. `BILLING` permission added in `AndroidManifest.xml`. |
| **Brokkr-Forge folder schema** | **New** | `CaseFolderService` rewritten to the canonical 11-folder layout (00_Case_Overview … 10_Trial) plus `98_Scans/` and `99_Inbox/`. New `routeIngestedFile()` overload routes by keyword first (motion, complaint, summons, exhibit, witness, etc.) then by mime/extension family (image → 04_Evidence/Photos, audio → 04_Evidence/Audio, etc.). Unknown files land in `99_Inbox/`. Existing call sites (scanner, share-into-app, deadline auto-folder) keep working through the refreshed `classifyForCase()` keyword table. |
| **Case ingestion buttons** | **New** | `CaseIngestService` handles SAF tree-pick (`OpenDocumentTree`) and multi-file pick (`OpenMultipleDocuments`). The Case Profile edit dialog grew an **Ingest folder** + **Ingest images** row under "Complaint filed" with an inline rename tip. Reports surface as a snackbar (`Imported N files · X to 99_Inbox`). |
| **Asvaettir Labs branding** | **New** | About card in Settings now carries the "Forseti is built for fighters" message + "Asvaettir Labs — Tools for the Determined." sign-off (also rendered on the splash). Strings `about_brand_title` / `about_brand_body` / `about_brand_signoff`. |
| **Local PDF reader for uploads** | **New** | New `LocalPdfReader` composable wraps the existing `PdfViewer` for arbitrary `File` sources. The Uploaded Rules tab now opens imported PDFs in-app (tap card or the new book-icon button) — same pinch-zoom + page tracking as Quick Jump. |
| **Case Studies "Save case" tip** | **New** | Card on the Case Studies screen explains the browser → Save as PDF → Share-into-Forseti pipeline so users can keep precedent as referenceable evidence in their case workspaces. |
| **New guide: case ingestion** | **New** | `assets/guides/case_ingestion.md` walks novices through the Brokkr-Forge schema, both ingestion buttons, the rename-first habit (with bad/good filename table), and a recommended workflow. Indexed in `00_index.json`. |
| **In-app file viewer (case workspace)** | **New** | Tapping any file in `CaseDetailScreen` opens it inside Forseti via the new `casefiles/CaseFileViewer.kt` dispatcher: PDFs route to `LocalPdfReader`, images get a pinch-zoomable preview, text/markdown lands in a scrollable `SelectionContainer` (with read-aloud), and unknown formats get an *Open in another app* fallback that fires `ACTION_VIEW` through the existing FileProvider. |
| **Move-after-rename prompt** | **New** | After every successful rename `CasesDetailViewModel.rename` invokes a callback that opens `MoveAfterRenameDialog`, listing every Brokkr-Forge phase + subfolder. One tap moves the file via the new `CaseFolderService.moveFile` API; *Keep here* dismisses. |
| **Clickable deadlines** | **New** | `DeadlineRow` is now a `clickable` card opening `DeadlineDetailDialog` — countdown ("3 days remaining" / "Overdue by 2 days" / "Completed"), authority + hint pulled from `TimingRules` when the citation matches, reminder schedule, and shortcut buttons for *Mark complete*, *Folder*, and *Delete*. |
| **Page bookmark icon** | **New** | Quick Jump's top bar grew a `Bookmark` toggle next to the existing menu-book / sticky-note icons. It writes/removes a `BookmarkEntity` keyed `frcp.p<page>` via `NotesViewModel.toggleBookmark` so saved pages show up in Notes → Bookmarks tab. |
| **Read-aloud (system TTS)** | **New** | New `tts/ForsetiTts.kt` (Hilt singleton) wraps Android's `TextToSpeech`, queues until init succeeds, chunks long input, and exposes a `Ready/Speaking/Unavailable` state flow. `tts/PageOcr.kt` rasterizes a `PdfRenderer.Page` and runs ML Kit `text-recognition.latin` on it so PDFs (which have no exposed text layer in the framework renderer) can be read aloud. `ReadAloudControls` composable plugs into Quick Jump, the `LocalPdfReader` (uploaded rule PDFs and case-workspace PDFs), the Guides screen, and case-workspace text/markdown viewers. If the system reports no engine the dialog deep-links to `TTS_SETTINGS` (or accessibility settings as fallback) and offers a Play Store link to install Google's TTS engine. |
| **Copy/paste in long-form text** | **New** | `GuidesScreen` body, case-workspace text viewer, and individual notes in `NotesScreen` are wrapped in `SelectionContainer` so long-press surfaces the standard Android selection handles + Copy. Markdown body fed to TTS goes through a `stripMarkdown` regex pass so `**bold**` and tables don't get spoken literally. |
| **TTS lifecycle** | **New** | `MainActivity.onPause` stops any active utterance; `onDestroy` shuts down the engine when the activity is finishing. `LocalTts` CompositionLocal mirrors `LocalBilling` / `LocalEntitlement` so any deep child can call read-aloud without dragging the singleton through every screen signature. |
| **New guide: pro-se field manual** | **New** | `assets/guides/using_forseti_as_pro_se.md` (indexed second, right after the shortcuts cheat sheet) covers: the rule-memorization problem and how Forseti's Quick Jump / State Rules / Uploaded Rules / Bookmarks / Notes / Case Studies tabs combine into a "swiss army knife" for pro-se litigants; pro-se advantages courts recognize (liberal pleading construction, sincerity, factual mastery) vs. disadvantages (procedural traps, evidence rules, emotional cost, no malpractice backstop); the strong recommendation to consult an attorney — including limited-scope / unbundled / "lawyer for one matter" representation; free / low-cost options (legal aid, clinics, bar referrals, court self-help, lawyer-of-the-day); when to absolutely hire counsel; a "read the rules → bookmark → note → draft → sleep on it → file" workflow; and a personal-responsibility section emphasizing the user is responsible for their own actions, not for the conduct of others. Closes with "Good luck." and the Asvaettir Labs sign-off. |
| **PdfViewer: preserve page on rotation** | **Fix** | Rotating the device used to slam the FRCP / uploaded-rule reader back to page 1 because `LaunchedEffect(jumpTarget)` re-collected the StateFlow's initial replay value (always 0) and called `scrollToItem(0)`, overriding the LazyListState's saved restore. Fixed by `jumpTarget.drop(1).collect { … }` so only *post-init* TOC taps trigger a scroll. Also added an `initialPage` parameter sourced from `currentPage` (Quick Jump) / `rememberSaveable` (LocalPdfReader) so the first composition lands on the page the user was on, even on cold start after process death. |
| **OCR resolution bumped** | **Fix** | `PageOcr.extractText` default `targetWidthPx` raised from 1600 → 2400 (≈300 DPI on an 8 in page). At the lower setting the FRCP body text was too dense for ML Kit's latin recognizer to pick up reliably, leaving Read-aloud silent. The new resolution gives the recognizer clean glyph edges. |
| **Read-aloud audible feedback** | **Fix** | `ReadAloudControls` now speaks an honest fallback ("Forseti found no recognizable text on this page" / "Sorry — Forseti couldn't read this page right now") when OCR fails or returns blank, so the user always hears *something* when they tap Read instead of dead air. |
| **Glossary tap-to-speak** | **New** | Each `GlossaryCard` is now `clickable`; tapping speaks "Term. Definition." through the system voice. Added a small inline tip + `RecordVoiceOver` icon on each card so the affordance is discoverable. Card body wrapped in a `SelectionContainer` so long-press copy still works without conflicting with the tap-to-speak gesture. |
| **Settings read-aloud** | **New** | Settings top bar grew a `ReadAloudControls` icon. The fetchText lambda assembles a live narration (current trial state with countdown OR "Forseti unlocked permanently" for purchased users, force-dark toggle state, bundled-rules summary, version + build, brand mark, disclaimer) so the user can audit the screen by ear. |
| **Splash duration → 7s** | **Tuned** | `BootstrapViewModel.MIN_SPLASH_MS` raised from 5000 → 7000ms so the brand mark, version, motto, sign-off, disclaimer, and the new trial banner all have time to be read. PDF warmup runs concurrently and almost always finishes before the timer, so the splash is the bottleneck (intentional). |
| **Trial countdown on splash** | **New** | `SplashOverlay` accepts an optional `EntitlementManager.Entitlement` and, for non-purchased users, renders a `TrialSplashBanner` chip ("Free trial: X days, Y hours remaining" in gold; "Trial ending soon — N hours remaining" in amber when <24h; "Trial ended — purchase Forseti to continue" in amber when expired). Purchased and Loading states render no banner — by spec, the splash stays clean for paying users. `MainActivity` now passes the live entitlement state + formatted remaining string into the splash. |

---

## 0. Critical bug fixes (spec vs codebase)

### 0.1 Guides tab crash

**Actual implementation:** Guides render **Markdown** (`com.mikepenz:multiplatform-markdown-renderer-m3`), not PDF. Content loads from `assets/guides/<file>` per `assets/guides/00_index.json`.

**Done:**

- `GuideRepository.loadBody` sanitizes GitHub-style checkboxes (`- [ ]`) that can break parsers.  
- Missing file → fallback Markdown string instead of throwing.

**Still to do (recommended):**

- On app launch (or debug-only): validate every `(id, file)` pair exists under `assets/guides/`.  
- In UI: if render fails, show a simple **“Guide unavailable”** card (Compose can’t try/catch composition; handle errors in repository + ViewModel state).  
- Optional: `Timber`/`Log` tag `Guides` with `guide id` + path.

### 0.2 Motion Builder / Drafts crash

**Actual paths:** `DraftCatalog`, `DraftGenerator`, `DraftsViewModel.materialize`.

**Done:**

- Bundled PDF copy returns null if asset missing; generation wrapped in `runCatching`.

**Still to do:**

- Pre-flight modal: required fields / case metadata when you add a formal **Case profile** screen.  
- Log template id + throwable in debug builds.

---

## 1. Deadlines + case profile (integration)

**Goal:** Richer case details → better defaults for deadlines and folder naming.

**Current:** `CaseEntity` has title, court, case number, role, complaint filed date optional.

**Planned:**

- Optional completeness indicator (not necessarily “70%”) driving a **banner**: *Complete your case details to unlock full deadline automation.*  
- Pass structured metadata into folder naming / future templates.

---

## 2. Case import behavior

**Not implemented.** Requires storage permission strategy (scoped storage), SAF (`ACTION_OPEN_DOCUMENT`), and routing into `CaseFolderService` paths.

**UX:** One tip per session (“Rename files before import…”) + DataStore flag “don’t show again today.”

---

## 3. Case workspace folder generation

**Implemented (baseline):** `com.forseti.casefiles.CaseFolderService` — triggered when saving a deadline (see `DeadlinesViewModel`).

**Still to do:**

- Open folder from deadline row (Intent `ACTION_VIEW` / file manager path text).  
- Mark phases complete / remedial subfolders on missed deadlines (spec enhancement).  
- Export ZIP / PDF binder (section 5).

---

## 4. Splash screen

**Implemented:** Compose `SplashOverlay` during bootstrap; version label.

**Tuning:** Increase `Modifier.size(...)` on the logo drawable or replace `@drawable/splash_raven_scales` with higher-resolution art.

---

## 5. Export / print / email entire case

**Not implemented.** Constraints:

- All generation on-device; user-triggered share only.  
- Prefer `java.util.zip`, existing `PdfDocument` patterns, `FileProvider` (already used for drafts).

---

## 6. Document Scanner tab (implemented baseline)

**Files:** `ui/screens/ScannerScreen.kt`, `ui/screens/ScannerViewModel.kt`, `casefiles/ScannerService.kt`.

**Behavior:** Pick the active case from a chip strip, capture pages with the rear camera, review/remove thumbnails, then save them as a single Letter-size PDF into `case_workspace/<case>/98_Scans/` via `PdfDocument`. Atomic write (`*.partial` → rename).

**Future:** ML Kit-based perspective crop, page reorder, OCR-on-save.

---

## 7. Backup tab (implemented baseline)

**Files:** `ui/screens/BackupScreen.kt`, `ui/screens/BackupViewModel.kt`, `casefiles/BackupService.kt`.

**Behavior:** "Save backup ZIP" walks `case_workspace/`, produces a timestamped ZIP under app cache (`backups/forseti_backup_<ts>.zip`), and lists prior backups for share or delete. Includes a `MANIFEST.txt` so an empty workspace still produces a valid archive.

**Future:** Direct save to `Downloads/` via MediaStore on API 29+, optional Room DB export, in-app restore using SAF (`ACTION_OPEN_DOCUMENT`).

---

## 8. Settings — disclaimer rewrite (recommended text)

Use short, honest language (adapt `strings.xml`):

> Forseti helps you organize procedure and draft filings, but it can make mistakes. Always review every document against your court’s rules before filing. You are responsible for your legal decisions and submissions.

Keep separate from the existing first-run disclaimer overlay if both are needed (informational vs. mandatory acknowledge).

---

## 9. Guides content expansion

Guides must stay **original prose** — summarize FRCP concepts; do not paste rule text. Ideas for new `assets/guides/*.md` + `00_index.json` entries:

- Rule 12 motions overview  
- Discovery requests checklist  
- Exhibits and trial binders  
- Service and subpoena hygiene  

---

## 10. State Rules tab (screenshots context)

- List UI: `StatesScreen` + `StateRules` URLs + `StateCacheManager` downloads.  
- Failures: fix **URLs** in `StateRules.kt`, handle HTTP errors in UI (snackbar), optional “recent” list from cache metadata.

---

## 11. Build & install (developer)

```bash
cd ~/Desktop/Projects/Forseti
bash scripts/fetch_assets.sh
bash scripts/fetch_fonts.sh
./gradlew installDebug
adb shell am start -n com.forseti.debug/com.forseti.MainActivity
```

---

## 12. Related docs

- `docs/RUN_LOCALLY.md` — environment setup  
- `docs/QA_CHECKLIST.md` — manual QA  

---

*Last consolidated for Cursor / Copilot handoff. Update the Status table when shipping features.*
