# Forseti ingest sorting — training & audit notes

## Confidence tiers (v0.1.10+)

| Tier | Range | Routing | UI |
|------|-------|---------|-----|
| Auto-file | ≥ 85% | Saved to predicted Brokkr-Forge folder | "Auto-filed · 0.92" |
| Inbox + suggested | 70–84% | Saved to `99_Inbox/` with suggested folder | "File Here → …" chip |
| Inbox only | < 70% | Saved to `99_Inbox/` only | "classify manually" |

Thresholds live in `app/src/main/assets/ingest/ingest_schema.json` (`confidenceHigh`, `confidenceMedium`).

## Root cause fixed (2026-06)

Prior builds mapped schema keyword scores with `(0.58 + raw × 0.22)`, which capped typical matches at ~77–83%. **Nothing with a normal keyword weight could reach 85% auto-file**, so Pelletier-scale imports landed almost entirely in `99_Inbox`.

Additional gaps:

1. **Path hints ignored for routing** — PDF/DOCX/image ingest ran the schema classifier but did not apply `CaseFolderService.KEYWORD_ROUTES` on folder paths (`Interrogatories/`, `Complaint/`, `photos for/`, etc.).
2. **Schema keywords too OCR-specific** — e.g. `"complaint for"` did not match a parent folder named `Complaint`.
3. **Keyword fallback only for unknown mime types** — PDFs never used `decideIngestRoute()`.

## Current pipeline

```
CaseIngestService.collect → pathHaystack (relativePath/name)
  → DocumentIngestPipeline.analyze* (OCR/text + hintFilename)
  → IngestClassifier.classify
       1. Schema keyword/anchor scan on haystack
       2. CaseFolderService.findKeywordRoute(hint) overlay with path boost
  → ConfidenceRouter.tier
  → resolveTargetFolder (AUTO_FILE → schema folder; else 99_Inbox)
  → INGEST_AUDIT_latest.txt
```

### Path match confidence

| Signal | Confidence | Tier |
|--------|------------|------|
| Keyword in **folder path** (not just filename) | 0.92 | Auto-file |
| Keyword in **filename** only | 0.86 | Auto-file |
| Keyword in OCR/body only | 0.76–0.84 | Inbox suggested |
| Schema OCR anchor (e.g. COMPLAINT) | 0.68–0.96 scaled | Usually suggested |
| No match | 0.15–0.35 | Inbox only |

## Pelletier corpus expectations

| Source path pattern | Expected folder |
|--------------------|-----------------|
| `EVIDENCE FOR …/photos for …/*.jpg` | `04_Evidence/Photos` |
| `…/Interrogatories/…` | `03_Discovery/Interrogatories` |
| `…/Advocacy OutReach/…` | `06_Correspondence/Misc` |
| `…/Affidavit of Authenticity.docx` | `03_Discovery` |
| Filename or OCR contains `complaint`, `motion`, `order` | Respective pleadings/motions folders |

## Duplicates

- **Ingest**: skips files whose normalized name + size already exist in the workspace, or repeat in the same batch (`CaseIngestService`).
- **Inbox UI**: flags rows where normalized name + size match another Inbox file.

## Manual verification

After rebuild, re-ingest a Pelletier subtree and inspect `INGEST_AUDIT_latest.txt` at the case root:

```bash
cd /home/user/Desktop/Projects/Forseti
./gradlew :app:assembleDebug
```

Expect meaningful counts under `01_Pleadings`, `03_Discovery`, `04_Evidence`, `06_Correspondence` — not 400+ in `99_Inbox` alone.

## Do not change

Privileged / RI defamation special routing (if present elsewhere) — out of scope for sorting fixes.
