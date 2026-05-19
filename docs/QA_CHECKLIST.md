# Forseti QA Checklist

The QA budget the plan committed to (Pixel 9 / API 35 / Tensor G4):

| Metric | Budget | How to measure |
| --- | --- | --- |
| Cold start (process start to first frame) | < 1.2s | `adb shell am start -W com.forseti/.MainActivity` |
| Splash hold | exactly 5s + warmup | Wall-clock from launcher tap to first non-splash frame |
| Install size | < 60MB | `adb shell du -h /data/app/.../base.apk` after release build with `--release` |
| PDF page render | < 80ms / page | Trace `renderPage()` with Macrobenchmark |
| OCR latency | < 600ms / capture | Trace `OcrAnalyzer.recognize()` |
| Offline | every screen except States download | Airplane mode smoke run |

## Manual smoke run (Pixel 9 emulator, factory image)

Pre-req: `scripts/fetch_assets.sh` has been run and the FRCP PDF + pro se forms
are present in `app/src/main/assets/`.

1. Cold launch from launcher.
   - [ ] Splash mark (raven on scales) appears immediately.
   - [ ] Splash holds for ~5 seconds.
   - [ ] Disclaimer overlay appears once on first launch; tapping "I understand" dismisses it.
2. Sidebar.
   - [ ] Sidebar visible on the left, color #2A2D33.
   - [ ] Tap the back chevron in the sidebar header -> sidebar collapses with animation.
   - [ ] Tap the menu icon in the top bar -> sidebar expands again.
3. Quick Jump.
   - [ ] Main pane shows page 1 of FRCP.
   - [ ] Tap the book icon in the top bar -> Quick Jump panel slides down.
   - [ ] Type "Rule 12" -> only matching rules remain.
   - [ ] Tap "Rule 12" -> main pane jumps to that page; panel closes.
   - [ ] Pinch to zoom in -> all pages scale.
4. Drafts.
   - [ ] Sections render: Pleadings, Discovery, Motions, Disclosures, Service, Judgment, Fees.
   - [ ] Tap "Print / Save PDF" on a Generated draft -> Android print sheet opens with the draft preview.
   - [ ] Tap "Capture & Fill" on a Generated draft -> camera permission prompt -> camera preview.
   - [ ] Capture a page of typed text -> review sheet shows blocks.
   - [ ] Tap a block, then tap a field -> the field shows the text.
   - [ ] Tap "Build PDF" -> print sheet shows draft with the captured text in the caption.
   - [ ] Tap "Print / Save PDF" on a Bundled form (e.g. Pro Se 1) -> the official USCourts PDF prints.
5. Guides.
   - [ ] All 10 guides listed.
   - [ ] Tapping any guide opens the markdown view.
   - [ ] Back arrow returns to the index.
6. States.
   - [ ] All 50 states + DC + federal circuits listed.
   - [ ] Filter by "Texas" -> only Texas remains.
   - [ ] Tap any row -> opens browser to the official PDF.
   - [ ] Long-press any row -> shows download starting; cloud icon turns gold when done.
   - [ ] Long-press a downloaded row -> deletes the offline copy.
7. Deadlines.
   - [ ] FAB labeled "Add case" when no cases exist.
   - [ ] Add case "Test v. Test" -> appears in the case selector.
   - [ ] FAB now reads "Add deadline".
   - [ ] Quick-add "File answer (after personal service)" -> deadline = trigger + 21 days, rolled off weekend/holiday.
   - [ ] Toggle the checkbox -> deadline grays out.
   - [ ] Tap "Export ICS" -> share sheet appears with text/calendar payload.
8. Glossary.
   - [ ] Terms grouped by first letter.
   - [ ] Search "subpoena" -> single result.
9. Notes & Bookmarks.
   - [ ] Empty states render correctly.
   - [ ] Add a note with anchor "rule.12.b.6" and body text.
   - [ ] Note appears in the Notes tab.
10. Settings.
    - [ ] Version name + code matches `BuildConfig`.
11. Offline test.
    - [ ] Enable airplane mode.
    - [ ] All screens still work (no crashes, no freezes).
    - [ ] States tab still lists everything; tapping "Open" fails to load (expected); cached PDFs still openable.

## Automated checks

Add these to `:app:check`:

- Robolectric unit test for `Rule6.computeDeadline()` covering:
  - Triggering on a Friday with 14 days, no service mode = expect rolled off Saturday.
  - Triggering with mail (+3) and a deadline that lands on Memorial Day = expect Tuesday.
  - Juneteenth observance variants (Sat -> Fri, Sun -> Mon).
- ML Kit smoke test that runs `OcrAnalyzer.recognize` against a PNG asset of typed lorem-ipsum and asserts > 1 block returned.
- Room migration test against an `assets/test_dbs/forseti_v1.db` snapshot.

## Crash reporting

Forseti deliberately ships *without* third-party telemetry. Crash collection is
enabled only via Android's built-in `ApplicationExitInfo` (`ActivityManager.getHistoricalProcessExitReasons`).
Add a one-liner in `ForsetiApp.onCreate()` to log the most-recent ANR/native
crash reason if you want it in logcat.

## F-Droid friendliness

- ML Kit's text recognition uses Google Play Services on most devices; a
  no-Google fallback would require swapping in `tessdata` + `tess-two`. Filed as
  a follow-up; F-Droid build will continue to bundle ML Kit unless we publish a
  separate `f-droid` flavor.
- Pdfium is open-source.
- Ktor + OkHttp + Kotlinx are all OSS.
- No Firebase or analytics dependencies.
