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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forseti.R
import com.forseti.drafts.DraftCatalog
import com.forseti.drafts.DraftDoc
import com.forseti.drafts.DraftPrinting
import com.forseti.drafts.DraftSource
import com.forseti.ocr.OcrCaptureScreen
import com.forseti.ocr.OcrReviewSheet
import com.forseti.ocr.OcrFieldMapper
import com.forseti.ocr.RecognizedBlock
import com.forseti.ui.shell.ForsetiTopBar
import com.forseti.ui.theme.ForsetiColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DraftsScreen(
    sidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    viewModel: DraftsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val byCategory = remember(context) { DraftCatalog.byCategory(context) }
    val scope = rememberCoroutineScope()
    var pendingShare by remember { mutableStateOf<DraftDoc?>(null) }
    var pendingPrint by remember { mutableStateOf<DraftDoc?>(null) }
    var ocrTarget by remember { mutableStateOf<DraftDoc?>(null) }
    var ocrBlocks by remember { mutableStateOf<List<RecognizedBlock>?>(null) }
    var working by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(pendingShare) {
        val doc = pendingShare ?: return@LaunchedEffect
        working = true
        val outcome = withContext(Dispatchers.IO) { viewModel.materialize(doc) }
        working = false
        when (outcome) {
            is DraftMaterialization.Success ->
                runCatching {
                    context.startActivity(DraftPrinting.shareChooser(context, outcome.file, doc.title))
                }.onFailure {
                    snackbar.showSnackbar("Could not open share sheet: ${it.message}")
                }
            is DraftMaterialization.Error -> snackbar.showSnackbar(outcome.message)
        }
        pendingShare = null
    }

    LaunchedEffect(pendingPrint) {
        val doc = pendingPrint ?: return@LaunchedEffect
        working = true
        val outcome = withContext(Dispatchers.IO) { viewModel.materialize(doc) }
        working = false
        when (outcome) {
            is DraftMaterialization.Success ->
                runCatching {
                    DraftPrinting.print(context, outcome.file, doc.title)
                }.onFailure {
                    snackbar.showSnackbar("Could not start print / Save as PDF: ${it.message}")
                }
            is DraftMaterialization.Error -> snackbar.showSnackbar(outcome.message)
        }
        pendingPrint = null
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ForsetiColors.Background)
        ) {
            ForsetiTopBar(
                title = stringResource(R.string.nav_drafts),
                sidebarExpanded = sidebarExpanded,
                onToggleSidebar = onToggleSidebar
            )
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                byCategory.forEach { (category, docs) ->
                    item(key = "cat_${category.title}") {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = ForsetiColors.RuneGold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                        )
                    }
                    items(docs, key = { it.id }) { doc ->
                        DraftCard(
                            doc = doc,
                            onPrint = { pendingPrint = doc },
                            onShare = { pendingShare = doc },
                            onCapture = if (doc.source is DraftSource.Generated) {
                                { ocrTarget = doc }
                            } else null
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (working) {
        WorkingOverlay()
    }

    if (ocrTarget != null && ocrBlocks == null) {
        OcrCaptureScreen(
            onBlocks = { ocrBlocks = it },
            onCancel = { ocrTarget = null }
        )
    }
    val target = ocrTarget
    val blocks = ocrBlocks
    if (target != null && blocks != null) {
        OcrReviewSheet(
            blocks = blocks,
            initial = OcrFieldMapper.emptyAssignments(),
            onDismiss = { ocrTarget = null; ocrBlocks = null },
            onConfirm = { assignments ->
                val prefill = OcrFieldMapper.toPrefill(assignments)
                ocrTarget = null
                ocrBlocks = null
                scope.launch {
                    working = true
                    val outcome = withContext(Dispatchers.IO) {
                        viewModel.materializeWithPrefill(target, prefill)
                    }
                    working = false
                    when (outcome) {
                        is DraftMaterialization.Success ->
                            runCatching {
                                DraftPrinting.print(context, outcome.file, target.title)
                            }.onFailure {
                                snackbar.showSnackbar("Could not start print / Save as PDF: ${it.message}")
                            }
                        is DraftMaterialization.Error -> snackbar.showSnackbar(outcome.message)
                    }
                }
            }
        )
    }
}

@Composable
private fun WorkingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0x80000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = ForsetiColors.Surface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = ForsetiColors.RuneGold,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.height(20.dp).width(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.drafts_building_pdf), color = ForsetiColors.AshWhite)
            }
        }
    }
}

@Composable
private fun DraftCard(
    doc: DraftDoc,
    onPrint: () -> Unit,
    onShare: () -> Unit,
    onCapture: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Surface(
                color = ForsetiColors.Charcoal,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(40.dp).height(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (doc.source) {
                            is DraftSource.Bundled -> Icons.Outlined.Description
                            is DraftSource.Generated -> Icons.AutoMirrored.Outlined.NoteAdd
                        },
                        contentDescription = null,
                        tint = ForsetiColors.RuneGold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(doc.title, style = MaterialTheme.typography.titleMedium, color = ForsetiColors.AshWhite)
                Text(
                    text = doc.ruleCitation,
                    style = MaterialTheme.typography.labelSmall,
                    color = ForsetiColors.MeadAmber,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = doc.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ForsetiColors.AshGrey,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = onPrint,
                        label = { Text(stringResource(R.string.drafts_print)) },
                        leadingIcon = { Icon(Icons.Outlined.Print, null, modifier = Modifier.height(18.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = ForsetiColors.SurfaceVariant,
                            labelColor = ForsetiColors.AshWhite,
                            leadingIconContentColor = ForsetiColors.RuneGold
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onShare) {
                        Icon(Icons.Outlined.Share, null, tint = ForsetiColors.RavenBlue)
                        Spacer(Modifier.width(6.dp))
                        Text("Share", color = ForsetiColors.RavenBlue)
                    }
                    if (onCapture != null) {
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = onCapture) {
                            Icon(Icons.Outlined.CameraAlt, null, tint = ForsetiColors.MeadAmber)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.drafts_capture_fill), color = ForsetiColors.MeadAmber)
                        }
                    }
                }
            }
        }
    }
}
