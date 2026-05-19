package com.forseti.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.DriveFolderUpload
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.forseti.R
import com.forseti.data.entities.CaseEntity
import com.forseti.ui.shell.ForsetiTopBar
import com.forseti.ui.theme.ForsetiColors

/**
 * Multi-page document scanner. Capture pages with the rear camera, review the
 * thumbnails, then save them as a single PDF inside the active case workspace.
 */
@Composable
fun ScannerScreen(
    sidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val cases by viewModel.cases.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val error by viewModel.error.collectAsState()
    val ingestMessage by viewModel.ingestMessage.collectAsState()

    var selectedCaseId by remember { mutableStateOf<Long?>(null) }
    val active = cases.firstOrNull { it.id == selectedCaseId } ?: cases.firstOrNull()
    LaunchedEffect(cases) {
        if (selectedCaseId == null && active != null) selectedCaseId = active.id
    }

    var label by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(saved) {
        saved?.let { f ->
            val sub = f.parentFile?.name.orEmpty()
            val phase = f.parentFile?.parentFile?.name.orEmpty()
            val location = when {
                sub == "98_Scans" -> "98_Scans (no keyword matched — move it from Case Profile if you like)"
                phase.matches(Regex("^\\d{2}_.*")) -> "$phase / $sub"
                sub.isNotBlank() -> sub
                else -> "case workspace"
            }
            snackbar.showSnackbar("Saved ${f.name} → $location")
            viewModel.consumeSaved()
        }
    }
    LaunchedEffect(error) {
        error?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeError()
        }
    }
    LaunchedEffect(ingestMessage) {
        ingestMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeIngestMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ForsetiColors.Background)
        ) {
            ForsetiTopBar(
                title = stringResource(R.string.nav_scanner),
                sidebarExpanded = sidebarExpanded,
                onToggleSidebar = onToggleSidebar
            )

            if (cases.isEmpty()) {
                ScannerEmpty()
            } else {
                CasePickerStrip(
                    cases = cases,
                    selectedId = active?.id,
                    onSelect = { selectedCaseId = it.id }
                )
                HorizontalDivider(color = ForsetiColors.Stone)
                // Everything below the case picker scrolls together so the Save
                // button is always reachable on small phones / when many pages
                // are queued. Camera pane uses a fixed height (320.dp) instead
                // of an aspect ratio so it doesn't push the rest off-screen.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    CameraPane(
                        onCaptured = { bm -> viewModel.addPage(bm) },
                        onIngestFolder = { uri ->
                            active?.let { viewModel.ingestFolderInto(it, uri) }
                        },
                        onIngestFiles = { uris ->
                            active?.let { viewModel.ingestFilesInto(it, uris) }
                        },
                        canIngest = active != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    )
                    PageStrip(
                        pages = pages,
                        onRemove = { viewModel.removePage(it) }
                    )
                    ScannerActions(
                        label = label,
                        onLabelChange = { label = it },
                        canSave = pages.isNotEmpty() && active != null,
                        onSave = {
                            active?.let { viewModel.save(it, label.trim().ifBlank { null }) }
                            label = ""
                        },
                        onClear = { viewModel.clear() }
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ScannerEmpty() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Add a case first",
            style = MaterialTheme.typography.headlineSmall,
            color = ForsetiColors.RuneGold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Scanned pages are saved into a case workspace folder. Create a case under Deadlines or Case Profile and come back.",
            style = MaterialTheme.typography.bodyMedium,
            color = ForsetiColors.AshGrey
        )
    }
}

