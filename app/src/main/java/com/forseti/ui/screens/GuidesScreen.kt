package com.forseti.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forseti.LocalTts
import com.forseti.R
import com.forseti.guides.GuideMeta
import com.forseti.guides.SafeMarkdown
import com.forseti.tts.ReadAloudControls
import com.forseti.ui.shell.ForsetiTopBar
import com.forseti.ui.theme.ForsetiColors

@Composable
fun GuidesScreen(
    sidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    viewModel: GuidesViewModel = hiltViewModel()
) {
    val guides by viewModel.guides.collectAsState()
    var openId by remember { mutableStateOf<String?>(null) }
    val openGuide = guides.firstOrNull { it.id == openId }
    val body by viewModel.bodyFor(openGuide).collectAsState()

    val tts = LocalTts.current
    Column(modifier = Modifier.fillMaxSize().background(ForsetiColors.Background)) {
        ForsetiTopBar(
            title = openGuide?.title ?: stringResource(R.string.nav_guides),
            sidebarExpanded = sidebarExpanded,
            onToggleSidebar = onToggleSidebar,
            actions = {
                if (openGuide != null) {
                    ReadAloudControls(
                        tts = tts,
                        fetchText = { stripMarkdown(body) },
                        iconTint = ForsetiColors.AshWhite
                    )
                    IconButton(onClick = { openId = null }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            stringResource(R.string.guides_back_cd),
                            tint = ForsetiColors.AshWhite
                        )
                    }
                }
            }
        )
        if (openGuide == null) {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(guides, key = { it.id }) { meta ->
                    GuideCard(meta = meta, onOpen = { openId = meta.id })
                    Spacer(Modifier.height(10.dp))
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                SelectionContainer(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Column {
                        // SafeMarkdown is an in-house renderer built to never crash on the
                        // markdown subset we ship in `assets/guides/`. It replaced the
                        // multiplatform-markdown-renderer-m3 dependency that crashed on tables.
                        if (body.isNotBlank()) {
                            SafeMarkdown(content = body)
                        } else {
                            Text(
                                "Loading…",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = ForsetiColors.AshGrey
                            )
                        }
                        Spacer(Modifier.height(48.dp))
                    }
                }
            }
        }
    }
}

/**
 * Strips markdown syntax so the TTS engine speaks readable prose instead of
 * pronouncing `#`, `**`, etc. The full guide is shipped as a single string so
 * we can normalise it once per Read tap rather than walking the AST.
 */
private fun stripMarkdown(md: String): String {
    if (md.isBlank()) return ""
    return md
        .replace(Regex("```[\\s\\S]*?```"), " ")           // fenced code
        .replace(Regex("`[^`]*`"), " ")                     // inline code
        .replace(Regex("!\\[[^]]*]\\([^)]*\\)"), " ")       // images
        .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")     // links → label only
        .replace(Regex("^[#>\\-*+]+\\s*", RegexOption.MULTILINE), "")
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")       // bold
        .replace(Regex("\\*([^*]+)\\*"), "$1")               // italics
        .replace(Regex("_([^_]+)_"), "$1")                  // underline italics
        .replace(Regex("\\|"), " ")                         // table pipes
        .replace(Regex("\\s+"), " ")
        .trim()
}

@Composable
private fun GuideCard(meta: GuideMeta, onOpen: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onOpen)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = null,
                tint = ForsetiColors.RuneGold
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(meta.title, style = MaterialTheme.typography.titleMedium, color = ForsetiColors.AshWhite)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(
                        Icons.Outlined.Schedule,
                        null,
                        tint = ForsetiColors.AshGrey,
                        modifier = Modifier.height(14.dp).width(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.guides_min_read_fmt, meta.minutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = ForsetiColors.AshGrey
                    )
                }
            }
        }
    }
}
