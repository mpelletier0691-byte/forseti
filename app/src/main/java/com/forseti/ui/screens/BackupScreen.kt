package com.forseti.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forseti.R
import com.forseti.ui.shell.ForsetiTopBar
import com.forseti.ui.theme.ForsetiColors
import com.forseti.ui.theme.ForsetiDestinationScaffold
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Backup tab. One-tap "Save case workspace" produces an atomic ZIP under app
 * cache and lists prior backups for share/delete. Restore is intentionally
 * out-of-app (extract the ZIP into the shown workspace path) until SAF
 * picker work lands.
 */
@Composable
fun BackupScreen(
    sidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val busy by viewModel.busy.collectAsState()
    val backups by viewModel.backups.collectAsState()
    val result by viewModel.lastResult.collectAsState()

    LaunchedEffect(result) {
        result?.let { r ->
            snackbar.showSnackbar(
                "Backup ready: ${r.entries} entries, ${formatSize(r.sizeBytes)}"
            )
            viewModel.consumeResult()
        }
    }

    ForsetiDestinationScaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ForsetiTopBar(
                title = stringResource(R.string.nav_backup),
                sidebarExpanded = sidebarExpanded,
                onToggleSidebar = onToggleSidebar
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ForsetiColors.Background)
        ) {
            BackupHeader(
                workspacePath = viewModel.workspacePath,
                busy = busy,
                onCreate = { viewModel.createBackup() }
            )
            HorizontalDivider(color = ForsetiColors.Stone)
            BackupList(
                files = backups,
                onShare = { file ->
                    val uri = viewModel.shareUri(file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share backup"))
                },
                onDelete = { viewModel.delete(it) }
            )
        }
    }
}

@Composable
private fun BackupHeader(
    workspacePath: String,
    busy: Boolean,
    onCreate: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Folder, null, tint = ForsetiColors.RuneGold)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Workspace path",
                        style = MaterialTheme.typography.titleMedium,
                        color = ForsetiColors.AshWhite
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    workspacePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = ForsetiColors.AshGrey
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onCreate,
            enabled = !busy,
            colors = ButtonDefaults.buttonColors(
                containerColor = ForsetiColors.RuneGold,
                contentColor = ForsetiColors.SplashBlack
            )
        ) {
            if (busy) {
                CircularProgressIndicator(
                    color = ForsetiColors.SplashBlack,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.backup_creating))
            } else {
                Icon(Icons.Outlined.Backup, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.backup_save_zip))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Backups bundle every case workspace folder on this device. The ZIP " +
                "is stored in app cache and can be shared anywhere (Drive, email, USB).",
            style = MaterialTheme.typography.bodySmall,
            color = ForsetiColors.AshGrey
        )
    }
}

@Composable
private fun BackupList(
    files: List<File>,
    onShare: (File) -> Unit,
    onDelete: (File) -> Unit
) {
    if (files.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "No backups yet.",
                style = MaterialTheme.typography.titleMedium,
                color = ForsetiColors.RuneGold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.backup_first_hint),
                style = MaterialTheme.typography.bodySmall,
                color = ForsetiColors.AshGrey
            )
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        items(files, key = { it.absolutePath }) { file ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            file.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = ForsetiColors.AshWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${formatSize(file.length())} \u00B7 ${formatStamp(file.lastModified())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForsetiColors.AshGrey
                        )
                    }
                    IconButton(onClick = { onShare(file) }) {
                        Icon(
                            Icons.Outlined.IosShare,
                            "Share backup",
                            tint = ForsetiColors.RavenBlue
                        )
                    }
                    IconButton(onClick = { onDelete(file) }) {
                        Icon(
                            Icons.Outlined.Delete,
                            "Delete backup",
                            tint = ForsetiColors.AshGrey
                        )
                    }
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}

private fun formatStamp(epochMs: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(epochMs))
