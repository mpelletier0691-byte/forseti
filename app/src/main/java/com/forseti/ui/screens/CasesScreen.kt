package com.forseti.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DriveFolderUpload
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.forseti.data.entities.CaseEntity
import com.forseti.ui.shell.ForsetiScreenScaffold
import com.forseti.util.IngestUriPermissions
import com.forseti.util.RequestNotificationsPermissionOnce
import com.forseti.ui.theme.ForsetiColors
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

import androidx.compose.runtime.rememberCoroutineScope

/**
 * Case Profile tab. Browse and edit cases, see a completeness hint per card,
 * and reveal the on-device folder path that the workspace creates.
 */
@Composable
fun CasesScreen(
    sidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    viewModel: CasesViewModel = hiltViewModel()
) {
    RequestNotificationsPermissionOnce()

    val cases by viewModel.cases.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<CaseEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    // While the New-case dialog is open, the first Brokkr-Forge / image-ingest
    // tap transparently saves the draft so a real workspace exists; we cache
    // that id here so subsequent ingests (and the final Save) target the same
    // case instead of inserting duplicates.
    var creatingDraftId by remember { mutableStateOf(0L) }
    var openCaseId by remember { mutableStateOf<Long?>(null) }
    // Two-step confirmation guard for the destructive Delete action. We keep
    // the CaseEntity in state so the dialog can show its title and so a stale
    // tap on a recomposed list can't trigger the wrong deletion.
    var pendingDelete by remember { mutableStateOf<CaseEntity?>(null) }

    val ingestMessage by viewModel.ingestMessage.collectAsState()
    LaunchedEffect(ingestMessage) {
        ingestMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeIngestMessage()
        }
    }

    // Drill-in to the in-app file browser for the selected case.
    val opened = openCaseId
    if (opened != null) {
        // Intercept system back / swipe-back so it closes the case detail view
        // before the shell-level BackHandler returns the user to the dashboard.
        BackHandler { openCaseId = null }
        CaseDetailScreen(
            caseId = opened,
            onBack = { openCaseId = null },
            sidebarExpanded = sidebarExpanded,
            onToggleSidebar = onToggleSidebar
        )
        return
    }

    ForsetiScreenScaffold(
        title = stringResource(R.string.nav_cases),
        sidebarExpanded = sidebarExpanded,
        onBackToDashboard = onToggleSidebar,
        snackbarHostState = snackbar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                containerColor = ForsetiColors.RuneGold,
                contentColor = ForsetiColors.SplashBlack
            ) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.cases_new_case))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ForsetiColors.Background)
        ) {

            if (cases.isEmpty()) {
                EmptyCases()
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(cases, key = { it.id }) { case ->
                        CaseCard(
                            case = case,
                            completeness = viewModel.completeness(case),
                            onOpen = { openCaseId = case.id },
                            onEdit = { editing = case },
                            onShowFolder = { viewModel.showFolderPath(case) },
                            onDelete = { pendingDelete = case }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }
        }
    }

    if (creating) {
        EditCaseDialog(
            initial = blankCase(),
            title = "New case",
            onDismiss = {
                creating = false
                creatingDraftId = 0L
            },
            onSave = { c ->
                // If the user already ingested files during creation we saved a
                // draft on their behalf — reuse that id so Save updates rather
                // than inserts a second copy.
                val effective = if (creatingDraftId != 0L) c.copy(id = creatingDraftId) else c
                viewModel.upsert(effective)
                creating = false
                creatingDraftId = 0L
            },
            onIngestFolder = { snapshot, uri ->
                val target = if (creatingDraftId != 0L) snapshot.copy(id = creatingDraftId) else snapshot
                viewModel.saveAndIngestFolder(
                    case = target,
                    treeUri = uri,
                    onSaved = { savedId -> creatingDraftId = savedId },
                    onIngestStarted = {
                        creating = false
                        creatingDraftId = 0L
                    }
                )
            },
            onIngestImages = { snapshot, uris ->
                val target = if (creatingDraftId != 0L) snapshot.copy(id = creatingDraftId) else snapshot
                viewModel.saveAndIngestFiles(
                    case = target,
                    uris = uris,
                    onSaved = { savedId -> creatingDraftId = savedId },
                    onIngestStarted = {
                        creating = false
                        creatingDraftId = 0L
                    }
                )
            }
        )
    }
    editing?.let { current ->
        EditCaseDialog(
            initial = current,
            title = "Edit case",
            onDismiss = { editing = null },
            onSave = { updated ->
                viewModel.upsert(updated)
                editing = null
            },
            onIngestFolder = { snapshot, uri -> viewModel.ingestFolderInto(snapshot, uri) },
            onIngestImages = { snapshot, uris -> viewModel.ingestFilesInto(snapshot, uris) },
            onDelete = {
                pendingDelete = current
                editing = null
            }
        )
    }

    pendingDelete?.let { victim ->
        DeleteCaseConfirmDialog(
            case = victim,
            onCancel = { pendingDelete = null },
            onConfirm = {
                viewModel.delete(victim)
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun DeleteCaseConfirmDialog(
    case: CaseEntity,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Icon(Icons.Outlined.DeleteForever, null, tint = ForsetiColors.MeadAmber)
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.case_delete_confirm),
                    color = ForsetiColors.MeadAmber
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.case_delete_cancel), color = ForsetiColors.AshGrey)
            }
        },
        title = {
            Text(stringResource(R.string.case_delete_title), color = ForsetiColors.RuneGold)
        },
        text = {
            Column {
                Text(
                    case.title.ifBlank { "Untitled case" },
                    style = MaterialTheme.typography.titleMedium,
                    color = ForsetiColors.AshWhite
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.case_delete_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForsetiColors.AshGrey
                )
            }
        },
        containerColor = ForsetiColors.Surface
    )
}

