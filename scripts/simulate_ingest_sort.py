#!/usr/bin/env python3
"""
Simulate Forseti v0.1.10 IDP ingest sorting offline.
Mirrors IngestClassifier + CaseFolderService KEYWORD_ROUTES + ConfidenceRouter.
"""
from __future__ import annotations

import json
import re
import shutil
import zipfile
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from dataclasses import dataclass
from datetime import date
from enum import Enum, auto
from pathlib import Path

HIGH = 0.85
MEDIUM = 0.70
RENAME_TEMPLATE = "{date}_{docType}_{caseId}_v1"

KEYWORD_ROUTES: list[tuple[str, str]] = [
    ("trial brief", "10_Trial/Trial_Brief"),
    ("witness list", "10_Trial/Witness_List"),
    ("witness", "10_Trial/Witness_List"),
    ("jury instruction", "10_Trial/Jury_Instructions"),
    ("jury", "10_Trial/Jury_Instructions"),
    ("trial binder", "10_Trial/Final_Binder"),
    ("final binder", "10_Trial/Final_Binder"),
    ("pretrial", "09_Hearings/Prep"),
    ("hearing notice", "09_Hearings/Notices"),
    ("notice of hearing", "09_Hearings/Notices"),
    ("hearing", "09_Hearings/Notices"),
    ("exhibit list", "08_Exhibits/Labels"),
    ("final exhibit", "08_Exhibits/Final_Exhibits"),
    ("exhibit", "08_Exhibits/Labels"),
    ("calendar", "07_Deadlines"),
    ("deadline", "07_Deadlines"),
    ("communications", "06_Correspondence/Misc"),
    ("communication", "06_Correspondence/Misc"),
    ("text message", "06_Correspondence/Misc"),
    ("messenger", "06_Correspondence/Misc"),
    ("facebook", "06_Correspondence/Misc"),
    ("opposing counsel", "06_Correspondence/Opposing_Party"),
    ("opposing party", "06_Correspondence/Opposing_Party"),
    ("court letter", "06_Correspondence/Court"),
    ("letter", "06_Correspondence/Misc"),
    ("email", "06_Correspondence/Misc"),
    ("motion to dismiss", "05_Motions/Drafts"),
    ("motion in limine", "05_Motions/Drafts"),
    ("notice of motion", "05_Motions/Drafts"),
    ("memorandum in support", "05_Motions/Drafts"),
    ("memorandum of law", "05_Motions/Drafts"),
    ("memorandum", "05_Motions/Drafts"),
    ("brief in support", "05_Motions/Drafts"),
    ("motion", "05_Motions/Drafts"),
    ("opposition", "05_Motions/Drafts"),
    ("reply", "05_Motions/Drafts"),
    ("court response", "05_Motions/Court_Responses"),
    ("screenshot", "04_Evidence/Screenshots"),
    ("photos for", "04_Evidence/Photos"),
    ("videos for", "04_Evidence/Video"),
    ("evidence for", "04_Evidence/PDFs"),
    ("evidence", "04_Evidence/PDFs"),
    ("gmail", "06_Correspondence/Misc"),
    ("declaration", "03_Discovery"),
    ("affidavit", "03_Discovery"),
    ("interrogatories", "03_Discovery/Interrogatories"),
    ("interrog", "03_Discovery/Interrogatories"),
    ("request for production", "03_Discovery/Requests_for_Production"),
    ("requests for production", "03_Discovery/Requests_for_Production"),
    ("rfp", "03_Discovery/Requests_for_Production"),
    ("request for admission", "03_Discovery/Admissions"),
    ("requests for admission", "03_Discovery/Admissions"),
    ("rfa", "03_Discovery/Admissions"),
    ("deposition", "03_Discovery/Depositions"),
    ("depo", "03_Discovery/Depositions"),
    ("discovery response", "03_Discovery/Discovery_Responses"),
    ("discovery", "03_Discovery"),
    ("subpoena duces tecum", "03_Discovery"),
    ("subpoena", "03_Discovery"),
    ("proof of service", "02_Service_of_Process/Proof_of_Service"),
    ("service of process", "02_Service_of_Process/Proof_of_Service"),
    ("summons", "02_Service_of_Process/Summons"),
    ("service", "02_Service_of_Process/Proof_of_Service"),
    ("counterclaim", "01_Pleadings"),
    ("cross-claim", "01_Pleadings"),
    ("crossclaim", "01_Pleadings"),
    ("complaint", "01_Pleadings/Complaint"),
    ("answer", "01_Pleadings/Answer"),
    ("order", "01_Pleadings/Orders"),
    ("judgment", "01_Pleadings/Orders"),
    ("pleading", "01_Pleadings"),
    ("parties", "00_Case_Overview"),
    ("case overview", "00_Case_Overview"),
    ("notes", "00_Case_Overview/Notes"),
]

