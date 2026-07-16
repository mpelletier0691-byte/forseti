package com.forseti.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forseti.R
import com.forseti.imports.UploadedRulesService
import com.forseti.pdf.LocalPdfReader
import com.forseti.ui.shell.ForsetiTopBar
import com.forseti.ui.theme.ForsetiColors
import com.forseti.ui.theme.ForsetiDestinationScaffold
import java.io.File
import java.text.DateFormat
import java.util.Date

/**
 * Lets the user import court-rule PDFs that the app couldn't fetch on its own
 * (state site down, paywalled HTML, custom annotated copy, etc.). Files stay
 * in this tab — the case auto-filer skips them — until the user explicitly
 * shares them out or moves them into a case workspace.
 */
@Composable
fun UploadedRulesScreen(
    sidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    viewModel: UploadedRulesViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var viewing by remember { mutableStateOf<UploadedRulesService.UploadedRule?>(null) }

    viewing?.let { rule ->
        LocalPdfReader(
            file = rule.file,
            title = rule.displayTitle,
            onClose = { viewing = null }
        )
        return
    }

    val pickPdf = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingUri = uri
    }

    fun shareFile(file: File) {
        runCatching {
            val uri = viewModel.shareableUri(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share rule PDF"))
        }
    }

    ForsetiDestinationScaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ForsetiTopBar(
                title = stringResource(R.string.nav_imports),
                sidebarExpanded = sidebarExpanded,
                onToggleSidebar = onToggleSidebar,
                actions = {
                    IconButton(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                        Icon(
                            Icons.Outlined.UploadFile,
                            contentDescription = "Import PDF",
                            tint = ForsetiColors.AshWhite
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ForsetiColors.Background)
        ) {
            ImportsBanner(count = items.size, onPick = { pickPdf.launch(arrayOf("application/pdf")) })

            if (items.isEmpty()) {
                EmptyImports(onPick = { pickPdf.launch(arrayOf("application/pdf")) })
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(items, key = { it.file.absolutePath }) { rule ->
                        UploadedRuleCard(
                            rule = rule,
                            onOpen = { viewing = rule },
                            onShare = { shareFile(rule.file) },
                            onRename = { renameTarget = rule.file },
                            onDelete = { viewModel.delete(rule.file) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    pendingUri?.let { uri ->
        ImportMetadataDialog(
            onDismiss = { pendingUri = null },
            onConfirm = { title, jurisdiction, source ->
                viewModel.import(uri, title, jurisdiction, source)
                pendingUri = null
            }
        )
    }

    renameTarget?.let { file ->
        RenameUploadDialog(
            initial = file.nameWithoutExtension,
            onDismiss = { renameTarget = null },
            onConfirm = { title ->
                viewModel.rename(file, title)
                renameTarget = null
            }
        )
    }
}

@Composable
private fun ImportsBanner(count: Int, onPick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.PictureAsPdf, null, tint = ForsetiColors.RuneGold, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your local rules library",
                    style = MaterialTheme.typography.titleMedium,
                    color = ForsetiColors.AshWhite
                )
                Text(
                    text = "Import PDFs of court rules the app couldn't pull from the official site, or your own annotated copies. They stay here and never get auto-filed into a case.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForsetiColors.AshGrey
                )
                Text(
                    text = "$count file${if (count == 1) "" else "s"} stored on this device",
                    style = MaterialTheme.typography.labelSmall,
                    color = ForsetiColors.MeadAmber,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onPick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForsetiColors.RuneGold,
                    contentColor = ForsetiColors.SplashBlack
                )
            ) {
                Icon(Icons.Outlined.UploadFile, null)
                Spacer(Modifier.width(8.dp))
                Text("Import PDF")
            }
        }
    }
}

@Composable
private fun EmptyImports(onPick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.PictureAsPdf,
                null,
                tint = ForsetiColors.RuneGoldDim,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("No uploaded rules yet", style = MaterialTheme.typography.headlineSmall, color = ForsetiColors.RuneGold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap Import PDF to add rules from your downloads, drive, or another app. Forseti keeps the file private to this app.",
                style = MaterialTheme.typography.bodyMedium,
                color = ForsetiColors.AshGrey
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onPick) { Text("Import PDF") }
        }
    }
}

@Composable
private fun UploadedRuleCard(
    rule: UploadedRulesService.UploadedRule,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().clickable { onOpen() }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Description, null, tint = ForsetiColors.MeadAmber)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    rule.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = ForsetiColors.AshWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildString {
                        if (rule.jurisdiction.isNotBlank()) append(rule.jurisdiction).append(" · ")
                        append(rule.file.name)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ForsetiColors.AshGrey
                )
                if (rule.source.isNotBlank()) {
                    Text(
                        "Source: ${rule.source}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForsetiColors.AshGrey
                    )
                }
                Text(
                    "Imported ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(rule.importedAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ForsetiColors.AshGrey,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            IconButton(onClick = onOpen) {
                Icon(Icons.AutoMirrored.Outlined.MenuBook, "Open in reader", tint = ForsetiColors.RuneGold)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, "Share", tint = ForsetiColors.AshGrey)
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Outlined.DriveFileRenameOutline, "Rename", tint = ForsetiColors.AshGrey)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, "Delete", tint = ForsetiColors.AshGrey)
            }
        }
    }
}

@Composable
private fun ImportMetadataDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, jurisdiction: String, source: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var jurisdiction by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onConfirm(title.trim(), jurisdiction.trim(), source.trim()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForsetiColors.RuneGold,
                    contentColor = ForsetiColors.SplashBlack
                )
            ) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = ForsetiColors.AshGrey) } },
        title = { Text("Describe this rule PDF", color = ForsetiColors.RuneGold) },
        containerColor = ForsetiColors.Surface,
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text("Title (e.g. Massachusetts R. Civ. P. 12)") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = goldImportField()
                )
                OutlinedTextField(
                    value = jurisdiction,
                    onValueChange = { jurisdiction = it },
                    singleLine = true,
                    label = { Text("Jurisdiction (state, court, or 'Federal')") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = goldImportField()
                )
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    singleLine = true,
                    label = { Text("Source URL or note (optional)") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = goldImportField()
                )
                Text(
                    "Files stay app-private until you tap Share. They are not auto-filed into any case workspace.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ForsetiColors.AshGrey,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    )
}

@Composable
private fun RenameUploadDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }) {
                Text("Save", color = ForsetiColors.RuneGold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = ForsetiColors.AshGrey) } },
        title = { Text("Rename rule", color = ForsetiColors.RuneGold) },
        containerColor = ForsetiColors.Surface,
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text("New title") },
                modifier = Modifier.fillMaxWidth(),
                colors = goldImportField()
            )
        }
    )
}

@Composable
private fun goldImportField() = TextFieldDefaults.colors(
    focusedContainerColor = ForsetiColors.SurfaceVariant,
    unfocusedContainerColor = ForsetiColors.SurfaceVariant,
    focusedIndicatorColor = ForsetiColors.RuneGold,
    cursorColor = ForsetiColors.RuneGold,
    focusedLabelColor = ForsetiColors.RuneGold
)
