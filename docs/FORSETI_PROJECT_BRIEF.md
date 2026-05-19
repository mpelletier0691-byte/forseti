# Forseti — Pro Se Civil Procedure Companion (Android)

**Project brief for AI assistants and engineers.** Standalone document; safe to share. Update this file when architecture or dependencies change materially.

- **Repository root (typical):** `~/Desktop/Projects/Forseti/`
- **Audience:** U.S. *pro se* federal civil litigants on Android.
- **Primary content:** Federal Rules of Civil Procedure (FRCP), restyled edition effective Dec 1, 2024.

---

## 1. Product vision (summary)

Forseti is a **self-contained, information-only** companion: offline FRCP reading with Quick Jump, printable USCourts forms plus generated filing skeletons, and **FRCP 6** deadline math (6(a) counting + 6(d) mail/electronic +3). It is **not** legal advice; first-launch disclaimer + sidebar footer state that clearly.

**Hard non-goals:** legal advice, jurisdiction/strategy automation, UPL risk features.

---

## 2. Tech stack (as implemented)

| Concern | Choice | Notes |
| --- | --- | --- |
| Language | Kotlin 2.0.21 | K2 |
| UI | Jetpack Compose + Material 3 (BOM `2024.12.01` in version catalog) | Single activity |
| Min / Target SDK | 26 / 35 | `compileSdk = 35` |
| Build | Gradle 8.10.2 + AGP 8.7.3 | Version catalog: `gradle/libs.versions.toml` |
| DI | Hilt 2.52 + KSP | |
| DB | Room 2.6.1 | `forseti.db`, migrations empty today |
| Background | WorkManager + Hilt worker factory | Deadlines + state PDF fetch |
| Networking | Ktor + OkHttp | State-rule downloads |
| **PDF view** | **`android.graphics.pdf.PdfRenderer`** | Framework API; bundled FRCP copied to cache for `ParcelFileDescriptor` |
| PDF generate | `android.graphics.pdf.PdfDocument` | Draft DSL |
| Camera / OCR | CameraX + ML Kit on-device text recognition | |
| Markdown (Guides) | `com.mikepenz:multiplatform-markdown-renderer-m3` **0.35.0** | Pinned because **0.36+ targets compileSdk 36**; see comment in `libs.versions.toml` |
| Date math | `kotlinx-datetime` 0.6.1 | `deadlines/Rule6.kt` |
| Splash | `core-splashscreen` + in-app **`SplashOverlay`** | Bootstrap holds readiness (see `BootstrapViewModel`); version from `BuildConfig.VERSION_NAME` |
| Fonts | **Bundled** Cinzel / Inter under `res/font/` | Populate via `scripts/fetch_fonts.sh` (also invoked from `bootstrap.sh`) |
| Repos | Google, Maven Central, **JitPack** | No PDF native dependency currently; JitPack kept for optional/future artifacts |

Java **17** required.

---

## 3. Repository layout (high level)

```
app/src/main/java/com/forseti/
  ForsetiApp.kt, MainActivity.kt, BootstrapViewModel.kt
  di/AppModule.kt
  data/              Room DB, DAOs, entities
  pdf/               PdfRepository (PdfRenderer), PdfViewer, Toc, FrcpOutline
  drafts/            DraftCatalog, DraftGenerator, DraftPrinting
  guides/, glossary/, states/, deadlines/, ocr/
  casefiles/         CaseFolderService, ScannerService, BackupService
  ui/                ForsetiShell, theme, screens
  util/

assets/              rules/frcp_2024.pdf, forms/*.pdf, guides/*.md, glossary.json
scripts/             fetch_assets.sh, fetch_fonts.sh
docs/                QA_CHECKLIST, RUN_LOCALLY, this brief, implementation notes
```

### Sidebar tabs (current order)

1. Quick Jump — FRCP PDF + curated TOC
2. Motion Drafts — bundled forms + generated PDFs
3. Guides — bundled markdown
4. State Rules — 50 states + DC + circuits + offline cache
5. Deadlines — Rule 6 engine + ICS export + folder shortcut + profile nudge
6. **Case Profile** — case CRUD with completeness bar + folder path
7. **Document Scanner** — multi-page camera capture into case PDFs
8. **Backup** — atomic ZIP of the case workspace, share/delete prior backups
9. Glossary — searchable A–Z
10. Notes & Bookmarks — per-rule annotations
11. Settings — theme, version, disclaimer