IMAGE_EXTS = {"jpg", "jpeg", "png", "webp", "heic", "heif"}
VIDEO_EXTS = {"mp4", "3gpp", "mov", "avi"}
AUDIO_EXTS = {"mp3", "wav", "m4a"}
TEXT_EXTS = {"txt", "md"}


class MatchSource(Enum):
    NONE = auto()
    PATH_SCHEMA = auto()
    PATH_KEYWORD = auto()
    FILENAME_KEYWORD = auto()
    KEYWORD_TEXT = auto()
    SCHEMA_TEXT = auto()


@dataclass
class Classification:
    document_type: str
    folder: str
    confidence: float
    matched_keyword: str | None
    schema_id: str | None
    tier: str
    saved_folder: str
    suggested_filename: str


def normalize_hint(text: str) -> str:
    return re.sub(r"[^a-z0-9]+", " ", text.lower()).strip()


def path_contains_needle(path_hint: str, needle: str) -> bool:
    path_portion = path_hint.rsplit("/", 1)[0] if "/" in path_hint else path_hint
    return needle in path_portion


def find_keyword_route(hint: str) -> tuple[str, str] | None:
    normalized = normalize_hint(hint)
    if not normalized:
        return None
    filed = any(x in normalized for x in ("filed", "stamped", "entered", "e-filed"))
    for needle, target in KEYWORD_ROUTES:
        if needle in normalized:
            if filed and target.startswith("05_Motions/"):
                return needle, "05_Motions/Filed"
            return needle, target
    return None


def document_type_for_folder(folder: str) -> str | None:
    mapping = [
        ("Interrogatories", "Interrogatories"),
        ("Complaint", "Complaint"),
        ("Answer", "Answer"),
        ("Orders", "CourtOrder"),
        ("Photos", "PhotoEvidence"),
        ("Video", "VideoEvidence"),
        ("Screenshots", "Screenshot"),
        ("Motions", "Motion"),
        ("Correspondence", "Correspondence"),
        ("Discovery", "Discovery"),
        ("Depositions", "Deposition"),
        ("Proof_of_Service", "ProofOfService"),
        ("Summons", "Summons"),
        ("Exhibits", "Exhibit"),
        ("Hearings", "HearingNotice"),
    ]
    for frag, dtype in mapping:
        if frag in folder:
            return dtype
    return None


def normalize_confidence(raw: float, matched: bool, has_text: bool, source: MatchSource) -> float:
    if not matched:
        return 0.35 if has_text else 0.15
    if source in (MatchSource.PATH_KEYWORD, MatchSource.PATH_SCHEMA):
        return min(0.98, max(0.85, 0.88 + min(raw, 1.3) * 0.08))
    if source == MatchSource.FILENAME_KEYWORD:
        return min(0.90, max(0.78, 0.82 + min(raw, 1.2) * 0.06))
    if source == MatchSource.KEYWORD_TEXT:
        return 0.76
    if source == MatchSource.SCHEMA_TEXT:
        return min(0.96, max(0.68, 0.52 + raw * 0.32))
    return 0.35 if has_text else 0.15


def tier(confidence: float, has_match: bool) -> str:
    if not has_match or confidence < MEDIUM:
        return "inbox-only"
    if confidence >= HIGH:
        return "auto-filed"
    return "inbox-suggested"


def resolve_saved_folder(predicted: str, routing_tier: str) -> str:
    return predicted if routing_tier == "auto-filed" else "99_Inbox"


def build_rename(doc_type: str, original: str, case_id: str = "nc-2026-0073") -> str:
    ext = original.rsplit(".", 1)[-1] if "." in original else "pdf"
    d = date.today().strftime("%Y%m%d")
    cid = re.sub(r"[^A-Za-z0-9\-]", "", case_id)[:24]
    base = RENAME_TEMPLATE.replace("{date}", d).replace("{docType}", doc_type).replace("{caseId}", cid)
    return f"{base}.{ext}"


