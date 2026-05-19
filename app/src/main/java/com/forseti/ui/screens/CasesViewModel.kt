package com.forseti.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forseti.casefiles.CaseFolderService
import com.forseti.casefiles.CaseIngestService
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
import javax.inject.Inject

@HiltViewModel
class CasesViewModel @Inject constructor(
    private val repository: DeadlineRepository,
    private val folders: CaseFolderService,
    private val ingest: CaseIngestService
) : ViewModel() {

    private val _ingestMessage = MutableStateFlow<String?>(null)
    val ingestMessage: StateFlow<String?> = _ingestMessage.asStateFlow()
    fun consumeIngestMessage() { _ingestMessage.value = null }

    fun ingestFolderInto(case: CaseEntity, treeUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val report = ingest.ingestTree(case, treeUri)
            _ingestMessage.value = report.summary()
        }
    }

    fun ingestFilesInto(case: CaseEntity, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val report = ingest.ingestUris(case, uris)
            _ingestMessage.value = report.summary()
        }
    }

    /**
     * Ingest a Brokkr-Forge folder while the user is still in the "new case" dialog.
     * Saves the draft so a real workspace exists, then routes every file into it.
     * The freshly assigned case id is reported via [onSaved] so subsequent ingests
     * (or the final Save) reuse it instead of inserting duplicates.
     */
    fun saveAndIngestFolder(case: CaseEntity, treeUri: Uri, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val newId = repository.upsertCase(case)
            val stored = case.copy(id = if (case.id == 0L) newId else case.id)
            withContext(Dispatchers.IO) {
                runCatching { folders.ensureCaseRoot(stored) }
            }
            onSaved(stored.id)
            withContext(Dispatchers.IO) {
                val report = ingest.ingestTree(stored, treeUri)
                _ingestMessage.value = report.summary()
            }
        }
    }

    /** Same as [saveAndIngestFolder] but for the multi-file "Image Ingestion" picker. */
    fun saveAndIngestFiles(case: CaseEntity, uris: List<Uri>, onSaved: (Long) -> Unit) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val newId = repository.upsertCase(case)
            val stored = case.copy(id = if (case.id == 0L) newId else case.id)
            withContext(Dispatchers.IO) {
                runCatching { folders.ensureCaseRoot(stored) }
            }
            onSaved(stored.id)
            withContext(Dispatchers.IO) {
                val report = ingest.ingestUris(stored, uris)
                _ingestMessage.value = report.summary()
            }
        }
    }

    val cases: StateFlow<List<CaseEntity>> = repository.observeCases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun upsert(case: CaseEntity, onSaved: (CaseEntity) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.upsertCase(case)
            val stored = case.copy(id = if (case.id == 0L) id else case.id)
            withContext(Dispatchers.IO) {
                runCatching { folders.ensureCaseRoot(stored) }
            }
            onSaved(stored)
        }
    }

    /**
     * Permanently deletes [case]: cascades through all of its deadlines (and
     * cancels their scheduled reminders), removes the DB row, and finally
     * deletes the on-disk Brokkr-Forge workspace folder. Surfaces a snackbar
     * via [ingestMessage] so the user gets feedback even if the screen
     * recomposes immediately.
     */
    fun delete(case: CaseEntity) {
        if (case.id == 0L) return
        viewModelScope.launch {
            runCatching { repository.deleteCase(case) }
            withContext(Dispatchers.IO) {
                runCatching { folders.deleteCaseWorkspace(case) }
            }
            _ingestMessage.value = "Deleted ${case.title.ifBlank { "case" }} and its workspace."
        }
    }

    fun folderPath(case: CaseEntity): String? =
        folders.ensureCaseRoot(case)?.let { folders.displayPath(it) }

    /**
     * Lightweight completeness score (0..1) so the UI can show a banner nudging
     * the user to fill in the missing fields. We deliberately avoid hard gating
     * — this is a hint, not a requirement.
     */
    fun completeness(case: CaseEntity): Float {
        val checks = listOf(
            case.title.isNotBlank(),
            case.court.isNotBlank(),
            case.caseNumber.isNotBlank(),
            case.role.isNotBlank(),
            case.complaintFiledAt != null
        )
        val filled = checks.count { it }
        return filled.toFloat() / checks.size.toFloat()
    }
}
