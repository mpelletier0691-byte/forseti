package com.forseti.pdf

/**
 * Curated FRCP (2024 restyled) outline used as a fallback when the bundled PDF
 * does not expose a usable bookmark tree. Page numbers are *approximate* and
 * tuned against the published 2024 PDF; the in-app search jumps a final time
 * by matching the rule heading inside the page text, so small drift is fine.
 *
 * Anchor scheme: "rule.<n>" or "rule.<n>.<sub>".
 */
object FrcpOutline {
    val entries: List<TocEntry> = listOf(
        title("title.1", "TITLE I. Scope of Rules; Form of Action", 1) {
            rule("rule.1", "Scope and Purpose", 1)
            rule("rule.2", "One Form of Action", 1)
        },
        title("title.2", "TITLE II. Commencing an Action; Service of Process, Pleadings, Motions, and Orders", 2) {
            rule("rule.3", "Commencing an Action", 2)
            rule("rule.4", "Summons", 3)
            rule("rule.5", "Serving and Filing Pleadings and Other Papers", 14)
            rule("rule.5.1", "Constitutional Challenge to a Statute", 18)
            rule("rule.5.2", "Privacy Protection For Filings Made with the Court", 19)
            rule("rule.6", "Computing and Extending Time", 21)
        },
        title("title.3", "TITLE III. Pleadings and Motions", 24) {
            rule("rule.7", "Pleadings Allowed; Form of Motions and Other Papers", 24)
            rule("rule.7.1", "Disclosure Statement", 25)
            rule("rule.8", "General Rules of Pleading", 26)
            rule("rule.9", "Pleading Special Matters", 28)
            rule("rule.10", "Form of Pleadings", 30)
            rule("rule.11", "Signing Pleadings, Motions, and Other Papers; Representations to the Court; Sanctions", 31)
            rule("rule.12", "Defenses and Objections: When and How Presented; Motion for Judgment on the Pleadings; Consolidating Motions; Waiving Defenses; Pretrial Hearing", 33)
            rule("rule.13", "Counterclaim and Crossclaim", 38)
            rule("rule.14", "Third-Party Practice", 40)
            rule("rule.15", "Amended and Supplemental Pleadings", 43)
            rule("rule.16", "Pretrial Conferences; Scheduling; Management", 46)
        },
        title("title.4", "TITLE IV. Parties", 51) {
            rule("rule.17", "Plaintiff and Defendant; Capacity; Public Officers", 51)
            rule("rule.18", "Joinder of Claims", 53)
            rule("rule.19", "Required Joinder of Parties", 53)
            rule("rule.20", "Permissive Joinder of Parties", 55)
            rule("rule.21", "Misjoinder and Nonjoinder of Parties", 55)
            rule("rule.22", "Interpleader", 56)
            rule("rule.23", "Class Actions", 57)
            rule("rule.23.1", "Derivative Actions", 64)
            rule("rule.23.2", "Actions Relating to Unincorporated Associations", 65)
            rule("rule.24", "Intervention", 66)
            rule("rule.25", "Substitution of Parties", 67)
        },
        title("title.5", "TITLE V. Disclosures and Discovery", 69) {
            rule("rule.26", "Duty to Disclose; General Provisions Governing Discovery", 69)
            rule("rule.27", "Depositions to Perpetuate Testimony", 80)
            rule("rule.28", "Persons Before Whom Depositions May Be Taken", 82)
            rule("rule.29", "Stipulations About Discovery Procedure", 82)
            rule("rule.30", "Depositions by Oral Examination", 83)
            rule("rule.31", "Depositions by Written Questions", 89)
            rule("rule.32", "Using Depositions in Court Proceedings", 91)
            rule("rule.33", "Interrogatories to Parties", 94)
            rule("rule.34", "Producing Documents, Electronically Stored Information, and Tangible Things, or Entering onto Land, for Inspection and Other Purposes", 96)
            rule("rule.35", "Physical and Mental Examinations", 99)
            rule("rule.36", "Requests for Admission", 100)
            rule("rule.37", "Failure to Make Disclosures or to Cooperate in Discovery; Sanctions", 102)
        },
        title("title.6", "TITLE VI. Trials", 110) {
            rule("rule.38", "Right to a Jury Trial; Demand", 110)
            rule("rule.39", "Trial by Jury or by the Court", 111)
            rule("rule.40", "Scheduling Cases for Trial", 112)
            rule("rule.41", "Dismissal of Actions", 112)
            rule("rule.42", "Consolidation; Separate Trials", 114)
            rule("rule.43", "Taking Testimony", 114)
            rule("rule.44", "Proving an Official Record", 115)
            rule("rule.44.1", "Determining Foreign Law", 117)
            rule("rule.45", "Subpoena", 117)
            rule("rule.46", "Objecting to a Ruling or Order", 124)
            rule("rule.47", "Selecting Jurors", 124)
            rule("rule.48", "Number of Jurors; Verdict; Polling", 125)
            rule("rule.49", "Special Verdict; General Verdict and Questions", 125)
            rule("rule.50", "Judgment as a Matter of Law in a Jury Trial; Related Motion for a New Trial; Conditional Ruling", 126)
            rule("rule.51", "Instructions to the Jury; Objections; Preserving a Claim of Error", 128)
            rule("rule.52", "Findings and Conclusions by the Court; Judgment on Partial Findings", 129)
            rule("rule.53", "Masters", 130)
        },
        title("title.7", "TITLE VII. Judgment", 135) {
            rule("rule.54", "Judgment; Costs", 135)
            rule("rule.55", "Default; Default Judgment", 137)
            rule("rule.56", "Summary Judgment", 138)
            rule("rule.57", "Declaratory Judgment", 141)
            rule("rule.58", "Entering Judgment", 141)
            rule("rule.59", "New Trial; Altering or Amending a Judgment", 142)
            rule("rule.60", "Relief from a Judgment or Order", 143)
            rule("rule.61", "Harmless Error", 145)
            rule("rule.62", "Stay of Proceedings to Enforce a Judgment", 145)
            rule("rule.62.1", "Indicative Ruling on a Motion for Relief That Is Barred by a Pending Appeal", 147)
            rule("rule.63", "Judge's Inability to Proceed", 147)
        },
        title("title.8", "TITLE VIII. Provisional and Final Remedies", 148) {
            rule("rule.64", "Seizing a Person or Property", 148)
            rule("rule.65", "Injunctions and Restraining Orders", 148)
            rule("rule.65.1", "Proceedings Against a Security Provider", 151)
            rule("rule.66", "Receivers", 151)
            rule("rule.67", "Deposit into Court", 152)
            rule("rule.68", "Offer of Judgment", 152)
            rule("rule.69", "Execution", 153)
            rule("rule.70", "Enforcing a Judgment for a Specific Act", 154)
            rule("rule.71", "Enforcing Relief For or Against a Nonparty", 154)
        },
        title("title.9", "TITLE IX. Special Proceedings", 155) {
            rule("rule.71.1", "Condemning Real or Personal Property", 155)
            rule("rule.72", "Magistrate Judges: Pretrial Order", 159)
            rule("rule.73", "Magistrate Judges: Trial by Consent; Appeal", 160)
        },
        title("title.10", "TITLE X. District Courts and Clerks: Conducting Business; Issuing Orders", 162) {
            rule("rule.77", "Conducting Business; Clerk's Authority; Notice of an Order or Judgment", 162)
            rule("rule.78", "Hearing Motions; Submission on Briefs", 163)
            rule("rule.79", "Records Kept by the Clerk", 163)
            rule("rule.80", "Stenographic Transcript as Evidence", 164)
        },
        title("title.11", "TITLE XI. General Provisions", 165) {
            rule("rule.81", "Applicability of the Rules in General; Removed Actions", 165)
            rule("rule.82", "Jurisdiction and Venue Unaffected", 166)
            rule("rule.83", "Rules by District Courts; Judge's Directives", 166)
            rule("rule.84", "Forms (Abrogated)", 167)
            rule("rule.85", "Title", 167)
            rule("rule.86", "Effective Dates", 167)
        }
    )

    private class TitleBuilder(val anchor: String, val name: String, val page: Int) {
        val rules = mutableListOf<TocEntry>()
        fun rule(anchor: String, name: String, page: Int) {
            rules += TocEntry(anchor = anchor, title = name, page = page, depth = 1)
        }
    }

    private fun title(anchor: String, name: String, page: Int, body: TitleBuilder.() -> Unit): TocEntry {
        val b = TitleBuilder(anchor, name, page).apply(body)
        return TocEntry(anchor = anchor, title = name, page = page, depth = 0, children = b.rules)
    }
}
