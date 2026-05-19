# Forseti

> *Lawgiver of the Aesir.*
> A pro-se litigant's companion for the Federal Rules of Civil Procedure.

Forseti is a native Android app (Kotlin + Jetpack Compose) that opens to the
Federal Rules of Civil Procedure (Dec 1, 2024 restyled edition) and gives a
self-represented litigant the support tools that paid practice management
software gives an attorney - all on-device, all offline-capable, all free.

This project is **information only**. It is not legal advice and using it does
not create an attorney-client relationship.

## Features

| Tab | What it does |
| --- | --- |
| Quick Jump | Collapsible TOC of the FRCP, with rule-level search; jumps the main PDF viewer to the selected rule. |
| Motion Drafts | Bundled USCourts Pro Se forms + 14 generated PDF templates for FRCP-required filings that have no official form. Print or share to "Save as PDF". |
| Guides | 10 plain-language walkthroughs (filing, responding, discovery, MSJ, hearings, computing time, digital case folders, common mistakes, OCR workflow). |
| State Rules | All 50 states + DC + every federal circuit, linking to the official rules PDF. Long-press to download for offline. |
| Deadlines | Per-case Rule 6 deadline tracker with FRCP timing rules built in (21-day answer, 30-day discovery, 28-day Rule 59 etc.), local notifications, and ICS export. |
| Glossary | 75+ legal terms, A-Z, searchable. |
| Notes & Bookmarks | Per-rule annotations and starred sections, persisted in Room. |
| Settings | Theme, content info, version, disclaimer. |

Plus:

- Splash screen with a 5-second hold and the *raven on scales* mark.
- Camera capture + on-device ML Kit OCR to populate generated drafts.
- Android Print framework integration for any draft - print directly or save to PDF.

## Tech stack

- Kotlin 2.0 / Jetpack Compose / Material 3
- Hilt for DI
- Room for persistence (bookmarks, notes, cases, deadlines)
- DataStore for prefs
- WorkManager for offline state-rule downloads and deadline notifications
- CameraX + ML Kit Text Recognition (on-device)
- Built-in `android.graphics.pdf.PdfRenderer` for PDF viewing (no third-party
  PDF native lib — keeps us 16 KB-page-aligned for Android 15+)
- Built-in `android.graphics.pdf.PdfDocument` for draft generation
- Ktor (OkHttp engine) for downloads
- Min SDK 26 (Android 8) / Target SDK 35 (Pixel 9)

## Getting started

1. Clone the repo.
2. Run `scripts/fetch_assets.sh` from the project root. It downloads the
   public-domain FRCP 2024 PDF and several USCourts pro se forms into
   `app/src/main/assets/`.
3. Open in Android Studio (Iguana or newer) and build / run.

The app degrades gracefully when assets are missing: it falls back to a
hard-coded TOC outline so Quick Jump still works during development.

## Project layout

```
forseti/
  app/
    src/main/
      java/com/forseti/
        ui/         Compose screens, theme, sidebar
        pdf/        PdfRenderer-backed repo + curated TOC + PDF viewer composable
        drafts/     PdfDocument generators per FRCP rule + bundled forms
        ocr/        CameraX + ML Kit OCR pipeline
        deadlines/  Rule 6 engine, ICS exporter, notification worker
        states/     50-state directory + offline cache
        guides/     Markdown guide loader
        glossary/   Bundled JSON glossary
        data/       Room db, DAOs, entities
        di/         Hilt module
        util/       Misc helpers
      assets/
        rules/frcp_2024.pdf       (added by fetch_assets.sh)
        forms/*.pdf               (added by fetch_assets.sh)
        guides/*.md
        glossary.json
      res/drawable/splash_raven_scales.png
  scripts/fetch_assets.sh
  build.gradle.kts
  settings.gradle.kts
```

## Performance budget (Pixel 9)

- Install size: < 60MB
- Cold start to first frame: < 1.2s; splash held to 5s deliberately
- PDF page render: < 80ms (`PdfRenderer`, ARGB bitmap)
- OCR latency: < 600ms per snapshot on Tensor G4
- All features work offline except state PDF downloads

## Source provenance

| Asset | Source | License |
| --- | --- | --- |
| FRCP 2024 PDF | uscourts.gov | Public domain (17 U.S.C. Sec. 105) |
| Pro Se 1, 2, 7, 14, 15 forms | uscourts.gov | Public domain |
| AO 240, 241, 242, 243 | uscourts.gov | Public domain |
| State rule URLs | each state judiciary | Linked, not rehosted |
| Guides, glossary, generated drafts | Forseti | CC BY 4.0 |
| Splash raven mark | bundled in `res/drawable/` | Project-internal |

## License

The app source is GPL-3.0. Generated drafts and guide content are CC BY 4.0
unless otherwise noted in the file. See `LICENSE` for the full text.