def extract_docx_text(path: Path) -> str:
    try:
        with zipfile.ZipFile(path) as zf:
            with zf.open("word/document.xml") as f:
                root = ET.parse(f).getroot()
        texts = []
        for el in root.iter():
            tag = el.tag.rsplit("}", 1)[-1]
            if tag == "t" and el.text:
                texts.append(el.text)
        return " ".join(texts)[:4000]
    except Exception:
        return ""


def extract_pdf_text(path: Path) -> str:
    try:
        import subprocess
        out = subprocess.run(
            ["pdftotext", "-l", "1", str(path), "-"],
            capture_output=True,
            text=True,
            timeout=8,
        )
        if out.returncode == 0:
            return out.stdout[:4000]
    except Exception:
        pass
    return ""


def mime_fallback(rel_path: str, name: str, ext: str) -> tuple[str, str, float]:
    text = normalize_hint(f"{rel_path}/{name}")
    hit = find_keyword_route(f"{rel_path}/{name}")
    if hit:
        needle, folder = hit
        in_path = path_contains_needle(f"{rel_path}/{name}".lower(), needle)
        conf = 0.92 if in_path else 0.86
        return folder, document_type_for_folder(folder) or "Unassigned", conf
    ext_l = ext.lower()
    is_screenshot = "screenshot" in text
    if ext_l in IMAGE_EXTS:
        folder = "04_Evidence/Screenshots" if is_screenshot else "04_Evidence/Photos"
        return folder, "Screenshot" if is_screenshot else "PhotoEvidence", 0.20
    if ext_l in VIDEO_EXTS:
        return "04_Evidence/Video", "VideoEvidence", 0.20
    if ext_l in AUDIO_EXTS:
        return "04_Evidence/Audio", "AudioEvidence", 0.20
    if ext_l == "pdf":
        return "99_Inbox", "Unassigned", 0.20
    if ext_l in TEXT_EXTS:
        return "00_Case_Overview/Notes", "Notes", 0.20
    return "99_Inbox", "Unassigned", 0.15


def classify_file(rel_path: str, name: str, text: str, schemas: list[dict]) -> Classification:
    hint = f"{rel_path}/{name}" if rel_path else name
    hint_lower = hint.lower()
    sample = text[:1000]
    haystack = f"{hint} {sample}".lower()

    best = None
    best_score = 0.0
    best_match = None
    match_source = MatchSource.NONE

    for doc_schema in schemas:
        weight = doc_schema.get("weight", 1.0)
        for keyword in doc_schema.get("keywords", []):
            k = keyword.lower()
            if k in haystack:
                in_path = path_contains_needle(hint_lower, k)
                score = weight * (0.9 + len(k) * 0.01)
                if score > best_score:
                    best_score = score
                    best = doc_schema
                    best_match = keyword
                    match_source = MatchSource.PATH_SCHEMA if in_path else MatchSource.SCHEMA_TEXT
        for anchor in doc_schema.get("anchors", []):
            a = anchor.lower()
            if a in haystack:
                score = weight * 0.85
                if score > best_score:
                    best_score = score
                    best = doc_schema
                    best_match = anchor
                    match_source = MatchSource.SCHEMA_TEXT

    doc_type = best["documentType"] if best else "Unassigned"
    folder = best["folder"] if best else "99_Inbox"
    schema_id = best["id"] if best else None
    confidence = normalize_confidence(best_score, best is not None, bool(sample.strip()), match_source)

    route = find_keyword_route(hint)
    if route:
        needle, route_folder = route
        in_path = path_contains_needle(hint_lower, needle)
        in_filename = needle in name.lower()
        route_conf = 0.92 if in_path else (0.86 if in_filename else (0.78 if needle in haystack else confidence))
        if route_conf >= confidence or schema_id is None:
            folder = route_folder
            doc_type = document_type_for_folder(route_folder) or doc_type
            confidence = route_conf
            best_match = needle
            schema_id = schema_id or "keyword-route"
            match_source = (
                MatchSource.PATH_KEYWORD if in_path
                else MatchSource.FILENAME_KEYWORD if in_filename
                else MatchSource.KEYWORD_TEXT
            )

    has_match = schema_id is not None
    routing = tier(confidence, has_match)
    saved = resolve_saved_folder(folder, routing)
    suggested = build_rename(doc_type, name)

    return Classification(
        document_type=doc_type,
        folder=folder,
        confidence=confidence,
        matched_keyword=best_match,
        schema_id=schema_id,
        tier=routing,
        saved_folder=saved,
        suggested_filename=suggested if routing == "auto-filed" else name,
    )


