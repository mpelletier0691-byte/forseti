package com.forseti.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forseti.casefiles.CaseIngestService
import com.forseti.casefiles.ScannerService
import com.forseti.data.entities.CaseEntity
import com.forseti.deadlines.DeadlineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val scanner: ScannerService,
    private val ingest: CaseIngestService,
    repository: DeadlineRepository
) : ViewModel() {

    val cases: StateFlow<List<CaseEntity>> = repository.observeCases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _pages = MutableStateFlow<List<Bitmap>>(emptyList())
    val pages: StateFlow<List<Bitmap>> = _pages.asStateFlow()

    private val _saved = MutableStateFlow<File?>(null)
    val saved: StateFlow<File?> = _saved.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Snackbar feed for "Ingest files" — the button that sits next to "Capture
     * page" on the scanner. Pictures/PDFs/audio/video the user picks are
     * auto-routed into the active case's Brokkr-Forge layout; anything the
     * router can't classify lands in `99_Inbox/` so it's still saved.
     */
    private val _ingestMessage = MutableStateFlow<String?>(null)
    val ingestMessage: StateFlow<String?> = _ingestMessage.asStateFlow()
    fun consumeIngestMessage() { _ingestMessage.value = null }

    fun ingestFilesInto(case: CaseEntity, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val report = ingest.ingestUris(case, uris)
            _ingestMessage.value = report.summary()
        }
    }

    fun ingestFolderInto(case: CaseEntity, treeUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val report = ingest.ingestTree(case, treeUri)
            _ingestMessage.value = report.summary()
        }
    }

    fun addPage(bitmap: Bitmap) {
        _pages.value = _pages.value + bitmap
    }

    fun removePage(index: Int) {
        _pages.value = _pages.value.toMutableList().also {
            if (index in it.indices) it.removeAt(index)
        }
    }

    fun clear() {
        _pages.value = emptyList()
        _saved.value = null
    }

    fun save(case: CaseEntity, label: String?) {
        val current = _pages.value
        if (current.isEmpty()) {
            _error.value = "Capture at least one page first."
            return
        }
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching { scanner.savePdf(case, current, label) }.getOrNull()
            }
            if (file == null) {
                _error.value = "Could not write PDF. Check storage permissions."
            } else {
                _saved.value = file
                _pages.value = emptyList()
            }
        }
    }

    fun consumeError() { _error.value = null }
    fun consumeSaved() { _saved.value = null }
}