@Composable
private fun EmptyCases() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "No cases yet",
            style = MaterialTheme.typography.headlineSmall,
            color = ForsetiColors.RuneGold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Create a case here or from the Deadlines tab to start organizing filings.",
            style = MaterialTheme.typography.bodyMedium,
            color = ForsetiColors.AshGrey
        )
    }
}

@Composable
private fun CaseCard(
    case: CaseEntity,
    completeness: Float,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onShowFolder: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().clickable { onOpen() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        case.title.ifBlank { "Untitled case" },
                        style = MaterialTheme.typography.titleMedium,
                        color = ForsetiColors.AshWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = listOfNotNull(
                        case.court.takeIf { it.isNotBlank() },
                        case.caseNumber.takeIf { it.isNotBlank() },
                        case.role.takeIf { it.isNotBlank() }
                    ).joinToString(" \u00B7 ")
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = ForsetiColors.AshGrey
                        )
                    }
                }
                IconButton(onClick = onShowFolder) {
                    Icon(Icons.Outlined.Folder, "Show folder", tint = ForsetiColors.RavenBlue)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, "Edit", tint = ForsetiColors.RuneGold)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.case_delete_cd),
                        tint = ForsetiColors.AshGrey
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            CompletenessBar(completeness = completeness)
        }
    }
}

@Composable
private fun CompletenessBar(completeness: Float) {
    val pct = (completeness * 100).toInt()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Profile $pct%",
                style = MaterialTheme.typography.labelLarge,
                color = if (pct >= 80) ForsetiColors.RuneGold else ForsetiColors.MeadAmber
            )
            Spacer(Modifier.width(8.dp))
            if (pct < 100) {
                Text(
                    "Fill in remaining fields for cleaner deadline defaults.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ForsetiColors.AshGrey,
                    maxLines = 2
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { completeness.coerceIn(0f, 1f) },
            color = if (pct >= 80) ForsetiColors.RuneGold else ForsetiColors.MeadAmber,
            trackColor = ForsetiColors.Stone,
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
    }
}

