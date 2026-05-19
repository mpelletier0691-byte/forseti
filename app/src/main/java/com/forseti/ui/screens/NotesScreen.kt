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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Note
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.forseti.R
import com.forseti.data.entities.BookmarkEntity
import com.forseti.data.entities.NoteEntity
import com.forseti.ui.shell.ForsetiTopBar
import com.forseti.ui.theme.ForsetiColors

@Composable
fun NotesScreen(
    sidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    var tab by remember { mutableStateOf(0) }
    var showAddNote by remember { mutableStateOf(false) }
    val bookmarks by viewModel.bookmarks.collectAsState()
    val notes by viewModel.notes.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(ForsetiColors.Background)) {
        ForsetiTopBar(
            title = stringResource(R.string.nav_notes),
            sidebarExpanded = sidebarExpanded,
            onToggleSidebar = onToggleSidebar,
            actions = {
                IconButton(onClick = { showAddNote = true }) {
                    Icon(Icons.AutoMirrored.Outlined.Note, "Add note", tint = ForsetiColors.AshWhite)
                }
            }
        )
        TabRow(
            selectedTabIndex = tab,
            containerColor = ForsetiColors.Background,
            contentColor = ForsetiColors.RuneGold
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }) {
                Text(stringResource(R.string.notes_tab_bookmarks), color = if (tab == 0) ForsetiColors.RuneGold else ForsetiColors.AshGrey, modifier = Modifier.padding(12.dp))
            }
            Tab(selected = tab == 1, onClick = { tab = 1 }) {
                Text(stringResource(R.string.notes_tab_notes), color = if (tab == 1) ForsetiColors.RuneGold else ForsetiColors.AshGrey, modifier = Modifier.padding(12.dp))
            }
        }

        when (tab) {
            0 -> BookmarkList(bookmarks, onRemove = viewModel::removeBookmark)
            1 -> NoteList(notes, onDelete = viewModel::deleteNote)
        }
    }

    if (showAddNote) {
        AddNoteDialog(
            onDismiss = { showAddNote = false },
            onSave = { anchor, body ->
                viewModel.upsertNote(anchor, body)
                showAddNote = false
            }
        )
    }
}

@Composable
private fun BookmarkList(items: List<BookmarkEntity>, onRemove: (String) -> Unit) {
    if (items.isEmpty()) {
        EmptyHint(
            icon = Icons.Outlined.Bookmark,
            title = "No bookmarks yet",
            body = "Use the bookmark button on a rule in the Quick Jump tab to save it here."
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(items, key = { it.id }) { b ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Bookmark, null, tint = ForsetiColors.RuneGold)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(b.displayLabel, style = MaterialTheme.typography.titleMedium, color = ForsetiColors.AshWhite)
                        Text(b.ruleAnchor, style = MaterialTheme.typography.bodySmall, color = ForsetiColors.AshGrey)
                    }
                    IconButton(onClick = { onRemove(b.ruleAnchor) }) {
                        Icon(Icons.Outlined.Delete, "Remove", tint = ForsetiColors.AshGrey)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NoteList(items: List<NoteEntity>, onDelete: (Long) -> Unit) {
    if (items.isEmpty()) {
        EmptyHint(
            icon = Icons.AutoMirrored.Outlined.Note,
            title = "No notes yet",
            body = "Tap the note icon in the top bar to add a quick thought tied to a rule anchor (e.g. rule.12.b.6)."
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(items, key = { it.id }) { n ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row {
                        Text(n.ruleAnchor, style = MaterialTheme.typography.labelLarge, color = ForsetiColors.MeadAmber, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onDelete(n.id) }) {
                            Icon(Icons.Outlined.Delete, "Delete", tint = ForsetiColors.AshGrey)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    SelectionContainer {
                        Text(n.body, style = MaterialTheme.typography.bodyMedium, color = ForsetiColors.AshWhite)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmptyHint(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = ForsetiColors.RuneGoldDim, modifier = Modifier.height(36.dp).width(36.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, color = ForsetiColors.RuneGold)
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = ForsetiColors.AshGrey)
        }
    }
}

@Composable
private fun AddNoteDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var anchor by remember { mutableStateOf("rule.") }
    var body by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (anchor.isNotBlank() && body.isNotBlank()) onSave(anchor.trim(), body.trim())
            }) { Text("Save", color = ForsetiColors.RuneGold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = ForsetiColors.AshGrey) } },
        title = { Text("New note", color = ForsetiColors.RuneGold) },
        containerColor = ForsetiColors.Surface,
        text = {
            Column {
                OutlinedTextField(
                    value = anchor,
                    onValueChange = { anchor = it },
                    label = { Text("Rule anchor (e.g. rule.12.b.6)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = goldField()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Note") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = goldField()
                )
            }
        }
    )
}

@Composable
private fun goldField() = TextFieldDefaults.colors(
    focusedContainerColor = ForsetiColors.SurfaceVariant,
    unfocusedContainerColor = ForsetiColors.SurfaceVariant,
    focusedIndicatorColor = ForsetiColors.RuneGold,
    cursorColor = ForsetiColors.RuneGold,
    focusedLabelColor = ForsetiColors.RuneGold
)
