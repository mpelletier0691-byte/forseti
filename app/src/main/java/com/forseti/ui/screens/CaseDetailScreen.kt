package com.forseti.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forseti.casefiles.CaseFileViewer
import com.forseti.casefiles.CaseFolderService
import com.forseti.ui.theme.ForsetiColors
import java.io.File
import java.text.DateFormat
import java.util.Date

/**
 * Brokkr-Forge style case workspace: shows the case header, the FRCP-aware
 * folder tree under the case root, and lets the user rename / delete / share /
 * import files into any subfolder.
 *
 * Wired into [CasesScreen] via the case-id navigation flag — the parent
 * `CasesScreen` swaps the list out for this when a card is tapped.
 */
@Composable
fun CaseDetailScreen(
    caseId: Long,
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") sidebarExpanded: Boolean,
    @Suppress("UNUSED_PARAMETER") onToggleSidebar: () -> Unit,
    viewModel: CaseDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    // Stash the import target across the SAF launcher round-trip.
    var importInto by remember { mutableStateOf<File?>(null) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var viewing by remember { mutableStateOf<File?>(null) }
    var moveAfterRename by remember { mutableStateOf<File?>(null) }

    // Folder viewer takes over the whole pane while the user is reading a file.
    viewing?.let { f ->
        CaseFileViewer(
            file = f,
            folderService = viewModel.folderService,
            onBack = { viewing = null }
        )
        return
    }

    val openDoc = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val target = importInto
        if (uri != null && target != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/')
            viewModel.importInto(uri, target, name)
        }
        importInto = null
    }

    LaunchedEffect(caseId) { viewModel.load(caseId) }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ForsetiColors.Background)
        ) {
            DetailTopBar(
                title = state.case?.title?.ifBlank { "Case" } ?: "Case",
                onBack = onBack
            )

            CaseHeader(
                case = state.case,
                workspaceRoot = state.workspaceRoot
            )

            HorizontalDivider(color = ForsetiColors.Stone)

            if (state.folders.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Workspace is being created…",
                        color = ForsetiColors.AshGrey,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(12.dp)) {
                    items(state.folders, key = { it.phase.absolutePath }) { phase ->
                        PhaseSection(
                            phase = phase,
                            isExpanded = expanded[phase.phase.absolutePath] ?: false,
                            onToggle = {
                                val key = phase.phase.absolutePath
                                expanded[key] = !(expanded[key] ?: false)
                            },
                            onShareFile = { file ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "*/*"
                                    putExtra(Intent.EXTRA_STREAM, viewModel.shareableUri(file))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
                            },
                            onRenameFile = { renameTarget = it },
                            onDeleteFile = { viewModel.delete(it) },
                            onImportInto = { dst ->
                                importInto = dst
                                openDoc.launch(arrayOf("*/*"))
                            },
                            onOpenFile = { viewing = it }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    item { Spacer(Modifier.height(48.dp)) }
                }
            }
        }
    }

    renameTarget?.let { file ->
        RenameDialog(
            current = file.name,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                viewModel.rename(file, newName) { renamed ->
                    moveAfterRename = renamed
                }
                renameTarget = null
            }
        )
    }

    moveAfterRename?.let { file ->
        MoveAfterRenameDialog(
            file = file,
            folders = state.folders,
            onKeep = { moveAfterRename = null },
            onMove = { dest ->
                viewModel.move(file, dest)
                moveAfterRename = null
            }
        )
    }
}

@Composable
private fun DetailTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ForsetiColors.Sidebar)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back to cases",
                tint = ForsetiColors.AshWhite
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = ForsetiColors.RuneGold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CaseHeader(
    case: com.forseti.data.entities.CaseEntity?,
    workspaceRoot: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        if (case != null) {
            Text(
                listOfNotNull(
                    case.court.takeIf { it.isNotBlank() },
                    case.caseNumber.takeIf { it.isNotBlank() },
                    case.role.takeIf { it.isNotBlank() }
                ).joinToString(" \u00B7 "),
                style = MaterialTheme.typography.bodyMedium,
                color = ForsetiColors.AshWhite
            )
            Spacer(Modifier.height(4.dp))
        }
        if (workspaceRoot.isNotBlank()) {
            Text(
                "Workspace: $workspaceRoot",
                style = MaterialTheme.typography.labelSmall,
                color = ForsetiColors.AshGrey
            )
        }
    }
}

