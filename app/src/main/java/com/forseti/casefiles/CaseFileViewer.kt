package com.forseti.casefiles

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.forseti.LocalTts
import com.forseti.pdf.LocalPdfReader
import com.forseti.tts.ReadAloudControls
import com.forseti.ui.theme.ForsetiColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Generic in-app file viewer for the case workspace. Picks the right reader
 * based on extension/mime:
 *   • `.pdf`            → [LocalPdfReader] (existing PdfRenderer-backed surface).
 *   • Images            → pinch-zoomable [Image] preview.
 *   • Text / Markdown   → scrollable, copyable [SelectionContainer] + read-aloud.
 *   • Anything else     → an "Open in another app" fallback that fires an
 *                         `ACTION_VIEW` intent through [FileProvider].
 */
@Composable
fun CaseFileViewer(
    file: File,
    folderService: CaseFolderService,
    onBack: () -> Unit
) {
    val ext = file.extension.lowercase(Locale.US)
    val mime = mimeFor(ext)
    val context = LocalContext.current

    when {
        mime == "application/pdf" -> LocalPdfReader(file = file, title = file.name, onClose = onBack)
        mime.startsWith("image/") -> ImagePreview(file = file, onBack = onBack, onShare = {
            shareFile(context, folderService, file)
        })
        ext == "docx" -> TextPreview(
            file = file,
            onBack = onBack,
            onShare = { shareFile(context, folderService, file) },
            loader = { f ->
                DocxReader.extractText(f)
                    ?: "(Could not read this .docx file. Try \"Open in another app\".)"
            }
        )
        mime.startsWith("text/") || ext in TEXT_EXTS -> TextPreview(file = file, onBack = onBack, onShare = {
            shareFile(context, folderService, file)
        })
        else -> UnknownFile(file = file, mime = mime, onBack = onBack, onOpenExternal = {
            openExternal(context, folderService, file, mime)
        })
    }
}

@Composable
private fun ImagePreview(file: File, onBack: () -> Unit, onShare: () -> Unit) {
    var zoom by remember { mutableFloatStateOf(1f) }
    val zoomRef = rememberUpdatedState(zoom)

    val bitmap = remember(file.absolutePath) {
        runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }

    Column(modifier = Modifier.fillMaxSize().background(ForsetiColors.Background)) {
        ViewerTopBar(title = file.name, onBack = onBack) {
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, "Share", tint = ForsetiColors.AshWhite)
            }
        }
        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ForsetiColors.SplashBlack)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoomChange, _ ->
                            if (zoomChange != 1f) {
                                zoom = (zoomRef.value * zoomChange).coerceIn(0.5f, 6f)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = file.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Couldn’t decode this image.",
                    color = ForsetiColors.AshGrey,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TextPreview(
    file: File,
    onBack: () -> Unit,
    onShare: () -> Unit,
    loader: suspend (File) -> String = DEFAULT_TEXT_LOADER
) {
    val tts = LocalTts.current
    var body by remember(file.absolutePath) { mutableStateOf<String?>(null) }
    val scroll = rememberScrollState()
    LaunchedEffect(file.absolutePath) {
        body = withContext(Dispatchers.IO) {
            runCatching { loader(file) }.getOrDefault("(Could not read file)")
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(ForsetiColors.Background)) {
        ViewerTopBar(title = file.name, onBack = onBack) {
            ReadAloudControls(tts = tts, fetchText = { body.orEmpty() })
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, "Share", tint = ForsetiColors.AshWhite)
            }
        }
        SelectionContainer(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scroll)) {
            Text(
                text = body ?: "Loading…",
                color = ForsetiColors.AshWhite,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun UnknownFile(file: File, mime: String, onBack: () -> Unit, onOpenExternal: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(ForsetiColors.Background)) {
        ViewerTopBar(title = file.name, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.Description,
                null,
                tint = ForsetiColors.RuneGold,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.size(12.dp))
            Text(
                "Forseti can’t preview this kind of file in-app.",
                style = MaterialTheme.typography.titleMedium,
                color = ForsetiColors.AshWhite
            )
            Spacer(Modifier.size(4.dp))
            Text(
                "Type: $mime",
                style = MaterialTheme.typography.bodySmall,
                color = ForsetiColors.AshGrey
            )
            Spacer(Modifier.size(16.dp))
            Button(
                onClick = onOpenExternal,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForsetiColors.RuneGold,
                    contentColor = ForsetiColors.SplashBlack
                )
            ) {
                Icon(Icons.Outlined.OpenInBrowser, null)
                Spacer(Modifier.size(6.dp))
                Text("Open in another app")
            }
        }
    }
}

@Composable
private fun ViewerTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ForsetiColors.Surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Close", tint = ForsetiColors.AshWhite)
        }
        Spacer(Modifier.width(4.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = ForsetiColors.AshWhite,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        actions()
    }
}

private fun shareFile(context: android.content.Context, folders: CaseFolderService, file: File) {
    val uri = folders.shareableUri(file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeFor(file.extension.lowercase(Locale.US))
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
}

private fun openExternal(
    context: android.content.Context,
    folders: CaseFolderService,
    file: File,
    mime: String
) {
    val uri = folders.shareableUri(file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Open ${file.name}"))
    }
}

private val TEXT_EXTS = setOf("txt", "md", "rtf", "log", "json", "csv", "yml", "yaml", "xml", "html", "htm")

private val DEFAULT_TEXT_LOADER: suspend (File) -> String = { it.readText() }

/**
 * Maps file extensions to a best-guess MIME type. The MIME drives two
 * things: (1) which in-app viewer the [CaseFileViewer] picks, and (2) which
 * external apps Android offers when we fall back to ACTION_VIEW for formats
 * we can't render natively. Office formats (.docx, .xlsx, .pptx, .odt, …)
 * MUST have their correct OOXML/ODF mime here or the Open-in-another-app
 * sheet shows generic "no apps" instead of Docs/Word/WPS/etc.
 */
private fun mimeFor(ext: String): String = when (ext) {
    "pdf" -> "application/pdf"
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "webp" -> "image/webp"
    "gif" -> "image/gif"
    "bmp" -> "image/bmp"
    "heic", "heif" -> "image/heic"
    "txt", "log" -> "text/plain"
    "md" -> "text/markdown"
    "rtf" -> "application/rtf"
    "json" -> "application/json"
    "csv" -> "text/csv"
    "html", "htm" -> "text/html"
    "xml" -> "text/xml"
    "yml", "yaml" -> "text/yaml"
    "doc" -> "application/msword"
    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    "xls" -> "application/vnd.ms-excel"
    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    "ppt" -> "application/vnd.ms-powerpoint"
    "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    "odt" -> "application/vnd.oasis.opendocument.text"
    "ods" -> "application/vnd.oasis.opendocument.spreadsheet"
    "odp" -> "application/vnd.oasis.opendocument.presentation"
    "epub" -> "application/epub+zip"
    "zip" -> "application/zip"
    "mp3" -> "audio/mpeg"
    "wav" -> "audio/wav"
    "m4a" -> "audio/mp4"
    "ogg" -> "audio/ogg"
    "mp4" -> "video/mp4"
    "mov" -> "video/quicktime"
    "mkv" -> "video/x-matroska"
    "webm" -> "video/webm"
    else -> "application/octet-stream"
}
