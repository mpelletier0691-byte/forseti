# Forseti Shortcuts and Safe-Workflow Cheat Sheet

This guide is the fastest path through the app. It only describes things Forseti
actually does today — no promises, no roadmap. If a step here ever stops working,
file an issue.

> **Forseti philosophy.** AI might be powerful, but nothing is more powerful than
> the human spirit and drive — let this help guide you in your saga. We don't
> push answers; we organize procedure so you can show up prepared.

---

## 1. Get a PDF *into* the app

You have four equally fast paths. Pick whichever fits the moment:

1. **Document Scanner tab** — best when you have a paper page in your hand.
   Tap **Capture page** for each page, then **Save PDF to case**. Forseti
   auto-files the result into the matching phase folder if you put a keyword
   like `motion`, `discovery`, `answer`, or `order` in the label field.
2. **Share into Forseti** — open any PDF in your phone (Files app, Drive,
   email attachment, scanner app) and pick the system **Share** button →
   choose **Forseti**. The app opens a chooser asking whether to file it as
   an Uploaded Rule or into a case workspace.
3. **Uploaded Rules tab → Import PDF** — for court-rule PDFs that Forseti
   couldn't pull on its own (state site down, paywalled HTML, your own
   annotated copy). These never get auto-filed into a case.
4. **Case Profile → open a case → Import file button** inside any subfolder
   of the workspace. Useful when you already know exactly which folder the
   document belongs in.

> **Why share-into-app is great:** the PDF stays app-private to Forseti.
> You're not making a second copy in your camera roll or downloads folder.

---

## 2. Auto-filing (how Forseti decides where things go)

When you save a scan or a shared PDF into a case, Forseti looks at the label
or filename for keywords and routes the file to the matching phase folder:

| Keyword in label/filename | Lands in |
| --- | --- |
| `complaint` | `01_Pleadings/Drafts/` |
| `answer` | `02_Answer/Drafts/` |
| `motion`, `sanction` | `03_Motions/Drafts/` |
| `discovery`, `interrog`, `deposition`, `subpoena`, `rfp`, `rfa` | `04_Discovery/Drafts/` |
| `pretrial`, `trial` | `05_Trial/Drafts/` |
| `judgment`, `appeal` | `06_PostTrial/Drafts/` |
| `order` | `07_General/Drafts/` |
| `exhibit` | `<phase>/Exhibits/` if available |
| anything else | `98_Scans/` (you can move it later) |

You can always override by opening **Case Profile → your case** and dragging
the file with the rename / share / delete row.

> **Uploaded rule PDFs are never auto-filed into a case.** They live in the
> Uploaded Rules tab until you explicitly share them out.

---

## 3. Fast capture habits that save you headaches

- **Keep the page flat.** The scanner uses the rear camera; warped pages
  produce warped PDFs.
- **Use the label field every time.** Three seconds of typing now means
  you'll find the file later.
- **Capture the cover sheet last** so it ends up first when stacked into a PDF.
- **For long documents, capture in batches of 10 pages or fewer.** If you
  back out mid-capture, only what you've already saved is committed.
- **Trust the camera over your phone's stock document scanner** if you want
  the file to land directly in the case workspace — Forseti only auto-files
  what it captures or what you share into it.

---

## 4. While you're reading rules

The **Quick Jump** tab is where you spend most of your reading time. Hidden
shortcuts:

- The **menu-book icon** opens the Quick Jump search panel — type a rule
  number ("12(b)(6)", "26", "56(a)") to jump straight to it.
- The **sticky-note icon** (next to it) drops a quick notepad on top of
  the page. Type, hit **Save** to keep it (it shows up later in the Notes
  tab anchored to the current page), or **Done** to discard.
- The **bookmark icon** stars the page you're on so it shows up in the
  Notes tab → Bookmarks list. Tap it again to un-star.
- The **microphone-style "Read" icon** runs on-device OCR over the current
  page and reads it aloud through whichever Android voice you've enabled
  under *Settings → Accessibility → Text-to-speech output*. Tap **Stop** to
  cut it off. If the system reports no voice, the app shows a one-tap
  shortcut to the right settings page (or to install Google's TTS engine).
- Pinch-zoom on the PDF; drag with one finger to scroll.
- Page counter in the bottom-right always shows where you are.

### Long-press for copy/paste

In the **Guides**, **Notes**, and **Case file viewer** for text/markdown
files, long-press to drag the standard Android selection handles. Then
**Copy** lands the selection on the system clipboard so you can paste into
your draft, an email, or a chat. PDFs are images at the page level, so the
copy gesture won't grab raw words from those — use **Read aloud** plus a
quick handwritten note for that case.

---

## 5. Drafts and signatures

The **Motion Drafts** tab generates editable PDFs from bundled templates —
shells for FRCP-style motions and answers. Forseti never signs for you;
print the draft, sign it by hand, then capture it back into the case via
the Document Scanner so the signed copy lives next to the unsigned draft.

---

## 6. Backups

The **Backup** tab produces a single ZIP of your entire case workspace.
Save it to Drive, email it to yourself, or copy it to a thumb drive.
Restoring on a fresh install of Forseti puts everything back in place.

---

## 7. Knowing the limits

Forseti is **information only — not legal advice**. The app:

- bundles the public-domain Federal Rules of Civil Procedure (Dec. 1, 2024 ed.)
- links to each state's official judiciary or legislature site for state rules
- caches PDFs you choose to download for offline use
- never sends your case data anywhere; everything stays on this device

If a state link 404s, that's the state's site changing — try the **References**
tab for the most stable parent URL, or import your own PDF via **Uploaded Rules**.

---

## 8. The workflow that wolves use

1. Open the **Case Profile** tab → create the case.
2. Open the **Deadlines** tab → log every date the court gave you.
3. Open the **Document Scanner** → capture the order, label it `order`.
4. Open the **Drafts** tab → generate your responsive motion shell.
5. Open the **Quick Jump** tab → re-read the rule the order cited.
6. Hit the **sticky-note icon** → jot what you'll argue.
7. Edit the draft in any PDF app; print, sign, scan back in.
8. Open the **Backup** tab → ZIP the case folder before you walk into court.

That's the loop. Repeat for every motion, every hearing, every appeal.
The system was designed to grind people down through procedure — Forseti
keeps the procedure visible so you can stand on equal footing.
