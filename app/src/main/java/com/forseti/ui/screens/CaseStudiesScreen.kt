package com.forseti.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.forseti.R
import com.forseti.ui.shell.ForsetiTopBar
import com.forseti.ui.theme.ForsetiColors
import java.net.URLEncoder

/**
 * Sends the user out to Google Scholar Case Law for actual case research, plus
 * a few starter shortcuts (federal, state, supreme court). Forseti deliberately
 * does not embed a webview here — Play Store policy makes embedded browsing of
 * arbitrary third-party content risky, and the user already has a configured
 * default browser they trust.
 */
@Composable
fun CaseStudiesScreen(
    sidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current

    fun open(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(ForsetiColors.Background)) {
        ForsetiTopBar(
            title = stringResource(R.string.nav_case_studies),
            sidebarExpanded = sidebarExpanded,
            onToggleSidebar = onToggleSidebar
        )

        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item {
                IntroCard()
                Spacer(Modifier.height(12.dp))
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Search Google Scholar Case Law",
                            style = MaterialTheme.typography.titleMedium,
                            color = ForsetiColors.AshWhite
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            placeholder = { Text("e.g. \"qualified immunity\" OR Bell Atlantic v. Twombly") },
                            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = ForsetiColors.RuneGold) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = ForsetiColors.SurfaceVariant,
                                unfocusedContainerColor = ForsetiColors.SurfaceVariant,
                                focusedIndicatorColor = ForsetiColors.RuneGold,
                                cursorColor = ForsetiColors.RuneGold
                            )
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    val q = URLEncoder.encode(query.ifBlank { "Federal Rules of Civil Procedure" }, "UTF-8")
                                    open("https://scholar.google.com/scholar?as_sdt=2006&q=$q")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ForsetiColors.RuneGold,
                                    contentColor = ForsetiColors.SplashBlack
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Search Case Law")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            item {
                Text(
                    "Quick links",
                    style = MaterialTheme.typography.titleLarge,
                    color = ForsetiColors.RuneGold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(quickLinks, key = { it.title }) { link ->
                CaseLinkCard(link = link, onOpen = { open(link.url) })
                Spacer(Modifier.height(8.dp))
            }
            item {
                Spacer(Modifier.height(16.dp))
                SaveCaseTipCard()
                Spacer(Modifier.height(12.dp))
                Text(
                    "Case search opens in your default browser. Forseti does not host or republish opinions; please respect each provider's terms of use and any paywall protections.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForsetiColors.AshGrey
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun IntroCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Gavel, null, tint = ForsetiColors.RuneGold, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Find binding & persuasive authority",
                    style = MaterialTheme.typography.titleMedium,
                    color = ForsetiColors.AshWhite
                )
                Text(
                    "Search Google Scholar Case Law for state and federal opinions, KeyCite-style cited-by lists, and citation strings you can copy into a brief.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForsetiColors.AshGrey
                )
            }
        }
    }
}

@Composable
private fun CaseLinkCard(link: CaseLink, onOpen: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Public, null, tint = ForsetiColors.MeadAmber)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(link.title, style = MaterialTheme.typography.titleMedium, color = ForsetiColors.AshWhite)
                Text(link.subtitle, style = MaterialTheme.typography.bodySmall, color = ForsetiColors.AshGrey)
            }
            Button(
                onClick = onOpen,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForsetiColors.SidebarSelected,
                    contentColor = ForsetiColors.RuneGold
                )
            ) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null)
                Spacer(Modifier.width(6.dp))
                Text("Open")
            }
        }
    }
}

@Composable
private fun SaveCaseTipCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.SurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = ForsetiColors.RuneGold)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Save a case into your case workspace",
                    style = MaterialTheme.typography.titleMedium,
                    color = ForsetiColors.RuneGold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "1. In your browser, open the opinion you want to keep.\n" +
                        "2. Use the browser\u2019s Print or Share \u2192 Save as PDF option.\n" +
                        "3. Share the resulting PDF to Forseti (Android share sheet \u2192 Forseti \u2192 Case workspace).\n" +
                        "4. Forseti will route it to 04_Evidence/PDFs/ or, if the filename contains \u201Corder\u201D / \u201Cbrief\u201D / etc., the right Brokkr-Forge folder.\n\n" +
                        "Tip: rename the PDF first (e.g. \u201CTwombly_v_Bell_Atlantic_550US544.pdf\u201D) so the auto-router can shelve it cleanly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForsetiColors.AshGrey
                )
            }
        }
    }
}

private data class CaseLink(val title: String, val subtitle: String, val url: String)

private val quickLinks = listOf(
    CaseLink(
        title = "Google Scholar — Case Law (all)",
        subtitle = "Federal + state opinions; default jurisdiction picker on the results page.",
        url = "https://scholar.google.com/scholar?as_sdt=2006"
    ),
    CaseLink(
        title = "U.S. Supreme Court (Scholar)",
        subtitle = "Highest court in the federal system.",
        url = "https://scholar.google.com/scholar?as_sdt=4,60"
    ),
    CaseLink(
        title = "Federal Circuit Courts (Scholar)",
        subtitle = "All thirteen U.S. Courts of Appeals.",
        url = "https://scholar.google.com/scholar?as_sdt=2&hl=en&as_yhi=2026"
    ),
    CaseLink(
        title = "PACER — federal docket lookup",
        subtitle = "Pay-per-page docket and filings; account required.",
        url = "https://pacer.uscourts.gov/"
    ),
    CaseLink(
        title = "CourtListener (free recap.uscourts.gov mirror)",
        subtitle = "Free copies of many federal filings recovered through the RECAP project.",
        url = "https://www.courtlistener.com/"
    ),
    CaseLink(
        title = "Justia — primary law",
        subtitle = "Free state and federal codes, regs, and case law.",
        url = "https://law.justia.com/"
    ),
    CaseLink(
        title = "Cornell LII — Wex legal dictionary",
        subtitle = "Plain-language explanations of doctrines and procedure.",
        url = "https://www.law.cornell.edu/wex"
    )
)
