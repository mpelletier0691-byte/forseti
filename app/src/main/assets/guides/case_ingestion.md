# Ingesting an Existing Case Into Forseti

So you already have months of paperwork, screenshots, and PDFs scattered across your phone, drive, and a couple of email folders. Forseti is built to absorb that mess and shelve it into a clean, repeatable structure.

This guide walks you through the **Case Profile ingestion buttons** (folder ingest + image ingest), explains the **Brokkr Forge case folder layout**, and teaches you the file-naming habits that make auto-routing accurate.

---

## 1. The Brokkr Forge layout (what every case looks like)

Every case Forseti creates uses the same eleven-folder skeleton. You never have to invent it — Forseti generates it the moment you save a new case. The numbering keeps the folders in court order regardless of which file manager you use.

```
Case_001_<your title>/
├── 00_Case_Overview/        Notes, parties list, the case index
├── 01_Pleadings/            Complaint, Answer, Motions, Orders
├── 02_Service_of_Process/   Proof_of_Service, Summons, Correspondence
├── 03_Discovery/            Interrogatories, Requests_for_Production,
│                            Admissions, Depositions, Discovery_Responses
├── 04_Evidence/             Photos, PDFs, Screenshots, Audio, Video
├── 05_Motions/              Drafts, Filed, Court_Responses
├── 06_Correspondence/       Opposing_Party, Court, Misc
├── 07_Deadlines/            Calendar entries + Completed/
├── 08_Exhibits/             Labels and Final_Exhibits
├── 09_Hearings/             Notices, Prep, Outcomes
├── 10_Trial/                Trial_Brief, Witness_List, Jury_Instructions, Final_Binder
├── 98_Scans/                Camera scanner inbox (no keyword matched)
└── 99_Inbox/                Auto-router put it here because it couldn’t classify
```

