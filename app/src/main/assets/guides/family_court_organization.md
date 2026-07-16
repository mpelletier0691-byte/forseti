# Staying Organized in Family Court

A practical, procedural guide for using Forseti to manage documents, dates, and evidence when you are handling a **state family-court** case on your own.

> **Forseti philosophy.** AI might be powerful, but nothing is more powerful than the human spirit and drive — let this help guide you in your saga. Forseti organizes procedure and paperwork; it does not replace a lawyer.

Family court relies heavily on documentation, timelines you can prove, and clear records. Forseti helps you keep materials structured, readable, and easy to retrieve on your phone. This guide explains how to use Forseti’s **real tools** for the procedural side of your case — not legal strategy.

---

## 1. Overview

Family-court cases often involve school records, medical notes, communication logs, photos, and official notices spread across email, text screenshots, and paper folders. Forseti gives you one **Case Profile** workspace per matter with a consistent folder tree (the **Brokkr Forge layout**), on-device sorting, deadlines with reminders, and a built-in file viewer.

**Start here:**

1. Create a case in **Case Profile** (title, court, case number, your role).
2. Import your existing files with **Brokkr Forge Process** or add scans from the **Document Scanner** tab.
3. Track hearing and filing dates in the **Deadlines** tab.
4. Use **State Rules** and **Uploaded Rules** for your jurisdiction’s family-court procedures.

---

## 2. Organizing family-court documents

Forseti stores everything in an app-private case workspace on your device. Nothing is uploaded to the cloud unless **you** tap Share on a specific file.

### How Forseti helps

| What you have | Where it usually lands | Forseti tool |
| --- | --- | --- |
| School reports, IEPs, report cards | `04_Evidence/PDFs/` or `01_Pleadings/` | **Brokkr Forge Process** + rename if needed |
| Medical notes, therapy records | `04_Evidence/PDFs/` | Same; path/filename hints improve sorting |
| Text/email screenshots | `04_Evidence/Screenshots/` or `06_Correspondence/` | **Image Ingestion** or folder ingest |
| Court orders, notices, motions | `01_Pleadings/Orders/` or `05_Motions/` | OCR reads PDFs; **File Here** chip if unsure |
| Photos (home, visits, injuries) | `04_Evidence/Photos/` | Scanner or image ingest |
| Unsorted imports | `99_Inbox/` | Review after **Brokkr Forge complete** notification |

**Brokkr Forge Process** walks your folder tree, samples PDF pages with on-device OCR, and auto-files documents when confidence is high (85%+). Lower-confidence items stay in **99_Inbox** with an optional **File Here →** suggestion (70–84%). See the guide *Ingesting an Existing Case Into Forseti* for background ingest, timing, and the audit log.

**Duplicate detection** skips files already in your workspace or picked twice in one batch.

---

## 3. Preparing exhibits

Courts expect exhibits to be clear, complete, and easy to review. Forseti helps you **prepare and organize** files; it does not file them with the clerk for you.

### Tools you can use today

| Task | How in Forseti |
| --- | --- |
| Paper → PDF | **Document Scanner** tab: capture multiple pages, save one PDF into the active case (`98_Scans/` or routed folder) |
| Consistent names | **Case Profile → open file → Rename**; Forseti offers to move the file after rename |
| Exhibit home | `08_Exhibits/` (Labels, Final_Exhibits) — drag or use **File Here** after ingest |
| Share a set | **Share** on any file (Android share sheet) to email, Drive, or print apps |
| Working copies | Keep drafts in `08_Exhibits/`; see *Exhibits and Trial Binders* for numbering conventions |

**What Forseti does not do:** there is no built-in “split PDF” or “merge arbitrary PDFs” tool beyond **multi-page camera capture into one PDF**. Use your court’s preferred app for heavy PDF surgery, then import the result with **Image Ingestion** or **Brokkr Forge Process**.

Always confirm **your judge’s exhibit format** (numbered vs. lettered, separate packet vs. embedded) in local rules or with an attorney.

---

## 4. Tracking dates and building a chronology

Family court turns on **when** things happened — school incidents, medical appointments, communication bursts, orders entered.

### Deadlines tab (hearings, filings, reminders)

Use **Deadlines** to record:

- Next hearing date and time  
- Mediation or parenting-class deadlines  
- Dates to file a response or update financial disclosures  
- Any date the court ordered you to “return” or “appear”

Forseti can remind you with a phone notification when a deadline’s notify time arrives (allow notifications when prompted).

Matching subfolders appear under `07_Deadlines/` in your case workspace.

### Chronology (timeline on paper)

Forseti does **not** include a separate “timeline builder” screen. To build a chronology attorneys can follow:

1. Create a simple list in `00_Case_Overview/Notes/` (text or markdown file), **or** use the **Notes** tab with bookmarks tied to key rule pages.
2. Name files with **`YYYY-MM-DD_description`** before ingest so Brokkr Forge sorts them in rough date order inside each folder.
3. Export or share the overview file before a consultation or hearing.

---

## 5. Managing communication records

