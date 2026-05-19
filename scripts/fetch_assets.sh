#!/usr/bin/env bash
# Fetches public-domain rule PDFs into the assets folder before build.
# Idempotent: skips files that already exist and pass a basic size check.
#
# Resilient: a missing or moved URL warns and is skipped. The app already
# falls back gracefully to a hard-coded outline if the FRCP PDF is absent,
# and falls back to generated drafts if a bundled pro se form is absent.
# We never fail the bootstrap because uscourts.gov reshuffled a path.

set -uo pipefail   # NOTE: no -e; individual fetch failures must not abort.

cd "$(dirname "$0")/.."

ASSETS_RULES="app/src/main/assets/rules"
ASSETS_FORMS="app/src/main/assets/forms"
mkdir -p "$ASSETS_RULES" "$ASSETS_FORMS"

skipped=()
fetched=()

# fetch DEST MIN_BYTES URL [URL ...]
# Tries each URL in order; first one that yields a file of MIN_BYTES wins.
# If every URL fails, records DEST in `skipped` and returns 0 anyway.
fetch() {
    local dest="$1"; shift
    local minbytes="$1"; shift
    if [[ -f "$dest" && $(stat -c%s "$dest" 2>/dev/null || stat -f%z "$dest") -ge "$minbytes" ]]; then
        echo "skip   $dest (already present)"
        return 0
    fi
    for url in "$@"; do
        if curl -fSL --retry 2 --retry-delay 1 --max-time 60 -o "$dest.partial" "$url" 2>/dev/null; then
            local size
            size=$(stat -c%s "$dest.partial" 2>/dev/null || stat -f%z "$dest.partial")
            if [[ "$size" -ge "$minbytes" ]]; then
                mv "$dest.partial" "$dest"
                echo "fetch  $dest  <- $url"
                fetched+=("$dest")
                return 0
            fi
        fi
        rm -f "$dest.partial"
    done
    echo "WARN   $dest  could not be fetched from any source - skipping (the app handles this)"
    skipped+=("$dest")
    return 0
}

# Federal Rules of Civil Procedure (Dec 1, 2024 restyled edition)
fetch "$ASSETS_RULES/frcp_2024.pdf" 200000 \
    "https://www.uscourts.gov/sites/default/files/2025-02/federal-rules-of-civil-procedure-dec-1-2024_0.pdf" \
    "https://www.uscourts.gov/sites/default/files/federal-rules-of-civil-procedure-dec-1-2024_0.pdf"

# Extract the text layer so the Quick Jump tab can read aloud / copy / display
# selectable text WITHOUT running on-device OCR. android.graphics.pdf.PdfRenderer
# doesn't expose the text stream, and OCR on dense legal type is unreliable.
# Pages are delimited by form-feed (\f) in pdftotext output, which the app uses
# directly to index pages.
#
# Regenerates whenever the PDF is newer than the sidecar OR the sidecar is
# missing. Gracefully degrades to OCR if pdftotext isn't installed.
regenerate_frcp_sidecar() {
    local pdf="$ASSETS_RULES/frcp_2024.pdf"
    local sidecar="$ASSETS_RULES/frcp_2024.pages.txt"
    if [[ ! -f "$pdf" ]]; then
        return 0
    fi
    if ! command -v pdftotext >/dev/null 2>&1; then
        echo "WARN   pdftotext not installed; cannot regenerate $sidecar (app will fall back to OCR)"
        return 0
    fi
    if [[ -f "$sidecar" && "$sidecar" -nt "$pdf" ]]; then
        echo "skip   $sidecar (up to date)"
        return 0
    fi
    if pdftotext -layout -enc UTF-8 "$pdf" "$sidecar" 2>/dev/null; then
        echo "fetch  $sidecar  <- pdftotext layout extract"
        fetched+=("$sidecar")
    else
        echo "WARN   $sidecar  pdftotext failed; app will fall back to OCR"
    fi
}
regenerate_frcp_sidecar

