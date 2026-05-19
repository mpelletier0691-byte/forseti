package com.forseti.pdf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.forseti.R
import com.forseti.tts.ForsetiTts
import com.forseti.ui.theme.ForsetiColors
import com.forseti.util.copyPlainText

/**
 * OCR-backed, selectable text for a single PDF page. Opened from Quick Jump /
 * Local PDF reader via long-press or double-tap on the page raster, or the
 * top-bar page-text icon. Supports copy-all and read-aloud for the whole page
 * or a text selection (system TTS).
 */
@Composable
fun PdfPageTextDialog(
    visible: Boolean,
    pageNumberOneBased: Int,
    loading: Boolean,
    body: String,
    onDismiss: () -> Unit,
    tts: ForsetiTts
) {
    if (!visible) return

    val context = LocalContext.current
    val titleText = stringResource(R.string.pdf_page_text_title, pageNumberOneBased)
    var tv by remember { mutableStateOf(TextFieldValue("", TextRange.Zero)) }

    LaunchedEffect(body, loading) {
        if (!loading) {
            tv = TextFieldValue(body, TextRange(0, 0))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(titleText, color = ForsetiColors.RuneGold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (loading) {
                    CircularProgressIndicator(color = ForsetiColors.RuneGold)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.pdf_page_text_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = ForsetiColors.AshGrey
                    )
                } else if (body.isBlank()) {
                    Text(
                        stringResource(R.string.quick_jump_copy_no_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ForsetiColors.AshGrey
                    )
                } else {
                    OutlinedTextField(
                        value = tv,
                        onValueChange = { tv = it },
                        readOnly = true,
                        minLines = 10,
                        maxLines = 22,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 420.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ForsetiColors.SurfaceVariant,
                            unfocusedContainerColor = ForsetiColors.SurfaceVariant,
                            focusedIndicatorColor = ForsetiColors.RuneGold,
                            cursorColor = ForsetiColors.RuneGold,
                            focusedTextColor = ForsetiColors.AshWhite,
                            unfocusedTextColor = ForsetiColors.AshWhite
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.pdf_page_text_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = ForsetiColors.AshGrey
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = {
                        tts.ensureReady()
                        if (body.isNotBlank()) tts.speak(body)
                    },
                    enabled = !loading && body.isNotBlank()
                ) {
                    Text(stringResource(R.string.pdf_read_all_page), color = ForsetiColors.RuneGold)
                }
                TextButton(
                    onClick = {
                        val sel = tv.selection
                        if (sel.collapsed) return@TextButton
                        val start = sel.min.coerceIn(0, tv.text.length)
                        val end = sel.max.coerceIn(0, tv.text.length)
                        val slice = tv.text.substring(start, end).trim()
                        if (slice.isNotBlank()) {
                            tts.ensureReady()
                            tts.speak(slice)
                        }
                    },
                    enabled = !loading && body.isNotBlank()
                ) {
                    Text(stringResource(R.string.pdf_read_selection), color = ForsetiColors.RuneGold)
                }
                TextButton(
                    onClick = {
                        if (body.isNotBlank()) {
                            context.copyPlainText(label = titleText, text = body)
                        }
                    },
                    enabled = !loading && body.isNotBlank()
                ) {
                    Text(stringResource(R.string.pdf_copy_all), color = ForsetiColors.MeadAmber)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pdf_page_text_close), color = ForsetiColors.AshGrey)
            }
        },
        containerColor = ForsetiColors.Surface
    )
}