Family court often reviews communication patterns — texts, emails, co-parenting app logs, and school messages.

**Practical workflow:**

1. Export or screenshot messages **with dates visible** when possible.
2. Rename before ingest: `2024-03-12_text_thread_school_pickup.png`.
3. Let Brokkr Forge route to `06_Correspondence/` or `04_Evidence/Screenshots/`.
4. After ingest, open **99_Inbox** and fix anything mislabeled with **File Here** or manual move.

For ongoing messages, add new batches with **Image Ingestion** as they arrive instead of re-importing the entire history.

---

## 6. Avoiding common document issues

| Problem | Forseti helps by… |
| --- | --- |
| Lost files | One case workspace; `00_INDEX.txt` activity log |
| Mystery PDFs | Built-in viewer + **Read aloud** on PDFs |
| Wrong folder | **INGEST_AUDIT_latest.txt** after bulk ingest; move/rename anytime |
| Duplicates | Skip on ingest; **Delete all** in Inbox when re-testing imports |
| Missed dates | **Deadlines** tab + optional notifications |
| Scattered photos | Scanner → PDF; Evidence folders |

**Procedural mistakes** (wrong form, missed filing window, improper service) still require **state rules** and usually an attorney. Forseti does not validate that a filing meets your county’s requirements.

---

## 7. What Forseti cannot do

Forseti is a tool for **organization and preparation**. It does **not**:

- Tell you what to file or when to file it  
- Provide legal strategy or predict outcomes  
- Replace an attorney licensed in your state  
- Interpret family-code statutes, custody standards, or best-interest factors for your situation  
- Guarantee that auto-sorted folders match your judge’s exact packet layout  

**Not legal advice.** Always verify forms, deadlines, and local procedures with official sources and qualified counsel.

---

## 8. Why Forseti is helpful in family court

Family court is document-driven. Forseti helps you:

- Keep records in one searchable workspace on your phone  
- Prepare exhibits and scans in PDF form  
- Track hearing and filing dates with reminders  
- Store communication screenshots where you can find them quickly  
- Arrive at mediation or consultation with an organized folder instead of a camera roll  

It reduces clutter and stress; it does not remove the need to follow your court’s rules.

---

## 9. State differences and working with attorneys

Family-court procedures **vary widely by state** — and often by county. Filing rules, required forms, custody evaluations, and exhibit expectations differ. **Consult a licensed attorney in your state** before major decisions.

Reading your state’s court rules ahead of time improves communication with counsel. When you understand motions, filings, and deadlines at a basic level, you can follow an attorney’s guidance more effectively.

**Bring an organized workspace to consultations:**

- Use **Backup** to ZIP your case workspace and share it before the meeting  
- Skim `00_Case_Overview/` and `INGEST_AUDIT_latest.txt` so you know what was auto-filed vs. manually sorted  

Many attorneys offer **limited-scope** (unbundled) help for one hearing or one motion — see *Using Forseti as a Pro Se Litigant* for referral ideas.

---

## 10. Federal vs. state courts: why folder structures differ

Forseti’s default **Brokkr Forge** tree mirrors **federal civil** procedure (FRCP-oriented folders: Pleadings, Discovery, Motions, Trial, etc.). That structure is intentional: federal district courts share nationwide conventions, and Forseti’s **Quick Jump** and **Drafts** tabs are built around the **Federal Rules of Civil Procedure**.

**Family court is state court.** Your jurisdiction may require:

- Different form titles (petition vs. complaint, parenting plan, financial affidavit)  
- Numbered or lettered exhibits, or court-specific cover sheets  
- Separate “evidence packets” or guardian ad litem submissions  
- Local rules that override anything in a generic folder name  

Some family materials still map cleanly:

| Family-court material | Suggested Brokkr Forge home |
| --- | --- |
| Petitions, orders, judgments | `01_Pleadings/` |
| Motions and responses | `05_Motions/` |
| School/medical records | `04_Evidence/` |
| Text/email screenshots | `04_Evidence/Screenshots/` or `06_Correspondence/` |
| Exhibit binders | `08_Exhibits/Final_Exhibits/` |
| Hearing notices | `09_Hearings/Notices/` |

If a folder name does not match local practice, **use it anyway as your private organizing system**, then rename or export files to whatever format your clerk or attorney requires. You can move files freely inside Case Profile; auto-file is never permanent.

For **state rule text**, open the **State Rules** tab. For **local family-court forms and standing orders**, import PDFs into **Uploaded Rules** or ingest them into `00_Case_Overview/`.

---

## Quick checklist before a hearing

1. **Deadlines** tab — next hearing date entered with reminder  
2. **08_Exhibits/** — working copies labeled and named consistently  
3. **99_Inbox/** — empty or triaged  
4. **Share** or **Backup** — copy ready if the court or GAL requests a packet  
5. **Attorney or self-help center** — confirm you meet **state and local** requirements  

---

> **Forseti philosophy:** AI might be powerful, but nothing is more powerful than the human spirit and drive — let this help guide you in your saga.