# Pro Se complaint forms (USCourts.gov, public domain). USCourts.gov uses
# descriptive filenames now; the older AO/Pro-Se numbered URLs return 404.
fetch "$ASSETS_FORMS/pro_se_1_complaint.pdf" 50000 \
    "https://www.uscourts.gov/sites/default/files/complaint_for_a_civil_case.pdf" \
    "https://www.uscourts.gov/sites/default/files/forms/pro_se_1_complaint_for_a_civil_case_0.pdf" \
    "https://www.uscourts.gov/sites/default/files/pro_se_1.pdf"

fetch "$ASSETS_FORMS/pro_se_2_complaint_diversity.pdf" 50000 \
    "https://www.uscourts.gov/sites/default/files/complaint_for_a_civil_case_alleging_diversity_of_citizenship.pdf" \
    "https://www.uscourts.gov/sites/default/files/forms/pro_se_2_complaint_for_a_civil_case_alleging_diversity_of_citizenship_0.pdf" \
    "https://www.uscourts.gov/sites/default/files/pro_se_2.pdf"

fetch "$ASSETS_FORMS/pro_se_7_complaint_employment.pdf" 50000 \
    "https://www.uscourts.gov/sites/default/files/complaint_for_employment_discrimination.pdf" \
    "https://www.uscourts.gov/sites/default/files/forms/pro_se_7_complaint_for_employment_discrimination_0.pdf" \
    "https://www.uscourts.gov/sites/default/files/pro_se_7.pdf"

fetch "$ASSETS_FORMS/pro_se_14_complaint_42_1983.pdf" 50000 \
    "https://www.uscourts.gov/sites/default/files/complaint_for_violation_of_civil_rights_prisoner.pdf" \
    "https://www.uscourts.gov/sites/default/files/complaint_for_violation_of_civil_rights_prisoner_complaint.pdf" \
    "https://www.uscourts.gov/sites/default/files/forms/pro_se_14_complaint_for_violation_of_civil_rights_prisoner_0.pdf" \
    "https://www.uscourts.gov/sites/default/files/pro_se_14.pdf"

fetch "$ASSETS_FORMS/pro_se_15_complaint_bivens.pdf" 50000 \
    "https://www.uscourts.gov/sites/default/files/complaint_for_violation_of_civil_rights_non-prisoner.pdf" \
    "https://www.uscourts.gov/sites/default/files/forms/pro_se_15_complaint_for_violation_of_civil_rights_non-prisoner_0.pdf" \
    "https://www.uscourts.gov/sites/default/files/pro_se_15.pdf"

# In Forma Pauperis + habeas / 2255 forms (older AO numbered series).
fetch "$ASSETS_FORMS/ao_240_in_forma_pauperis.pdf" 30000 \
    "https://www.uscourts.gov/sites/default/files/ao240.pdf" \
    "https://www.uscourts.gov/sites/default/files/forms/ao240.pdf"

fetch "$ASSETS_FORMS/ao_241_habeas_2254.pdf" 30000 \
    "https://www.uscourts.gov/sites/default/files/AO_241_0.pdf" \
    "https://www.uscourts.gov/sites/default/files/ao241_0.pdf" \
    "https://www.uscourts.gov/sites/default/files/ao241.pdf"

fetch "$ASSETS_FORMS/ao_242_habeas_2241.pdf" 30000 \
    "https://www.uscourts.gov/sites/default/files/ao242.pdf" \
    "https://www.uscourts.gov/sites/default/files/forms/ao242.pdf"

fetch "$ASSETS_FORMS/ao_243_2255_motion.pdf" 30000 \
    "https://www.uscourts.gov/sites/default/files/ao243_0.pdf" \
    "https://www.uscourts.gov/sites/default/files/ao243.pdf"

echo
echo "Fetched: ${#fetched[@]} file(s)"
if [[ ${#skipped[@]} -gt 0 ]]; then
    echo "Skipped: ${#skipped[@]} file(s) (the app falls back to generated drafts)"
    for f in "${skipped[@]}"; do echo "  - $f"; done
fi
exit 0
