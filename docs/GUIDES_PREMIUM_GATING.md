# Forseti guides — free vs purchase-gated (cloud agent briefing)

**Status:** Product decision confirmed (2026-07-16). Implementation may still be pending — check `GuideMeta` for a `premium` field and `GuidesScreen` for lock UI before assuming it ships in code.

**Owner intent:** Model **B** — trial users get **Forseti how-to / feature guides only**. Procedure and court-content guides require **`forseti_unlock`** purchase. **All new guides default to premium** unless explicitly marked free.

Related: billing is still all-or-nothing for the **app shell** (`TrialGate`). After trial expires, the whole app locks until purchase/restore. Per-guide gating applies **during trial** (and for any future free-tier redesign).

---

## Entitlement behavior (target)

| State | Free guides | Premium guides |
|-------|-------------|----------------|
| Trial / TrialEndingSoon | Open | Locked (show Buy + Restore) |
| Purchased | Open | Open |
| Expired | App locked (current `TrialExpiredScreen`) | Same |

Product ID: `forseti_unlock` (one-time non-consumable).

---

## Unlocked guides (trial + purchased)

These explain **how to use Forseti** or its features. Keep `premium: false` (or omit the field).

| id | Title |
|----|--------|
| `whats_new` | What's New in This Version |
| `forseti_shortcuts` | Forseti Shortcuts and Safe Workflow |
| `using_forseti_as_pro_se` | Using Forseti as a Pro Se Litigant — and Knowing When to Call a Lawyer |
| `drafts_federal_vs_local` | Drafts Tab: Federal Rules vs. State and Local Rules |
| `case_ingestion` | Ingesting an Existing Case Into Forseti |
| `ocr_capture_workflow` | Using the Camera Capture (OCR) Workflow |
| `digital_case_folder` | Building a Digital Case Folder on Your PC |

**Also free via Help (do not gate):** Settings → Help → What's New / `WhatsNewGuideDialog` / first-run What's New overlay for id `whats_new`.

---

## Locked guides (purchase only)

Visible in the Guides list during trial with a lock affordance; body opens only when `Entitlement.Purchased` (or equivalent `billing.isPurchased`).

| id | Title |
|----|--------|
| `family_court_organization` | Staying Organized in Family Court |
| `filing_complaint` | Filing a Civil Complaint |
| `responding_to_motion` | Responding to a Motion |
| `discovery_basics` | Discovery Basics |
| `summary_judgment` | Surviving (or Bringing) Summary Judgment |
| `hearing_what_to_expect` | Your First Hearing: What to Expect |
| `service_of_process` | Service of Process Without Mistakes |
| `computing_time` | Computing Time Under Rule 6 |
| `common_mistakes` | 10 Mistakes That Get Pro Se Cases Dismissed |
| `rule_12_motions` | Rule 12 Motions, Briefly |
| `discovery_checklist` | Discovery Requests Checklist |
| `exhibits_and_binders` | Exhibits and Trial Binders |
| `service_hygiene` | Service and Subpoena Hygiene |

**Rule:** Any **new** guide added to `00_index.json` is **`premium: true`** unless the owner explicitly requests it as a Forseti how-to/feature guide (then add it to the unlocked table above and update this doc).

---

## Suggested future premium guides (not yet written)

Modeled on existing procedure guides; all would be purchase-only:

1. Amending Your Complaint (Rule 15) — after `filing_complaint`
2. Default and Setting Aside Default (Rule 55 / 60) — after filing + service
3. Initial Disclosures and Scheduling (Rule 26 / 16) — after `discovery_basics`
4. Protective Orders and Objections in Discovery — after `discovery_checklist`
5. Motions in Limine and Pretrial Filings — after exhibits / hearing
6. Appeals Basics for Pro Se — after `common_mistakes`
7. Settlement, Stipulations, and Voluntary Dismissal (Rule 41)
8. Working With Local Rules and Standing Orders — deeper than `drafts_federal_vs_local`
9. Evidence Basics for Pro Se (authentication, hearsay traps) — after exhibits
10. In Forma Pauperis and Fee Waivers (AO 240) — after `filing_complaint`

---

## Implementation checklist (for agents)

When implementing or extending:

1. **Schema** — `GuideMeta` add `premium: Boolean = false`. Indexes already use `ignoreUnknownKeys`.
2. **Assets** — Set `"premium": true` on locked ids in:
   - `app/src/main/assets/guides/00_index.json`
   - `app/src/main/assets/guides-es/00_index.json`
   - `app/src/main/assets/guides-zh-rCN/00_index.json`  
   Keep the same `premium` flags across locales.
3. **UI** — `GuidesScreen`: lock icon on premium cards; on tap if not purchased → dialog with Buy + Restore (`LocalBilling` / `LocalEntitlement`). Do not load guide body until unlocked.
4. **Gate rule** — Premium open **only** when purchased (not during trial). Free guides open during trial.
5. **Copy** — Adjust trial / marketing strings that claim “All features are unlocked” if guides are partly locked.
6. **New guides** — Match style of `filing_complaint.md` / `family_court_organization.md`: philosophy quote, plain English, FRCP-oriented, Forseti tool tips where accurate, “not legal advice,” register in all three indexes with `premium: true`.
7. **Do not** put secrets or AABs in git. Do not change legal routing / Brokkr Forge filing logic unless asked.

---

## Files to touch

| Area | Paths |
|------|--------|
| Meta | `app/src/main/java/com/forseti/guides/Guides.kt` |
| UI | `app/src/main/java/com/forseti/ui/screens/GuidesScreen.kt` |
| Indexes | `app/src/main/assets/guides*/00_index.json` |
| Bodies | `app/src/main/assets/guides/*.md` (+ es/zh when translated) |
| Strings | `app/src/main/res/values/strings*.xml` (lock / buy copy) |
| Billing reference | `BillingService`, `EntitlementManager`, `TrialUi` / `TrialBanner` |

---

## Capability accuracy (when writing guides)

Do **not** claim features Forseti lacks. Known constraints:

- No dedicated timeline builder — use Deadlines + dated filenames + notes.
- No arbitrary PDF merge/split beyond multi-page Scanner → one PDF.
- Brokkr Forge folder tree is federal-civil oriented; family/state courts vary.
- Organization tool, not legal advice / not a substitute for an attorney.

See also: `docs/FORSETI_PROJECT_BRIEF.md`, `publish/Google Play/WHAT_IS_IN_VC20.md`.