def should_skip(path: Path) -> bool:
    parts = {p.lower() for p in path.parts}
    if any(p.startswith(".") for p in path.parts):
        return True
    if "system volume information" in parts:
        return True
    if "oebps" in parts or "meta-inf" in parts:
        return True
    return False


def main() -> None:
    repo = Path(__file__).resolve().parents[1]
    source = Path(
        "/home/user/Desktop/Pelletier_Cases/Pelletier-v-Anderson_Cases/"
        "Pelletier_VS_Anderson_RI_Defimation"
    )
    out_root = Path("/home/user/Desktop/test OCR folder")
    case_ws = out_root / "Case_003_Pelletier-vs-anderson"

    schema_path = repo / "app/src/main/assets/ingest/ingest_schema.json"
    schemas = json.loads(schema_path.read_text())["schemas"]

    if case_ws.exists():
        shutil.rmtree(case_ws)
    case_ws.mkdir(parents=True)

    audit: list[str] = []
    folder_tally: Counter[str] = Counter()
    tier_tally: Counter[str] = Counter()
    by_source: dict[str, Counter[str]] = defaultdict(Counter)
    examples: list[tuple[str, str, Classification]] = []

    files = [p for p in source.rglob("*") if p.is_file() and not should_skip(p)]
    files.sort()

    for src in files:
        rel = src.relative_to(source)
        rel_path = str(rel.parent) if rel.parent != Path(".") else ""
        name = rel.name
        ext = name.rsplit(".", 1)[-1] if "." in name else ""

        text = ""
        ext_l = ext.lower()
        if ext_l == "docx":
            text = extract_docx_text(src)
        elif ext_l == "pdf":
            text = extract_pdf_text(src)

        if ext_l in IMAGE_EXTS or ext_l in VIDEO_EXTS:
            # Images go through OCR in app; simulation uses path/filename only
            cls = classify_file(rel_path, name, "", schemas)
            if cls.confidence < MEDIUM and ext_l in IMAGE_EXTS:
                fb_folder, fb_type, fb_conf = mime_fallback(rel_path, name, ext)
                if fb_conf > cls.confidence or cls.tier == "inbox-only":
                    routing = tier(fb_conf, True) if find_keyword_route(f"{rel_path}/{name}") else "inbox-only"
                    cls = Classification(
                        document_type=fb_type,
                        folder=fb_folder,
                        confidence=fb_conf,
                        matched_keyword=find_keyword_route(f"{rel_path}/{name}") and find_keyword_route(f"{rel_path}/{name}")[0],
                        schema_id="mime-fallback",
                        tier=routing,
                        saved_folder=resolve_saved_folder(fb_folder, routing),
                        suggested_filename=name,
                    )
        else:
            cls = classify_file(rel_path, name, text, schemas)
            if cls.tier == "inbox-only" and ext_l not in ("docx", "pdf"):
                fb_folder, fb_type, fb_conf = mime_fallback(rel_path, name, ext)
                routing = tier(fb_conf, find_keyword_route(f"{rel_path}/{name}") is not None)
                cls = Classification(
                    document_type=fb_type,
                    folder=fb_folder,
                    confidence=fb_conf,
                    matched_keyword=(find_keyword_route(f"{rel_path}/{name}") or (None, None))[0],
                    schema_id="mime-fallback",
                    tier=routing,
                    saved_folder=resolve_saved_folder(fb_folder, routing),
                    suggested_filename=name,
                )

        dest_dir = case_ws / cls.saved_folder
        dest_dir.mkdir(parents=True, exist_ok=True)
        dest = dest_dir / cls.suggested_filename
        n = 1
        while dest.exists():
            stem = cls.suggested_filename.rsplit(".", 1)[0]
            ext_part = cls.suggested_filename.rsplit(".", 1)[-1] if "." in cls.suggested_filename else ""
            dest = dest_dir / (f"{stem}_{n}.{ext_part}" if ext_part else f"{stem}_{n}")
            n += 1
        dest.symlink_to(src.resolve())

        folder_tally[cls.saved_folder] += 1
        tier_tally[cls.tier] += 1
        top = rel_path.split("/")[0] if rel_path else "(root)"
        by_source[top][cls.saved_folder] += 1

        audit.append(
            f"{name[:40]:<40} | {dest.name[:40]:<40} | {cls.saved_folder:<45} | "
            f"{cls.confidence*100:5.1f}% | {cls.document_type:<18} | {cls.tier:<16} | "
            f"kw={cls.matched_keyword or '—'} | src={rel_path[:50]}"
        )
        if len(examples) < 25:
            examples.append((str(rel), cls.saved_folder, cls))

    lines = [
        "Forseti IDP ingest simulation — Pelletier_VS_Anderson_RI_Defimation",
        f"Source: {source}",
        f"Output workspace: {case_ws}",
        f"confidenceHigh={HIGH} confidenceMedium={MEDIUM}",
        f"Files processed: {len(files)}",
        "",
        "=== Tier totals ===",
        *[f"  {k}: {v}" for k, v in tier_tally.most_common()],
        "",
        "=== Saved folder totals ===",
        *[f"  {v} × {k}" for k, v in folder_tally.most_common()],
        "",
        "=== By your source top-folder → Forseti destination ===",
    ]
    for src_name in sorted(by_source):
        lines.append(f"\n[{src_name}]")
        for dest, count in by_source[src_name].most_common():
            lines.append(f"  → {dest}: {count}")

    lines += [
        "",
        "original | saved_as | saved_folder | confidence | type | tier | keyword | source_path",
        "-" * 140,
        *audit,
    ]
    (case_ws / "INGEST_AUDIT_latest.txt").write_text("\n".join(lines), encoding="utf-8")

    report = [
        "# Forseti Auto-Sort Simulation",
        "",
        f"**Source (your organized case):** `{source}`",
        f"**Simulated Brokkr-Forge workspace:** `{case_ws}`",
        "",
        "## Summary",
        "",
        f"| Metric | Count |",
        f"|--------|------:|",
        f"| Files processed | {len(files)} |",
        f"| Auto-filed (≥85%) | {tier_tally.get('auto-filed', 0)} |",
        f"| Inbox + File Here (70–84%) | {tier_tally.get('inbox-suggested', 0)} |",
        f"| Inbox only (<70%) | {tier_tally.get('inbox-only', 0)} |",
        "",
        "## How to read the test folder",
        "",
        "Open `test OCR folder/Case_003_Pelletier-vs-anderson/` — this mirrors what Forseti creates on-device.",
        "Files are **symlinks** to your originals (safe, no duplication).",
        "See `INGEST_AUDIT_latest.txt` for the full per-file log (same format as the app).",
        "",
        "## Example routings from your labels",
        "",
        "| Your folder / file | Forseti saves to | Tier |",
        "|--------------------|------------------|------|",
    ]
    for rel, saved, cls in examples[:20]:
        report.append(f"| `{rel}` | `{saved}` | {cls.tier} ({cls.confidence*100:.0f}%) |")

    report += [
        "",
        "## Your folder → expected Brokkr mapping",
        "",
        "| Your organized folder | Forseti should auto-file to |",
        "|----------------------|----------------------------|",
        "| `PRINT FOR CASE/Complaint/` | `01_Pleadings/Complaint` |",
        "| `Defendant Anderson REsponses/.../Response_Answer_01/` | `01_Pleadings/Answer` |",
        "| `Exhibit folder def case/` | `08_Exhibits/Labels` |",
        "| `Exhibit folder def case/Stalking photos/` | `04_Evidence/Photos` |",
        "| `PNSTAW/Domestic Violence 2019/` | `04_Evidence/PDFs` or Photos |",
        "| `Building case to sue kim/October Advocacy/` | `06_Correspondence/Misc` |",
        "| `LAWS/` (reference ebooks) | `99_Inbox` (manual — not court filing) |",
        "",
        "## Note on slow ingest in the app",
        "",
        "Bulk folder ingest runs OCR on every image/PDF sequentially on-device.",
        "358+ files can take several minutes before the summary snackbar appears.",
        "Check `INGEST_AUDIT_latest.txt` in the case workspace for live progress after completion.",
        "",
    ]
    (out_root / "README.md").write_text("\n".join(report), encoding="utf-8")
    print(f"Done. Workspace: {case_ws}")
    print(f"README: {out_root / 'README.md'}")
    print("Tier totals:", dict(tier_tally))
    print("Top destinations:", folder_tally.most_common(8))


if __name__ == "__main__":
    main()