@Composable
private fun PhaseSection(
    phase: CaseDetailViewModel.FolderNode,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onShareFile: (File) -> Unit,
    onRenameFile: (File) -> Unit,
    onDeleteFile: (File) -> Unit,
    onImportInto: (File) -> Unit,
    onOpenFile: (File) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Folder, null, tint = ForsetiColors.RuneGold)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        prettyName(phase.phase.name),
                        style = MaterialTheme.typography.titleMedium,
                        color = ForsetiColors.AshWhite
                    )
                    Text(
                        "${countFiles(phase)} file(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForsetiColors.AshGrey
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = ForsetiColors.AshGrey
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    phase.subfolders.forEach { sub ->
                        SubfolderBlock(
                            sub = sub,
                            isPhaseRoot = sub.folder == phase.phase,
                            onShareFile = onShareFile,
                            onRenameFile = onRenameFile,
                            onDeleteFile = onDeleteFile,
                            onImportInto = onImportInto,
                            onOpenFile = onOpenFile
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SubfolderBlock(
    sub: CaseDetailViewModel.SubfolderNode,
    isPhaseRoot: Boolean,
    onShareFile: (File) -> Unit,
    onRenameFile: (File) -> Unit,
    onDeleteFile: (File) -> Unit,
    onImportInto: (File) -> Unit,
    onOpenFile: (File) -> Unit
) {
    Surface(
        color = ForsetiColors.SurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = ForsetiColors.RavenBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isPhaseRoot) "(Top of phase)" else sub.folder.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = ForsetiColors.AshWhite,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onImportInto(sub.folder) }) {
                    Icon(Icons.Outlined.Add, "Import file", tint = ForsetiColors.RuneGold)
                }
            }
            if (sub.files.isEmpty()) {
                Text(
                    "Empty",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForsetiColors.AshGrey,
                    modifier = Modifier.padding(start = 26.dp, top = 2.dp, bottom = 2.dp)
                )
            } else {
                sub.files.forEach { file ->
                    FileRow(
                        file = file,
                        onOpen = { onOpenFile(file) },
                        onShare = { onShareFile(file) },
                        onRename = { onRenameFile(file) },
                        onDelete = { onDeleteFile(file) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    file: File,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 18.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = ForsetiColors.MeadAmber,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = ForsetiColors.AshWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${formatSize(file.length())} \u00B7 ${formatStamp(file.lastModified())}",
                style = MaterialTheme.typography.labelSmall,
                color = ForsetiColors.AshGrey
            )
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Outlined.Share, "Share", tint = ForsetiColors.RavenBlue)
        }
        IconButton(onClick = onRename) {
            Icon(Icons.Outlined.DriveFileRenameOutline, "Rename", tint = ForsetiColors.RuneGold)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, "Delete", tint = ForsetiColors.MeadAmber)
        }
    }
}

/**
 * After a successful rename we offer to relocate the file in case the new
 * filename suggests a different home (e.g. user renamed scan001.pdf →
 * 2024-05-08_proof_of_service.pdf and now wants it in 02_Service_of_Process).
 */
@Composable
private fun MoveAfterRenameDialog(
    file: File,
    folders: List<CaseDetailViewModel.FolderNode>,
    onKeep: () -> Unit,
    onMove: (File) -> Unit
) {
    val scroll = rememberScrollState()
    AlertDialog(
        onDismissRequest = onKeep,
        confirmButton = {
            TextButton(onClick = onKeep) { Text("Keep here", color = ForsetiColors.AshGrey) }
        },
        title = { Text("Move \"${file.name}\"?", color = ForsetiColors.RuneGold) },
        containerColor = ForsetiColors.Surface,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                Text(
                    "Now that the file has a clearer name, would you like to drop it into a different folder?",
                    color = ForsetiColors.AshGrey,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                ) {
                    folders.forEach { phase ->
                        phase.subfolders.forEach { sub ->
                            val label = if (sub.folder == phase.phase) {
                                prettyName(phase.phase.name)
                            } else {
                                "${prettyName(phase.phase.name)} / ${sub.folder.name}"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMove(sub.folder) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.FolderOpen, null, tint = ForsetiColors.MeadAmber)
                                Spacer(Modifier.width(10.dp))
                                Text(label, color = ForsetiColors.AshWhite, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun RenameDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(current) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank() && value != current,
                onClick = { onConfirm(value.trim()) }
            ) { Text("Rename", color = ForsetiColors.RuneGold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = ForsetiColors.AshGrey) }
        },
        title = { Text("Rename file", color = ForsetiColors.RuneGold) },
        containerColor = ForsetiColors.Surface,
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text("New name") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ForsetiColors.SurfaceVariant,
                    unfocusedContainerColor = ForsetiColors.SurfaceVariant,
                    focusedIndicatorColor = ForsetiColors.RuneGold,
                    cursorColor = ForsetiColors.RuneGold,
                    focusedLabelColor = ForsetiColors.RuneGold
                )
            )
        }
    )
}

private fun countFiles(node: CaseDetailViewModel.FolderNode): Int =
    node.subfolders.sumOf { it.files.size }

private fun prettyName(folderName: String): String {
    // "01_Pleadings" → "Pleadings"; "98_Scans" → "Scans"
    val cleaned = folderName.substringAfter('_', folderName)
    return cleaned.replace('_', ' ')
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes B"
    bytes < 1_048_576 -> "${"%.1f".format(bytes / 1_024f)} KB"
    bytes < 1_073_741_824 -> "${"%.1f".format(bytes / 1_048_576f)} MB"
    else -> "${"%.1f".format(bytes / 1_073_741_824f)} GB"
}

private fun formatStamp(epochMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(epochMs))
