package com.forseti.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forseti.LocalTts
import com.forseti.R
import com.forseti.pdf.PdfViewModel
import com.forseti.pdf.PdfPageTextDialog
import com.forseti.pdf.PdfViewer
import com.forseti.pdf.TocEntry
import com.forseti.tts.PageOcr
import com.forseti.tts.ReadAloudControls
import com.forseti.ui.shell.ForsetiTopBar
import com.forseti.ui.theme.ForsetiColors
import com.forseti.util.copyPlainText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main reading surface. Shows the FRCP PDF in the right pane and a Quick-Jump
 * TOC in a slide-down panel triggered from the top bar.
 *
 * Tabbed/expanded panels expand into this main pane (per the spec) instead of
 * popping a new screen.
 */
@Composable
fun QuickJumpScreen(
    sidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    viewModel: PdfViewModel = hiltViewModel(),
    notesViewModel: NotesViewModel = hiltViewModel()
) {
    val toc by viewModel.toc.collectAsState()
    val pageCount by viewModel.pageCount.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val query by viewModel.query.collectAsState()
    val jumpTarget = viewModel.jumpTarget
    val bookmarks by notesViewModel.bookmarks.collectAsState()
    val tts = LocalTts.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sheetPage by remember { mutableStateOf<Int?>(null) }
    var sheetLoading by remember { mutableStateOf(false) }
    var sheetBody by remember { mutableStateOf("") }

    LaunchedEffect(sheetPage) {
        val p = sheetPage ?: return@LaunchedEffect
        sheetLoading = true
        sheetBody = ""
        // Prefer the bundled per-page text sidecar (frcp_2024.pages.txt). It is
        // produced from the PDF's text layer at build time, so it's accurate,
        // instant, and immune to ML Kit's empty-result bug on dense legal type.
        val fromSidecar = viewModel.repository.pageText(p)
        sheetBody = if (!fromSidecar.isNullOrBlank()) {
            fromSidecar
        } else {
            val r = viewModel.repository.renderer()
            if (r == null) "" else withContext(Dispatchers.IO) {
                PageOcr.extractDisplayText(r, p, viewModel.repository.renderMutex)
            }
        }
        sheetLoading = false
    }

    var quickJumpOpen by remember { mutableStateOf(false) }
    var stickyOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        val anchor = "frcp.p${currentPage + 1}"
        val isBookmarked = remember(bookmarks, anchor) {
            bookmarks.any { it.ruleAnchor == anchor }
        }
        ForsetiTopBar(
            title = stringResource(R.string.nav_quick_jump),
            sidebarExpanded = sidebarExpanded,
            onToggleSidebar = onToggleSidebar,
            actions = {
                // Read-aloud, copy, bookmark, and page-text (OCR) stay available even
                // when the sidebar is open — hiding them made the feature look broken.
                if (pageCount > 0) {
                    ReadAloudControls(
                        tts = tts,
                        fetchText = {
                            // Sidecar-first: text-layer extract beats OCR every time
                            // for the bundled FRCP PDF.
                            viewModel.repository.pageText(currentPage)?.takeIf { it.isNotBlank() }
                                ?.let { return@ReadAloudControls it }
                            val renderer = viewModel.repository.renderer() ?: return@ReadAloudControls ""
                            PageOcr.extractText(
                                renderer = renderer,
                                pageIndex = currentPage,
                                renderMutex = viewModel.repository.renderMutex
                            )
                        },
                        iconTint = ForsetiColors.AshWhite
                    )
                    IconButton(
                        onClick = {
                            val renderer = viewModel.repository.renderer()
                            if (renderer == null) {
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.quick_jump_copy_no_renderer),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                sheetPage = currentPage
                            }
                        }
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
                                val sidecar = viewModel.repository.pageText(currentPage)
                                if (!sidecar.isNullOrBlank()) {
                                    context.copyPlainText(
                                        label = "FRCP page ${currentPage + 1}",
                                        text = sidecar
                                    )
                                    return@launch
                                }
                                val renderer = viewModel.repository.renderer()
                                if (renderer == null) {
                                    android.widget.Toast.makeText(
                                        context,
                                        context.getString(R.string.quick_jump_copy_no_renderer),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }
                                val text = withContext(Dispatchers.IO) {
                                    PageOcr.extractText(
                                        renderer = renderer,
                                        pageIndex = currentPage,
                                        renderMutex = viewModel.repository.renderMutex
                                    )
                                }
                                if (text.isBlank()) {
                                    sheetPage = currentPage
                                    android.widget.Toast.makeText(
                                        context,
                                        context.getString(R.string.quick_jump_copy_try_ocr_sheet),
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    context.copyPlainText(
                                        label = "FRCP page ${currentPage + 1}",
                                        text = text
                                    )
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
                    IconButton(onClick = {
                        notesViewModel.toggleBookmark(anchor, "FRCP page ${currentPage + 1}")
                    }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark page",
                            tint = if (isBookmarked) ForsetiColors.RuneGold else ForsetiColors.AshWhite
                        )
                    }
                }
                IconButton(onClick = {
                    stickyOpen = !stickyOpen
                    if (stickyOpen) quickJumpOpen = false
                }) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = "Quick note",
                        tint = if (stickyOpen) ForsetiColors.RuneGold else ForsetiColors.AshWhite
                    )
                }
                IconButton(onClick = {
                    quickJumpOpen = !quickJumpOpen
                    if (quickJumpOpen) stickyOpen = false
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = "Toggle Quick Jump",
                        tint = if (quickJumpOpen) ForsetiColors.RuneGold else ForsetiColors.AshWhite
                    )
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            val renderer = viewModel.repository.renderer()
            if (renderer != null && pageCount > 0) {
                PdfViewer(
                    renderer = renderer,
                    pageCount = pageCount,
                    jumpTarget = jumpTarget,
                    onPageChange = viewModel::onScrollChanged,
                    renderMutex = viewModel.repository.renderMutex,
                    modifier = Modifier.fillMaxSize(),
                    initialPage = currentPage,
                    onPageLongPress = { sheetPage = it }
                )
            } else {
                AssetMissingState(modifier = Modifier.fillMaxSize())
            }

            if (quickJumpOpen) {
                QuickJumpPanel(
                    toc = toc,
                    query = query,
                    onQuery = viewModel::setQuery,
                    onSelect = {
                        viewModel.jumpTo(it)
                        quickJumpOpen = false
                    },
                    onDismiss = { quickJumpOpen = false }
                )
            }

            if (stickyOpen) {
                StickyNotePanel(
                    pageNumber = currentPage + 1,
                    onSave = { body ->
                        if (body.isNotBlank()) {
                            notesViewModel.upsertNote(
                                anchor = "frcp.p${currentPage + 1}.${System.currentTimeMillis()}",
                                body = body.trim()
                            )
                        }
                        stickyOpen = false
                    },
                    onDone = { stickyOpen = false }
                )
            }

            PdfPageTextDialog(
                visible = sheetPage != null,
                pageNumberOneBased = (sheetPage ?: 0) + 1,
                loading = sheetLoading,
                body = sheetBody,
                onDismiss = { sheetPage = null },
                tts = tts
            )

            if (pageCount > 0) {
                PageCounterChip(
                    page = currentPage + 1,
                    total = pageCount,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickJumpPanel(
    toc: List<TocEntry>,
    query: String,
    onQuery: (String) -> Unit,
    onSelect: (TocEntry) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = ForsetiColors.SurfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                placeholder = { Text("Search rules e.g. \"Rule 12(b)(6)\"") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ForsetiColors.Surface,
                    unfocusedContainerColor = ForsetiColors.Surface,
                    focusedIndicatorColor = ForsetiColors.RuneGold,
                    cursorColor = ForsetiColors.RuneGold
                )
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = ForsetiColors.Stone)
            val expanded = remember { mutableStateMapOf<String, Boolean>() }
            val filtered = remember(toc, query) { filterToc(toc, query) }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                tocItems(
                    entries = filtered,
                    expanded = { expanded[it.anchor] ?: (query.isNotBlank()) },
                    onToggleExpand = { expanded[it.anchor] = !(expanded[it.anchor] ?: (query.isNotBlank())) },
                    onSelect = onSelect
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelLarge,
                    color = ForsetiColors.RuneGold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.tocItems(
    entries: List<TocEntry>,
    expanded: (TocEntry) -> Boolean,
    onToggleExpand: (TocEntry) -> Unit,
    onSelect: (TocEntry) -> Unit
) {
    entries.forEach { entry ->
        item(key = entry.anchor) {
            TocRow(entry = entry, expanded = expanded(entry), onToggleExpand = { onToggleExpand(entry) }, onSelect = onSelect)
        }
        if (expanded(entry) && entry.children.isNotEmpty()) {
            tocItems(entry.children, expanded, onToggleExpand, onSelect)
        }
    }
}

@Composable
private fun TocRow(
    entry: TocEntry,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelect: (TocEntry) -> Unit
) {
    val indent = (entry.depth * 16).dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onSelect(entry) }
            .padding(start = 8.dp + indent, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (entry.children.isNotEmpty()) {
            IconButton(onClick = onToggleExpand, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = ForsetiColors.AshGrey
                )
            }
        } else {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = ForsetiColors.AshGrey,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = entry.title,
            style = if (entry.depth == 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (entry.depth == 0) ForsetiColors.RuneGold else ForsetiColors.AshWhite,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "p ${entry.page}",
            style = MaterialTheme.typography.labelSmall,
            color = ForsetiColors.AshGrey
        )
    }
}

/**
 * A pop-out sticky note overlay. Tap the sticky-note icon in the top bar →
 * a small notepad surface drops in beside the PDF. The user types, then:
 *   • Save  → persists to the Notes tab anchored to the current page, collapses.
 *   • Done  → discards the draft and collapses.
 *
 * Per the spec the notepad always opens blank: this is a quick capture surface,
 * not an editor for past notes (those live in the Notes tab).
 */
@Composable
private fun StickyNotePanel(
    pageNumber: Int,
    onSave: (String) -> Unit,
    onDone: () -> Unit
) {
    var body by remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, end = 12.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Surface(
            color = ForsetiColors.MeadAmber.copy(alpha = 0.96f),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .padding(8.dp)
                .width(320.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Quick note · page $pageNumber",
                    style = MaterialTheme.typography.titleMedium,
                    color = ForsetiColors.SplashBlack
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = { Text("Capture a thought, citation, or follow-up…") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ForsetiColors.AshWhite,
                        unfocusedContainerColor = ForsetiColors.AshWhite,
                        focusedIndicatorColor = ForsetiColors.RuneGold,
                        cursorColor = ForsetiColors.RuneGold,
                        focusedTextColor = ForsetiColors.SplashBlack,
                        unfocusedTextColor = ForsetiColors.SplashBlack
                    )
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDone) {
                        Text("Done", color = ForsetiColors.SplashBlack)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(body) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForsetiColors.RuneGold,
                            contentColor = ForsetiColors.SplashBlack
                        )
                    ) {
                        Text("Save")
                    }
                }
                Text(
                    text = "Saved notes show up under the Notes tab.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ForsetiColors.SplashBlack.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PageCounterChip(page: Int, total: Int, modifier: Modifier = Modifier) {
    Surface(
        color = ForsetiColors.Charcoal.copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Text(
            text = "$page / $total",
            color = ForsetiColors.AshWhite,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun AssetMissingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(ForsetiColors.Background), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "FRCP PDF not bundled",
                style = MaterialTheme.typography.headlineMedium,
                color = ForsetiColors.RuneGold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Run scripts/fetch_assets.sh from the project root to download the public-domain Federal Rules of Civil Procedure (Dec 1, 2024) into app/src/main/assets/rules/, then rebuild.",
                style = MaterialTheme.typography.bodyMedium,
                color = ForsetiColors.AshGrey
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Quick Jump still works using the bundled outline; tapping an entry won't change pages until the PDF is present.",
                style = MaterialTheme.typography.bodySmall,
                color = ForsetiColors.AshGrey
            )
        }
    }
}

private fun filterToc(entries: List<TocEntry>, query: String): List<TocEntry> {
    if (query.isBlank()) return entries
    val needle = query.trim().lowercase()
    fun matchOrPrune(e: TocEntry): TocEntry? {
        val selfMatches = e.title.lowercase().contains(needle) || e.anchor.contains(needle)
        val matchedChildren = e.children.mapNotNull(::matchOrPrune)
        return when {
            selfMatches -> e.copy(children = matchedChildren.ifEmpty { e.children })
            matchedChildren.isNotEmpty() -> e.copy(children = matchedChildren)
            else -> null
        }
    }
    return entries.mapNotNull(::matchOrPrune)
}
