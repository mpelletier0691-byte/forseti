# Bundled Rules PDFs

`frcp_2024.pdf` should be the official restyled Federal Rules of Civil Procedure
PDF, sourced from the U.S. Courts and bundled at build time.

## Source

```
https://www.uscourts.gov/sites/default/files/federal_rules_of_civil_procedure_-_dec_1_2024_0.pdf
```

This file is a federal government work and is in the public domain
(17 U.S.C. Sec. 105).

## Build-time fetch

Run the helper from the repo root before building a release:

```sh
scripts/fetch_assets.sh
```

That script writes the file here at `app/src/main/assets/rules/frcp_2024.pdf`.

## Why we don't commit it

The PDF is several MB and changes whenever the Supreme Court adopts amendments.
Keeping it out of git makes diffs reasonable; the helper script makes sure
debug builds always have a current copy.

## Sidecar text file

`frcp_2024.pages.txt` is a UTF-8 dump of the PDF's text layer, produced by
`pdftotext -layout`. Pages are separated by form-feed (`\f`, U+000C).

The Quick Jump tab loads this sidecar at startup so reading aloud, copying, and
the long-press "page text" dialog can serve real PDF text instantly instead of
running on-device OCR (which is slow and unreliable on dense legal type). The
app gracefully falls back to OCR if the sidecar is absent.

`scripts/fetch_assets.sh` regenerates the sidecar any time the PDF is newer
than it (and skips silently if `pdftotext` isn't installed).
