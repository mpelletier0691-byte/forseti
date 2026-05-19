package com.forseti.pdf

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.forseti.LocalTts
import com.forseti.R
import com.forseti.tts.PageOcr
import com.forseti.tts.ReadAloudControls
import com.forseti.ui.theme.ForsetiColors
import com.forseti.util.copyPlainText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Standalone full-screen reader for an arbitrary File (uploaded rule PDF,
 * imported case PDF, etc.). Reuses the same [PdfViewer] used for the bundled
 * FRCP — pinch-zoom, vertical scrolling, page tracking — so the UX is
 * identical to the Quick Jump experience.
 */
@Composable
fun LocalPdfReader(
    file: File,
    title: String,
    onClose: () -> Unit
) {
    val pfdState = remember(file.absolutePath) {
        runCatching { ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) }
            .getOrNull()
    }
    val rendererState = remember(file.absolutePath) {
        pfdState?.let { runCatching { PdfRenderer(it) }.getOrNull() }
    }
    val mutex = remember { Mutex() }
    val jumpFlow = remember { MutableStateFlow(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // rememberSaveable so the page survives rotation / activity recreation.
    var currentPage by androidx.compose.runtime.saveable.rememberSaveable(file.absolutePath) {
        mutableIntStateOf(0)
    }

    var sheetPage by remember { mutableStateOf<Int?>(null) }
    var sheetLoading by remember { mutableStateOf(false) }
    var sheetBody by remember { mutableStateOf("") }

    LaunchedEffect(sheetPage, file.absolutePath) {
        val p = sheetPage ?: return@LaunchedEffect
        val r = rendererState ?: return@LaunchedEffect
        sheetLoading = true
        sheetBody = ""
        sheetBody = withContext(Dispatchers.IO) {
            PageOcr.extractDisplayText(r, p, mutex)
        }
        sheetLoading = false
    }

    DisposableEffect(file.absolutePath) {
        onDispose {
            runCatching { rendererState?.close() }
            runCatching { pfdState?.close() }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(ForsetiColors.Background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ForsetiColors.Surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Close", tint = ForsetiColors.AshWhite)
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ForsetiColors.AshWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val pages = rendererState?.pageCount ?: 0
                if (pages > 0) {
                    Text(
                        "Page ${currentPage + 1} of $pages",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForsetiColors.AshGrey
                    )
                }
            }
            val tts = LocalTts.current
            val activeRenderer = rendererState
            if (activeRenderer != null && (activeRenderer.pageCount) > 0) {
                ReadAloudControls(
                    tts = tts,
                    fetchText = {
                        PageOcr.extractText(activeRenderer, currentPage, mutex)
                    },
                    iconTint = ForsetiColors.AshWhite
                )
                IconButton(
                    onClick = { sheetPage = currentPage }
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Notes,
                        contentDescription = stringResource(R.string.cd_pdf_page_text),
                        tint = ForsetiColors.AshWhite
                    )
                }
                IconButton(
                    onClick = {
                        scope.launch {
                            val text = withContext(Dispatchers.IO) {
                                PageOcr.extractText(activeRenderer, currentPage, mutex)
                            }
                            if (text.isBlank()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.quick_jump_copy_no_text),
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                context.copyPlainText(title, text)
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.cd_copy_page_text),
                        tint = ForsetiColors.AshWhite
                    )
                }
            }
        }

        val renderer = rendererState
        val pageCount = renderer?.pageCount ?: 0
        if (renderer != null && pageCount > 0) {
            PdfViewer(
                renderer = renderer,
                pageCount = pageCount,
                jumpTarget = jumpFlow.asStateFlow(),
                onPageChange = { currentPage = it },
                renderMutex = mutex,
                modifier = Modifier.fillMaxSize(),
                initialPage = currentPage,
                onPageLongPress = { sheetPage = it }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(
                        "Couldn’t open this PDF.",
                        style = MaterialTheme.typography.titleMedium,
                        color = ForsetiColors.RuneGold
                    )
                    Text(
                        "The file may be encrypted, corrupted, or not a PDF.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForsetiColors.AshGrey
                    )
                }
            }
        }

        PdfPageTextDialog(
            visible = sheetPage != null,
            pageNumberOneBased = (sheetPage ?: 0) + 1,
            loading = sheetLoading,
            body = sheetBody,
            onDismiss = { sheetPage = null },
            tts = LocalTts.current
        )
    }
}