Anything that lands in **99_Inbox/** is yours to drag into the right place from **Case Profile → Open case**.

---

## 2. Two ingestion buttons

Open Case Profile, edit (or create) the case you want to populate, scroll under **Complaint filed**:

| Button | Use when… |
| --- | --- |
| **Brokkr Forge Process** | You already have a folder on your phone, on a USB-OTG stick, or on Google Drive that holds dozens of mixed files for this case. |
| **Image Ingestion** | You want to grab specific photos, screenshots, or short PDFs without dragging in everything around them. |

Both buttons funnel through the **same auto-router**, so the only difference is *what you pick*: a whole tree vs. a hand-curated list.

### 2.1 Folder ingest (background)

Tapping **Brokkr Forge Process** opens Android’s SAF directory picker. Select the case folder, hit *Use this folder*, and Forseti will:

1. Start sorting **in the background** — you do **not** need to keep the app open.
2. Show a snackbar: *Brokkr Forge started — you’ll get a notification when sorting finishes.*
3. Display an ongoing notification while work runs: **Brokkr Forge sorting…**
4. Walk every file in the tree (subfolders included), using **filename + relative path** plus **multi-page OCR sampling** on PDFs.
5. Drop each file into the correct Brokkr Forge folder (or **99_Inbox** when confidence is low).
6. Log every move to `00_INDEX.txt` and write **`INGEST_AUDIT_latest.txt`** at the case root.
7. Send a completion notification: **Brokkr Forge complete, head back to forseti to finalize your case.**

When you return, open **Case Profile → your case** and skim **99_Inbox/**. Move stragglers or tap **File Here** on suggested items.

> **Use SAF, not adb push.** On emulators and devices, always pick the folder through **Brokkr Forge Process**. Pushing files with `adb` bypasses Storage Access Framework permissions and does not match how real users import. If testing large folders on an emulator, wipe app data or free storage first.

### 2.2 Image ingest

Tapping **Image Ingestion** opens the system file picker filtered to images, PDFs, audio, and video. Multi-select what you want, hit *Open*.

- **Two or more files** → same background workflow as folder ingest (notification when done).
- **One file** → finishes inline with an immediate snackbar summary.

Useful when you have a thousand photos in your gallery but only need the seven that show the broken stair from May.

---

## 3. How long does ingest take?

Sorting is **on-device** and **slow by design**: Forseti samples multiple PDF pages with OCR and runs late-binding classification rather than trusting extensions alone.

| Approx. files | Typical time (mid-range phone) | Tip |
|---------------|-------------------------------|-----|
| Under 50 | A few minutes | Fine in one batch |
| ~300 (organized reference) | **15–30 minutes** | Leave the app; wait for the notification |
| 1,300+ (full corpus) | **Hours** | Split into smaller folders (Pleadings, Discovery, etc.) |

From a real ~317-file organized reference import, expect roughly: Pleadings 17, Motions 5, Correspondence 5, Evidence 1, Exhibits 1, Hearings 1, Deadlines 1, Case Overview 1, Inbox 6, plus other Brokkr-Forge folders for the rest.

---

## 4. Make the auto-router smart by renaming

The router decides where a file lives by looking at its **filename and folder path** first, then **OCR text** from sampled PDF pages.

The fastest way to dramatically improve accuracy is to rename files **before** ingesting. Two examples:

| Bad name | Good name | Where it lands |
| --- | --- | --- |
| `IMG_20240312_103144.jpg` | `2024-03-12_broken_stair_evidence.jpg` | `04_Evidence/Photos/` |
| `Document (3).pdf` | `2024-04-01_motion_to_dismiss.pdf` | `05_Motions/Drafts/` |
| `Screenshot_20240419_181203.png` | `2024-04-19_screenshot_text_threats.png` | `04_Evidence/Screenshots/` |
| `scan001.pdf` | `2024-05-08_proof_of_service_summons.pdf` | `02_Service_of_Process/Proof_of_Service/` |

You don’t have to rename everything — Forseti will catch keywords like `complaint`, `answer`, `motion`, `discovery`, `deposition`, `interrog`, `subpoena`, `service`, `summons`, `exhibit`, `hearing`, `witness`, `trial brief`, `order`, `judgment`. Use the table above as a cheat sheet and the rest will fall into `99_Inbox/` for you to triage.

---

## 5. Best workflow for novices

1. Create the case in **Case Profile** with at least the title, court, and case number.
2. Go through your phone’s gallery and **rename the screenshots / photos** that matter — five minutes well spent.
3. From the **Edit case** dialog, tap **Brokkr Forge Process** and point Forseti at the messy source folder via the system picker.
4. Leave the app if you want — wait for the **Brokkr Forge complete** notification.
5. Open the case and skim **99_Inbox/**. Move stragglers into the right Brokkr Forge folder.
6. Set deadlines from the **Deadlines** tab — Forseti will create matching subfolders inside `07_Deadlines/`.
7. Use **File → Share** from any email or PDF reader and pick **Forseti → Case workspace** to keep adding documents over time.

---

## 6. Open, rename, and re-route from inside the case

Once a file is in the case workspace you don’t have to leave Forseti to work with it.

- **Tap any file** in **Case Profile → your case** to open it in the built-in viewer:
  - PDFs open in the same pinch-zoom reader you use for the FRCP, with a **Read aloud** button that speaks the page using your device’s system voice.
  - Images open in a pinch-zoom preview.
  - Text/markdown files render with selectable text — long-press to copy.
  - Any other format gets an **Open in another app** shortcut.
- **Rename a file** with the pencil icon. After every rename Forseti pops a *Move to a different folder?* prompt — pick the new home in one tap, or hit **Keep here** to leave it where it is. This is how you fix routing mistakes after you’ve given a file a clearer name.
- **Share** sends the file out via Android’s share sheet (FileProvider-backed, so other apps get a temporary read URL — your private folder stays private).
- **Delete** removes the file from the case workspace permanently.

> **Tip.** A scanned receipt called `scan001.pdf` will land in `98_Scans/`. Tap it once to look at it, hit **Rename** → `2024-08-14_receipt_repair_invoice.pdf`, then choose `04_Evidence/PDFs/` from the move prompt. Done in 10 seconds.

---

## 7. Privacy

Every file ingested is copied into Forseti’s own app-private storage under `Android/data/com.forseti/files/case_workspace/`. Nothing is uploaded; nothing is shared until you tap **Share** on a specific file. Auto Backup will only restore your case data on the same Google account when you reinstall.

---

> **Forseti philosophy:** AI might be powerful, but nothing is more powerful than the human spirit and drive — let this help guide you in your saga.