---

## 4. Architecture

Single-Activity Compose app: **`Destination` enum** routes in `ForsetiShell`. ViewModels + Hilt repositories; Room for bookmarks, notes, cases, deadlines.

---

## 5. FRCP 6 engine

`deadlines/Rule6.kt`: trigger-day exclusion, count all days, roll forward off weekends + **federal** holidays, optional **+3** for mail/electronic under 6(d). **State** holidays are **not** modeled (future work).

---

## 6. PDF viewer behavior (`pdf/PdfViewer.kt`)

- Pages are a **`LazyColumn`** of rasterized bitmaps from **`PdfRenderer`**.
- **Pinch-zoom** is a **shared** `zoom` applied via `graphicsLayer` scale; container height scales so pages don’t overlap incorrectly.
- Gesture: **`detectTransformGestures`** on the list updates zoom only when **`zoomChange != 1f`** (filters pure pans from being treated as zoom). **One-finger vertical scrolling** should move between pages; if a device routes drags into the transform detector and scrolling stalls while zoomed, **zoom out** slightly—full nested-scroll handoff while zoomed is backlog polish.

---

## 7. Splash (`ui/shell/SplashOverlay.kt`)

Composable overlay until bootstrap ready: logo sized from shorter screen dimension (~**55%**, capped), **scrollable column** so name / tagline / version / subtitle stay reachable on short or landscape layouts.

---

## 8. Case workspace folders

`casefiles/CaseFolderService.kt`: creates/maintains **app-private** per-case folders when deadlines are saved (`DeadlinesViewModel`). Not a user-visible file browser yet—see `docs/Forseti_Implementation_Notes.md`.

---

## 9. Permissions / manifest

Typical: `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `CAMERA`, `RECEIVE_BOOT_COMPLETED`. **`SCHEDULE_EXACT_ALARM`** removed (WorkManager schedules notifications). Runtime notification prompt via `NotificationsPermission.kt` on Android 13+.

---

## 10. Build & run

```bash
cd ~/Desktop/Projects/Forseti
bash scripts/fetch_assets.sh    # FRCP + forms (fault tolerant)
bash scripts/fetch_fonts.sh    # Cinzel + Inter into res/font (if missing)
./gradlew installDebug
```

Or **`bootstrap.sh`** for full machine bootstrap + emulator (Linux).

---

## 11. What’s NOT done yet (backlog highlights)

**Release:** signing config / keystore script, Play listing assets, privacy policy URL, Data Safety text, IARC rating, optional OSS crash reporting (e.g. Sentry).

**Product:** state-specific holiday calendars, local rule overlays, PDF text search, per-deadline alert offsets, richer TOC for non-FRCP PDFs.

**Engineering:** `@Preview` on screens, **unit/instrumented tests for `Rule6`**, baseline profiles, Room migrations when schema changes, metered-network guard for background downloads, accessibility pass.

**Docs debt:** Some older lines in `docs/QA_CHECKLIST.md` / `RUN_LOCALLY.md` may still mention Pdfium—prefer **this brief** and `README.md` as canonical for PDF stack.

---

## 12. Legal / ethical guardrails

No automated legal advice, no cloud LLM analysis of case merits, caption-style OCR fields only—preserve disclaimer flows and in-product “information only” positioning.

---

## 13. Handoff prompt for a new chat

> You’re picking up **Forseti**, a Kotlin + Compose Android app for U.S. pro se federal civil litigants. Read `docs/FORSETI_PROJECT_BRIEF.md` and `README.md`. Repo: `~/Desktop/Projects/Forseti/`. PDF viewing uses **`PdfRenderer`**, not Pdfium. Markdown renderer is **0.35.0** (compileSdk 35). Prefer **minimal diffs**, keep the dark Norse theme, and never add legal-advice features. Next step: …

---

*End of brief. Regenerate from source when the architecture changes substantially.*
