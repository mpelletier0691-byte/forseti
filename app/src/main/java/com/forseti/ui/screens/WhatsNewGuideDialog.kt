package com.forseti.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forseti.R
import com.forseti.guides.SafeMarkdown
import com.forseti.ui.theme.ForsetiColors

/** Offline guide reader for Settings → Help → What\'s New. */
@Composable
fun WhatsNewGuideDialog(onDismiss: () -> Unit, viewModel: GuidesViewModel = hiltViewModel()) {
    val guides by viewModel.guides.collectAsState()
    val meta = guides.firstOrNull { it.id == "whats_new" }
    val body by viewModel.bodyFor(meta).collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.whats_new_done), color = ForsetiColors.RuneGold)
            }
        },
        title = {
            Text(
                meta?.title ?: stringResource(R.string.settings_whats_new_title),
                color = ForsetiColors.RuneGold
            )
        },
        containerColor = ForsetiColors.Surface,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SafeMarkdown(body)
            }
        }
    )
}
