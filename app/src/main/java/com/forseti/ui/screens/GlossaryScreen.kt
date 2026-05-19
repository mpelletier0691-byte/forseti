package com.forseti.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forseti.LocalTts
import com.forseti.R
import com.forseti.glossary.GlossaryTerm
import com.forseti.tts.ForsetiTts
import com.forseti.ui.shell.ForsetiTopBar
import com.forseti.ui.theme.ForsetiColors

@Composable
fun GlossaryScreen(
    sidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    viewModel: GlossaryViewModel = hiltViewModel()
) {
    val terms by viewModel.terms.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(terms, query) {
        if (query.isBlank()) terms
        else terms.filter { it.term.contains(query, ignoreCase = true) || it.definition.contains(query, ignoreCase = true) }
    }
    val grouped = remember(filtered) { filtered.groupBy { it.term.firstOrNull()?.uppercaseChar() ?: '?' } }
    val tts = LocalTts.current

    Column(modifier = Modifier.fillMaxSize().background(ForsetiColors.Background)) {
        ForsetiTopBar(
            title = stringResource(R.string.nav_glossary),
            sidebarExpanded = sidebarExpanded,
            onToggleSidebar = onToggleSidebar
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.glossary_search_hint)) },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = ForsetiColors.Surface,
                unfocusedContainerColor = ForsetiColors.Surface,
                focusedIndicatorColor = ForsetiColors.RuneGold,
                cursorColor = ForsetiColors.RuneGold
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Text(
            text = "Tap a card to hear its definition aloud (uses your device's system voice).",
            style = MaterialTheme.typography.labelSmall,
            color = ForsetiColors.AshGrey,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
            grouped.forEach { (letter, items) ->
                item(key = "letter_$letter") {
                    Text(
                        text = letter.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = ForsetiColors.RuneGold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(items, key = { it.term }) { term ->
                    GlossaryCard(term = term, tts = tts)
                    Spacer(Modifier.height(8.dp))
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/**
 * Single glossary entry. Tapping the card kicks off speech for *just this
 * definition* via the shared [ForsetiTts] singleton — useful when the user is
 * studying terms because each card is short enough to read aloud quickly.
 */
@Composable
private fun GlossaryCard(term: GlossaryTerm, tts: ForsetiTts) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                tts.ensureReady()
                if (tts.state.value == ForsetiTts.State.Unavailable) return@clickable
                tts.speak("${term.term}. ${term.definition}")
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Column {
                    Text(term.term, style = MaterialTheme.typography.titleMedium, color = ForsetiColors.MeadAmber)
                    Spacer(Modifier.height(4.dp))
                    Text(term.definition, style = MaterialTheme.typography.bodyMedium, color = ForsetiColors.AshWhite)
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.RecordVoiceOver,
                contentDescription = "Read definition aloud",
                tint = ForsetiColors.RuneGold,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
