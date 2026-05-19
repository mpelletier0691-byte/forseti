# Building a Digital Case Folder on Your PC

Whether you are on Windows, macOS, or Linux, the same folder structure makes a pro se case manageable. Forseti's Notes tab is for thinking; **the case folder is for the official record**.

## The structure (copy this verbatim)

```
~/Cases/
  YYYY-MM-DD_<short-case-name>/
    00_INDEX.md                <- one-line summary of every file in this folder
    01_pleadings/              <- complaint, answer, amended pleadings
    02_motions/
       <date>_<who>_<title>.pdf
    03_discovery/
       outgoing/               <- requests you sent
       incoming/               <- responses you received
       privilege_log.xlsx
    04_evidence/
       documents/
       photos/
       audio/
       video/
       hashes.txt              <- SHA-256 of every file (chain of custody)
    05_correspondence/
       email/                  <- exported as .eml or .pdf
       letters/
       text_messages/
    06_court_orders/
    07_briefs_and_exhibits/
       motion_<id>/
          brief.pdf
          ex_A_<short-name>.pdf
          ex_B_<short-name>.pdf
    08_research/                <- saved cases, statutes, articles
    09_billing_and_costs/
    10_archive/                 <- superseded drafts; never delete
```

## Naming convention

`YYYY-MM-DD_<sender>_<short-title>.<ext>`

> Example: `2026-05-08_pl_motion-to-compel-rule-37.pdf`

Sortable, scriptable, no spaces (use hyphens).

## Backups - the 3-2-1 rule

- **3** copies of every file.
- On at least **2** different media (laptop SSD + external drive).
- **1** copy off-site (encrypted cloud or a drive at a relative's house).

For sensitive case files, prefer **end-to-end encrypted** cloud (Cryptomator + Dropbox, Tresorit, or Proton Drive) over plain Drive/iCloud.

## Tracking who has what

- Every email you send: BCC yourself, save the export to `05_correspondence/email/`.
- Every certified mail: scan the green card to `05_correspondence/letters/`.
- Every fax / e-file confirmation: PDF to the relevant folder.

## Versioning your drafts

- Use `_v1`, `_v2`, `_FINAL`, `_FILED` suffixes.
- The version that **was filed** belongs in `01_pleadings/`, `02_motions/`, etc. - never in `10_archive/`.
- The drafts before it go to `10_archive/`.

## File integrity

Run a one-time hash of your evidence so you can prove it has not been altered:

```sh
# macOS / Linux
find 04_evidence -type f -exec shasum -a 256 {} \; > 04_evidence/hashes.txt
```

```powershell
# Windows PowerShell
Get-ChildItem -Recurse 04_evidence | Get-FileHash -Algorithm SHA256 |
  Export-Csv 04_evidence\hashes.csv
```

## What to put in `00_INDEX.md`

A bullet list, one per file, with the date, sender, document title, and link. Update it every time you save a new file. Future-you will thank you when a judge asks "where exactly did you produce that?"

> Information only. Not legal advice.
