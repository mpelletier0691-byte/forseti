package com.forseti.drafts

import android.content.Context
import com.forseti.R

/**
 * The two flavors of draft document the app surfaces:
 *  - Bundled: official PDF in assets/forms/
 *  - Generated: rendered at print time by [DraftGenerator]
 */
sealed interface DraftSource {
    data class Bundled(val assetPath: String) : DraftSource
    data class Generated(val templateId: String) : DraftSource
}

/**
 * Motion draft catalog. Titles and descriptions are localized; bundled PDF paths
 * and generated template ids stay the same across languages.
 */
data class DraftCategory(val title: String)

data class DraftDoc(
    val id: String,
    val title: String,
    val ruleCitation: String,
    val description: String,
    val category: DraftCategory,
    val source: DraftSource
)

object DraftCatalog {

    fun all(context: Context): List<DraftDoc> {
        val pleadings = DraftCategory(context.getString(R.string.draft_cat_pleadings))
        val discovery = DraftCategory(context.getString(R.string.draft_cat_discovery))
        val motions = DraftCategory(context.getString(R.string.draft_cat_motions))
        val disclosures = DraftCategory(context.getString(R.string.draft_cat_disclosures))
        val service = DraftCategory(context.getString(R.string.draft_cat_service))
        val judgment = DraftCategory(context.getString(R.string.draft_cat_judgment))
        val fees = DraftCategory(context.getString(R.string.draft_cat_fees))

        return listOf(
            doc(context, "pro_se_1_complaint", pleadings, DraftSource.Bundled("forms/pro_se_1_complaint.pdf")),
            doc(context, "pro_se_2_diversity", pleadings, DraftSource.Bundled("forms/pro_se_2_complaint_diversity.pdf")),
            doc(context, "pro_se_7_employment", pleadings, DraftSource.Bundled("forms/pro_se_7_complaint_employment.pdf")),
            doc(context, "pro_se_14_1983", pleadings, DraftSource.Bundled("forms/pro_se_14_complaint_42_1983.pdf")),
            doc(context, "pro_se_15_bivens", pleadings, DraftSource.Bundled("forms/pro_se_15_complaint_bivens.pdf")),
            doc(context, "ao_240_ifp", fees, DraftSource.Bundled("forms/ao_240_in_forma_pauperis.pdf")),
            doc(context, "answer_general", pleadings, DraftSource.Generated("answer_general")),
            doc(context, "motion_to_dismiss_12b6", motions, DraftSource.Generated("motion_to_dismiss_12b6")),
            doc(context, "motion_more_definite", motions, DraftSource.Generated("motion_more_definite")),
            doc(context, "rule_71_disclosure", disclosures, DraftSource.Generated("rule_71_disclosure")),
            doc(context, "rule_26f_report", discovery, DraftSource.Generated("rule_26f_report")),
            doc(context, "interrogatories_33", discovery, DraftSource.Generated("interrogatories_33")),
            doc(context, "rfp_34", discovery, DraftSource.Generated("rfp_34")),
            doc(context, "rfa_36", discovery, DraftSource.Generated("rfa_36")),
            doc(context, "msj_56", motions, DraftSource.Generated("msj_56")),
            doc(context, "subpoena_45", service, DraftSource.Generated("subpoena_45")),
            doc(context, "cert_of_service", service, DraftSource.Generated("cert_of_service")),
            doc(context, "motion_default_55", judgment, DraftSource.Generated("motion_default_55")),
            doc(context, "motion_new_trial_59", judgment, DraftSource.Generated("motion_new_trial_59")),
            doc(context, "motion_relief_60", judgment, DraftSource.Generated("motion_relief_60")),
            doc(context, "amended_complaint_15", pleadings, DraftSource.Generated("amended_complaint_15")),
        )
    }

    fun byCategory(context: Context): Map<DraftCategory, List<DraftDoc>> =
        all(context).groupBy { it.category }

    private fun doc(
        context: Context,
        id: String,
        category: DraftCategory,
        source: DraftSource
    ): DraftDoc {
        val res = context.resources
        val pkg = context.packageName
        val titleId = res.getIdentifier("draft_${id}_title", "string", pkg)
        val descId = res.getIdentifier("draft_${id}_desc", "string", pkg)
        val citeId = res.getIdentifier("draft_${id}_cite", "string", pkg)
        return DraftDoc(
            id = id,
            title = if (titleId != 0) res.getString(titleId) else id,
            ruleCitation = if (citeId != 0) res.getString(citeId) else "",
            description = if (descId != 0) res.getString(descId) else "",
            category = category,
            source = source
        )
    }
}
