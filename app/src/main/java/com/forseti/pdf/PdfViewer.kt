package com.forseti.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.forseti.ui.theme.ForsetiColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * FRCP PDF viewer: [LazyColumn] of rasterized pages.
 *
 * - **Pinch-zoom** updates shared scale only when [detectTransformGestures] reports a real scale
 *   change (`zoomChange != 1f`), which avoids treating pure drags as zoom.
 * - **One-finger vertical drag** should scroll the list for next/previous pages; pinch uses two
 *   fingers. (If a device/OS build routes single-finger drags into the transform detector,
 *   zoom out slightly so the list can scroll.)
 */
@Composable
fun PdfViewer(
    renderer: PdfRenderer,
    pageCount: Int,
    jumpTarget: StateFlow<Int>,
    onPageChange: (Int) -> Unit,
    renderMutex: Mutex,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    onPageLongPress: (Int) -> Unit = {}
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    val zoomRef = rememberUpdatedState(zoom)

    // rememberLazyListState already restores firstVisibleItemIndex on its own
    // via rememberSaveable, but we also seed initialPage from the viewmodel so
    // that the *very first* composition (e.g. after rotation, when the saver
    // hasn't restored anything yet because the activity was destroyed and
    // recreated) lands on the page the user was reading.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage.coerceAtLeast(0))

    // Skip the StateFlow's initial replay. Without this, a rotation re-runs the
    // LaunchedEffect with the StateFlow's current value (typically 0 if the
    // user never tapped the Quick-Jump TOC) and slams the viewer back to page 1,
    // erasing the saved scroll position. drop(1) ensures only *new* jumps
    // (TOC taps after init) ever trigger scrollToItem.
    LaunchedEffect(jumpTarget) {
        jumpTarget.drop(1).collect { target ->
            if (target in 0 until pageCount) listState.scrollToItem(target)
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { onPageChange(it) }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    if (zoomChange != 1f) {
                        zoom = (zoomRef.value * zoomChange).coerceIn(0.5f, 5f)
                    }
                }
            }
    ) {
        items(pageCount, key = { it }) { pageIndex ->
            PdfPage(
                renderer = renderer,
                pageIndex = pageIndex,
                zoom = zoom,
                renderMutex = renderMutex,
                onLongPressOrDoubleTap = { onPageLongPress(pageIndex) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun PdfPage(
    renderer: PdfRenderer,
    pageIndex: Int,
    zoom: Float,
    renderMutex: Mutex,
    onLongPressOrDoubleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.roundToPx().coerceAtLeast(360) }
        val maxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)

        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(pageIndex, widthPx) {
            bitmap = null
            bitmap = withContext(Dispatchers.IO) {
                renderMutex.withLock { renderPage(renderer, pageIndex, widthPx) }
            }
        }

        val bm = bitmap
        if (bm != null) {
            val aspect = bm.height.toFloat() / bm.width.toFloat()
            val baseH = maxW * aspect
            val scaledH = baseH * zoom

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { scaledH.toDp() })
                    .clip(RoundedCornerShape(6.dp))
                    .background(ForsetiColors.AshWhite)
                    .pointerInput(onLongPressOrDoubleTap) {
                        detectTapGestures(
                            onLongPress = { onLongPressOrDoubleTap() },
                            onDoubleTap = { onLongPressOrDoubleTap() }
                        )
                    }
            ) {
                Image(
                    bitmap = bm.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(density) { baseH.toDp() })
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading page ${pageIndex + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForsetiColors.AshGrey
                )
            }
        }
    }
}

private fun renderPage(
    renderer: PdfRenderer,
    pageIndex: Int,
    targetWidthPx: Int
): Bitmap? = try {
    val page = renderer.openPage(pageIndex)
    try {
        val ratio = page.height.toFloat() / page.width.toFloat()
        val w = targetWidthPx
        val h = (w * ratio).toInt().coerceAtLeast(1)
        val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bm
    } finally {
        runCatching { page.close() }
    }
} catch (_: Throwable) {
    null
}
