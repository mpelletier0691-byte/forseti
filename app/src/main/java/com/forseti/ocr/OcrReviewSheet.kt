package com.forseti.ocr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.forseti.ui.theme.ForsetiColors

/**
 * Modal review surface shown after OCR capture. The user picks a block on the
 * left, then taps the field on the right that should receive that block's text.
 * "Build PDF" returns the populated assignments to the caller.
 */
@Composable
fun OcrReviewSheet(
    blocks: List<RecognizedBlock>,
    initial: List<FieldAssignment>,
    onDismiss: () -> Unit,
    onConfirm: (List<FieldAssignment>) -> Unit
) {
    var selectedBlockId by remember { mutableStateOf<Int?>(null) }
    val assignments = remember { initial.toMutableStateList() }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xCC000000)).clickable(onClick = onDismiss),
    ) {
        Surface(
            color = ForsetiColors.Surface,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .fillMaxWidth()
                .height(560.dp)
                .clickable(enabled = false) {}
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "Tap a recognized block, then tap a field to populate it.",
                    style = MaterialTheme.typography.titleMedium,
                    color = ForsetiColors.RuneGold
                )
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.weight(1f).fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                        Text("Recognized text", color = ForsetiColors.AshGrey, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        LazyColumn {
                            items(blocks, key = { it.id }) { b ->
                                BlockTile(
                                    block = b,
                                    selected = b.id == selectedBlockId,
                                    onClick = { selectedBlockId = b.id }
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                        Text("Fields", color = ForsetiColors.AshGrey, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        LazyColumn {
                            items(assignments.size) { idx ->
                                val a = assignments[idx]
                                FieldTile(
                                    assignment = a,
                                    onClick = {
                                        val sel = selectedBlockId
                                        if (sel != null) {
                                            val text = blocks.firstOrNull { it.id == sel }?.text
                                            assignments[idx] = a.copy(text = text)
                                        }
                                    },
                                    onClear = { assignments[idx] = a.copy(text = null) }
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = ForsetiColors.AshGrey) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(assignments.toList()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForsetiColors.RuneGold,
                            contentColor = ForsetiColors.SplashBlack
                        )
                    ) { Text("Build PDF") }
                }
            }
        }
    }
}

@Composable
private fun BlockTile(block: RecognizedBlock, selected: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) ForsetiColors.SidebarSelected else ForsetiColors.SurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) ForsetiColors.RuneGold else ForsetiColors.AshWhite,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Block ${block.id} \u00B7 confidence ${"%.0f".format(block.confidence * 100)}%",
                style = MaterialTheme.typography.labelSmall,
                color = ForsetiColors.AshGrey
            )
        }
    }
}

@Composable
private fun FieldTile(assignment: FieldAssignment, onClick: () -> Unit, onClear: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.SurfaceVariant),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(assignment.key.label, style = MaterialTheme.typography.labelLarge, color = ForsetiColors.MeadAmber)
                Text(
                    text = assignment.text ?: "tap to assign",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (assignment.text == null) ForsetiColors.AshGrey else ForsetiColors.AshWhite,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (assignment.text != null) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Outlined.ContentPaste, "Clear", tint = ForsetiColors.AshGrey, modifier = Modifier.height(18.dp).width(18.dp))
                }
            }
        }
    }
}

