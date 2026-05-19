package com.forseti.pdf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the main PDF viewer pane. Tracks the currently displayed page and
 * forwards Quick-Jump requests to it.
 */
@HiltViewModel
class PdfViewModel @Inject constructor(
    val repository: PdfRepository
) : ViewModel() {

    val toc: StateFlow<List<TocEntry>> = repository.toc
    val pageCount: StateFlow<Int> = repository.pageCount

    /**
     * The page the viewer should scroll to. Updated only by Quick-Jump selections
     * (NOT by user scroll). Mixing those two would produce a feedback loop where
     * every snapshot-flow tick re-triggered scrollToItem and cancelled the fling.
     */
    private val _jumpTarget = MutableStateFlow(0)
    val jumpTarget: StateFlow<Int> = _jumpTarget.asStateFlow()

    /** Page the user is currently looking at, derived from the viewer's scroll position. */
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    init {
        viewModelScope.launch { repository.warmFrcp() }
    }

    /** Quick Jump tap. Jumps the viewer to a 0-based page index. */
    fun jumpTo(page: Int) {
        val clamped = page.coerceIn(0, (pageCount.value - 1).coerceAtLeast(0))
        _jumpTarget.value = clamped
        _currentPage.value = clamped
    }

    /** Quick Jump tap on a TOC entry. [TocEntry.page] is always 1-based. */
    fun jumpTo(entry: TocEntry) = jumpTo((entry.page - 1).coerceAtLeast(0))

    /** Hook the PDF viewer calls as the user scrolls. */
    fun onScrollChanged(page: Int) { _currentPage.value = page }

    fun setQuery(q: String) { _query.value = q }
}