@Composable
private fun EditCaseDialog(
    initial: CaseEntity,
    title: String,
    onDismiss: () -> Unit,
    onSave: (CaseEntity) -> Unit,
    onIngestFolder: ((CaseEntity, Uri) -> Unit)?,
    onIngestImages: ((CaseEntity, List<Uri>) -> Unit)?,
    onDelete: (() -> Unit)? = null
) {
    var titleText by remember { mutableStateOf(initial.title) }
    var court by remember { mutableStateOf(initial.court) }
    var num by remember { mutableStateOf(initial.caseNumber) }
    var role by remember { mutableStateOf(initial.role) }
    var filedIso by remember {
        mutableStateOf(
            initial.complaintFiledAt?.let { isoFromEpoch(it) } ?: ""
        )
    }

    // Always build the case snapshot from the dialog's latest text so an ingest
    // triggered before the user clicks Save still lands the files inside *this*
    // case profile (with the title/court/number they just typed).
    fun snapshot(): CaseEntity {
        val filed = epochFromIsoOrNull(filedIso)
        return initial.copy(
            title = titleText.trim(),
            court = court.trim(),
            caseNumber = num.trim(),
            role = role.trim(),
            complaintFiledAt = filed,
            createdAt = if (initial.createdAt == 0L) System.currentTimeMillis() else initial.createdAt
        )
    }

    val context = LocalContext.current
    val scroll = rememberScrollState()

    val pickTree = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onIngestFolder?.invoke(snapshot(), it)
        }
    }

    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { IngestUriPermissions.persistUri(context, it) }
            onIngestImages?.invoke(snapshot(), uris)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(snapshot()) }) {
                Text(stringResource(R.string.action_save), color = ForsetiColors.RuneGold)
            }
        },
        dismissButton = {
            // Editing an existing case (id != 0L) gets a destructive Delete
            // button alongside Cancel. New-case dialogs never show this — there
            // is nothing to delete yet, and the user would just close instead.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null && initial.id != 0L) {
                    TextButton(onClick = onDelete) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = ForsetiColors.MeadAmber
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.case_delete_confirm),
                            color = ForsetiColors.MeadAmber
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = ForsetiColors.AshGrey)
                }
            }
        },
        title = { Text(title, color = ForsetiColors.RuneGold) },
        containerColor = ForsetiColors.Surface,
        text = {
            Column(Modifier.verticalScroll(scroll)) {
                Field("Case title", titleText) { titleText = it }
                Field("Court", court) { court = it }
                Field("Case number", num) { num = it }
                Field("Your role (Plaintiff, Defendant, Petitioner\u2026)", role) { role = it }
                Field("Complaint filed (YYYY-MM-DD)", filedIso) { filedIso = it }

                if (onIngestFolder != null && onIngestImages != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.case_ingest_section_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = ForsetiColors.RuneGold
                    )
                    Spacer(Modifier.height(6.dp))
                    Row {
                        OutlinedButton(
                            onClick = { runCatching { pickTree.launch(null) } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.DriveFolderUpload, null, tint = ForsetiColors.RuneGold)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.case_ingest_folder),
                                color = ForsetiColors.RuneGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    pickImages.launch(
                                        arrayOf("image/*", "application/pdf", "audio/*", "video/*")
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Collections, null, tint = ForsetiColors.RuneGold)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.case_ingest_images),
                                color = ForsetiColors.RuneGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.case_ingest_tip),
                        style = MaterialTheme.typography.labelSmall,
                        color = ForsetiColors.AshGrey
                    )
                }
            }
        }
    )
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        label = { Text(label) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = ForsetiColors.SurfaceVariant,
            unfocusedContainerColor = ForsetiColors.SurfaceVariant,
            focusedIndicatorColor = ForsetiColors.RuneGold,
            cursorColor = ForsetiColors.RuneGold,
            focusedLabelColor = ForsetiColors.RuneGold
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

private fun blankCase(): CaseEntity = CaseEntity(
    id = 0L,
    title = "",
    court = "",
    caseNumber = "",
    role = "",
    complaintFiledAt = null,
    createdAt = 0L
)

private fun isoFromEpoch(epochMs: Long): String {
    val ldt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    return "%04d-%02d-%02d".format(ldt.year, ldt.monthNumber, ldt.dayOfMonth)
}

private fun epochFromIsoOrNull(iso: String): Long? = runCatching {
    val parts = iso.trim().split('-')
    if (parts.size != 3) return null
    LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        .atStartOfDayIn(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()
}.getOrNull()