@Composable
private fun CasePickerStrip(
    cases: List<CaseEntity>,
    selectedId: Long?,
    onSelect: (CaseEntity) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        itemsIndexed(cases, key = { _, c -> c.id }) { _, case ->
            val selected = case.id == selectedId
            AssistChip(
                onClick = { onSelect(case) },
                label = {
                    Text(
                        case.title.ifBlank { "Case ${case.id}" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selected) ForsetiColors.SidebarSelected else ForsetiColors.Surface,
                    labelColor = if (selected) ForsetiColors.RuneGold else ForsetiColors.AshWhite
                ),
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun CameraPane(
    onCaptured: (Bitmap) -> Unit,
    onIngestFolder: (Uri) -> Unit,
    onIngestFiles: (List<Uri>) -> Unit,
    canIngest: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )
    val pickTree = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { onIngestFolder(it) } }

    val pickFiles = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> if (uris.isNotEmpty()) onIngestFiles(uris) }

    LaunchedEffect(Unit) {
        if (!hasPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    var ingestMenu by remember { mutableStateOf(false) }

    DisposableEffect(hasPermission) {
        if (hasPermission) {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                }
            }, ContextCompat.getMainExecutor(context))
        }
        onDispose {}
    }

    Box(
        modifier = modifier
            .background(ForsetiColors.SplashBlack)
    ) {
        if (hasPermission) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        capture(imageCapture, context) { onCaptured(it) }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForsetiColors.RuneGold,
                        contentColor = ForsetiColors.SplashBlack
                    )
                ) {
                    Icon(Icons.Outlined.PhotoCamera, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Capture page")
                }
                Spacer(Modifier.width(8.dp))
                Box {
                    IconButton(
                        onClick = { if (canIngest) ingestMenu = true },
                        enabled = canIngest
                    ) {
                        Icon(
                            Icons.Outlined.UploadFile,
                            contentDescription = stringResource(R.string.scanner_ingest_menu_cd),
                            tint = if (canIngest) ForsetiColors.RuneGold else ForsetiColors.AshGrey
                        )
                    }
                    DropdownMenu(
                        expanded = ingestMenu,
                        onDismissRequest = { ingestMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.case_ingest_folder),
                                    color = ForsetiColors.AshWhite
                                )
                            },
                            onClick = {
                                ingestMenu = false
                                runCatching { pickTree.launch(null) }
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.DriveFolderUpload, null, tint = ForsetiColors.RuneGold)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.case_ingest_images),
                                    color = ForsetiColors.AshWhite
                                )
                            },
                            onClick = {
                                ingestMenu = false
                                runCatching {
                                    pickFiles.launch(
                                        arrayOf("image/*", "application/pdf", "audio/*", "video/*")
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Collections, null, tint = ForsetiColors.RuneGold)
                            }
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Camera permission is required.",
                    color = ForsetiColors.AshWhite,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant permission")
                }
            }
        }
    }
}

@Composable
private fun PageStrip(pages: List<Bitmap>, onRemove: (Int) -> Unit) {
    if (pages.isEmpty()) {
        Text(
            "No pages captured yet.",
            color = ForsetiColors.AshGrey,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    LazyRow(contentPadding = PaddingValues(12.dp)) {
        itemsIndexed(pages) { index, bm ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(96.dp, 128.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = bm.asImageBitmap(),
                        contentDescription = "Page ${index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { onRemove(index) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(28.dp)
                            .background(ForsetiColors.SplashBlack.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            "Remove",
                            tint = ForsetiColors.AshWhite
                        )
                    }
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForsetiColors.RuneGold,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerActions(
    label: String,
    onLabelChange: (String) -> Unit,
    canSave: Boolean,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Tip: include words like \"complaint\", \"answer\", \"motion\", \"discovery\", " +
                "\"trial\", \"order\", or \"exhibit\" in the label and Forseti will auto-file the PDF " +
                "into the matching phase folder. No keyword? It lands in 98_Scans for you to move.",
            style = MaterialTheme.typography.bodySmall,
            color = ForsetiColors.AshGrey,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = label,
            onValueChange = onLabelChange,
            singleLine = true,
            label = { Text("Filename label (optional)") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = ForsetiColors.SurfaceVariant,
                unfocusedContainerColor = ForsetiColors.SurfaceVariant,
                focusedIndicatorColor = ForsetiColors.RuneGold,
                cursorColor = ForsetiColors.RuneGold,
                focusedLabelColor = ForsetiColors.RuneGold
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row {
            Button(
                onClick = onSave,
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForsetiColors.RuneGold,
                    contentColor = ForsetiColors.SplashBlack
                )
            ) {
                Icon(Icons.Outlined.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Save PDF to case")
            }
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = onClear) {
                Text("Clear all", color = ForsetiColors.AshGrey)
            }
        }
    }
}

private fun capture(
    imageCapture: ImageCapture,
    context: android.content.Context,
    onResult: (Bitmap) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = jpegToBitmap(image)
                image.close()
                if (bitmap != null) onResult(bitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                // Surface failures via snackbar in caller via error flow when wired.
            }
        }
    )
}

private fun jpegToBitmap(image: ImageProxy): Bitmap? {
    val plane = image.planes.firstOrNull() ?: return null
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

