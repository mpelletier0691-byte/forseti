package com.forseti.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Copyright
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.forseti.R
import com.forseti.states.StateRulesCatalog
import com.forseti.ui.shell.ForsetiTopBar
import com.forseti.ui.theme.ForsetiColors

/**
 * Single source-of-truth for everything Forseti links out to. Required by Play
 * Store reviewers and useful for the user — they can audit our citations,
 * follow the original sources, and see the copyright posture for every dataset.
 *
 * Add new external sources here whenever the app starts citing them.
 */
@Composable
fun ReferencesScreen(
    sidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit
) {
    val context = LocalContext.current
    val sectionBundled = stringResource(R.string.refs_section_bundled)
    val sectionFederal = stringResource(R.string.refs_section_federal)
    val sectionStates = stringResource(R.string.refs_section_states)
    val sectionCircuits = stringResource(R.string.refs_section_circuits)
    val sectionResearch = stringResource(R.string.refs_section_research)
    val sectionOpensource = stringResource(R.string.refs_section_opensource)
    val footerText = stringResource(R.string.refs_footer)
    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Column(modifier = Modifier.fillMaxSize().background(ForsetiColors.Background)) {
        ForsetiTopBar(
            title = stringResource(R.string.nav_references),
            sidebarExpanded = sidebarExpanded,
            onToggleSidebar = onToggleSidebar
        )

        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item { LegalNotice(); Spacer(Modifier.height(16.dp)) }

            sectionHeader(sectionBundled)
            staticGroup(bundledSources, ::open)

            sectionHeader(sectionFederal)
            staticGroup(federalSources, ::open)

            sectionHeader(sectionStates)
            stateLinks(StateRulesCatalog.states.map { it.name to it.url }, ::open)

            sectionHeader(sectionCircuits)
            stateLinks(StateRulesCatalog.circuits.map { it.name to it.url }, ::open)

            sectionHeader(sectionResearch)
            staticGroup(caseResearchSources, ::open)

            sectionHeader(sectionOpensource)
            staticGroup(openSource, ::open)

            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    footerText,
                    style = MaterialTheme.typography.bodySmall,
                    color = ForsetiColors.AshGrey
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun LegalNotice() {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Copyright, null, tint = ForsetiColors.RuneGold, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.refs_copyright_heading),
                    style = MaterialTheme.typography.titleMedium,
                    color = ForsetiColors.AshWhite
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.refs_copyright_body1),
                style = MaterialTheme.typography.bodyMedium,
                color = ForsetiColors.AshGrey
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.refs_copyright_body2),
                style = MaterialTheme.typography.bodyMedium,
                color = ForsetiColors.AshGrey
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.refs_not_legal_advice),
                style = MaterialTheme.typography.labelLarge,
                color = ForsetiColors.MeadAmber
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.sectionHeader(title: String) {
    item(key = "h_$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = ForsetiColors.RuneGold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.staticGroup(
    refs: List<Reference>,
    onOpen: (String) -> Unit
) {
    refs.forEach { ref ->
        item(key = "ref_${ref.title}_${ref.url ?: "bundled"}") {
            ReferenceRow(ref = ref, onOpen = onOpen)
            Spacer(Modifier.height(6.dp))
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.stateLinks(
    pairs: List<Pair<String, String>>,
    onOpen: (String) -> Unit
) {
    pairs.forEach { (name, url) ->
        item(key = "st_${name}_$url") {
            CompactLinkRow(name = name, url = url, onOpen = onOpen)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ReferenceRow(ref: Reference, onOpen: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .let { mod ->
                if (ref.url != null) mod.clickable { onOpen(ref.url) } else mod
            }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(ref.icon, null, tint = ForsetiColors.MeadAmber)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(ref.title, style = MaterialTheme.typography.titleMedium, color = ForsetiColors.AshWhite)
                Text(ref.subtitle, style = MaterialTheme.typography.bodySmall, color = ForsetiColors.AshGrey)
                if (ref.licence.isNotBlank()) {
                    Text(
                        stringResource(R.string.refs_license_fmt, ref.licence),
                        style = MaterialTheme.typography.labelSmall,
                        color = ForsetiColors.RuneGoldDim,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (ref.url != null) {
                    Text(
                        ref.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = ForsetiColors.RavenBlue,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            if (ref.url != null) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, stringResource(R.string.action_open), tint = ForsetiColors.AshGrey)
            }
        }
    }
}

@Composable
private fun CompactLinkRow(name: String, url: String, onOpen: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clickable { onOpen(url) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, color = ForsetiColors.AshWhite)
                Text(url, style = MaterialTheme.typography.labelSmall, color = ForsetiColors.RavenBlue)
            }
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, "Open", tint = ForsetiColors.AshGrey)
        }
    }
}

private data class Reference(
    val title: String,
    val subtitle: String,
    val licence: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val url: String? = null
)

private val bundledSources = listOf(
    Reference(
        title = "Federal Rules of Civil Procedure (December 1, 2024 edition)",
        subtitle = "Bundled PDF + extracted outline used by Quick Jump and Drafts.",
        licence = "U.S. Government work — public domain (17 U.S.C. § 105).",
        icon = Icons.Outlined.Verified,
        url = "https://www.uscourts.gov/rules-policies/current-rules-practice-procedure/federal-rules-civil-procedure"
    ),
    Reference(
        title = "In-app Guides",
        subtitle = "Authored for Forseti from publicly available procedure summaries.",
        licence = "© Forseti project. Personal use within the app.",
        icon = Icons.Outlined.Info,
        url = null
    ),
    Reference(
        title = "Glossary",
        subtitle = "Plain-language definitions assembled from the Cornell LII Wex glossary, federal court self-help guides, and other public sources.",
        licence = "Original entries © Forseti; cited definitions credited inline.",
        icon = Icons.Outlined.Info,
        url = "https://www.law.cornell.edu/wex"
    )
)

private val federalSources = listOf(
    Reference(
        title = "U.S. Courts — current rules of practice & procedure",
        subtitle = "Authoritative federal rules portal.",
        licence = "Public domain U.S. Government work.",
        icon = Icons.Outlined.Gavel,
        url = "https://www.uscourts.gov/rules-policies/current-rules-practice-procedure"
    ),
    Reference(
        title = "U.S. Supreme Court — court rules",
        subtitle = "Rules of the Supreme Court of the United States.",
        licence = "Public domain U.S. Government work.",
        icon = Icons.Outlined.Gavel,
        url = "https://www.supremecourt.gov/ctrules/ctrules.aspx"
    ),
    Reference(
        title = "Federal Judicial Center",
        subtitle = "Research and education arm of the federal courts.",
        licence = "Public domain U.S. Government work.",
        icon = Icons.Outlined.Public,
        url = "https://www.fjc.gov/"
    )
)

private val caseResearchSources = listOf(
    Reference(
        title = "Google Scholar — Case Law",
        subtitle = "Used by the Case Studies tab as the default search target.",
        licence = "© Google LLC. Linked under Google's terms of use.",
        icon = Icons.Outlined.Public,
        url = "https://scholar.google.com/"
    ),
    Reference(
        title = "CourtListener / RECAP",
        subtitle = "Free Law Project's repository of federal court documents.",
        licence = "© Free Law Project. Most documents in the public domain.",
        icon = Icons.Outlined.Public,
        url = "https://www.courtlistener.com/"
    ),
    Reference(
        title = "Justia",
        subtitle = "Free state and federal primary law.",
        licence = "© Justia.",
        icon = Icons.Outlined.Public,
        url = "https://law.justia.com/"
    ),
    Reference(
        title = "Cornell Legal Information Institute (LII) — Wex",
        subtitle = "Open legal encyclopedia.",
        licence = "Creative Commons Attribution-Noncommercial-ShareAlike 3.0.",
        icon = Icons.Outlined.Public,
        url = "https://www.law.cornell.edu/wex"
    ),
    Reference(
        title = "PACER — federal courts dockets",
        subtitle = "Public access to federal court records (paid).",
        licence = "© Administrative Office of the U.S. Courts.",
        icon = Icons.Outlined.Public,
        url = "https://pacer.uscourts.gov/"
    )
)

private val openSource = listOf(
    Reference(
        title = "Jetpack Compose, Hilt, Room, WorkManager, CameraX",
        subtitle = "Android Jetpack libraries used to build the UI and persistence layers.",
        licence = "Apache License 2.0.",
        icon = Icons.Outlined.Verified,
        url = "https://developer.android.com/jetpack"
    ),
    Reference(
        title = "Material Icons",
        subtitle = "Iconography across the app.",
        licence = "Apache License 2.0.",
        icon = Icons.Outlined.Verified,
        url = "https://fonts.google.com/icons"
    ),
    Reference(
        title = "kotlinx-datetime, Ktor, OkHttp",
        subtitle = "Date math and networking for state-rule downloads.",
        licence = "Apache License 2.0.",
        icon = Icons.Outlined.Verified,
        url = "https://github.com/Kotlin/kotlinx-datetime"
    )
)
