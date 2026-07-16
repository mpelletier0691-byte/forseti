package com.forseti.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forseti.R
import com.forseti.casefiles.CaseFolderService
import com.forseti.casefiles.CaseIngestCoordinator
import com.forseti.casefiles.CaseIngestService
import com.forseti.data.entities.CaseEntity
import com.forseti.deadlines.DeadlineRepository
import com.forseti.util.IngestUriPermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val repository: DeadlineRepository,
    private val folders: CaseFolderService,
    private val ingest: CaseIngestService,
    private val ingestCoordinator: CaseIngestCoordinator
) : ViewModel() {

    private val _ingestMessage = MutableStateFlow<String?>(null)
    val ingestMessage: StateFlow<String?> = _ingestMessage.asStateFlow()
    fun consumeIngestMessage() { _ingestMessage.value = null }

    fun ingestFolderInto(case: CaseEntity, treeUri: Uri) {
        if (case.id == 0L) {
            _ingestMessage.value = "Save the case first, then ingest."
            return
        }
        runCatching {
            IngestUriPermissions.persistTree(context, treeUri)
            ingestCoordinator.enqueueTreeIngest(case.id, treeUri)
        }.onSuccess {
            _ingestMessage.value = context.getString(R.string.brokkr_forge_started)
        }.onFailure {
            _ingestMessage.value = "Could not start Brokkr Forge: ${it.message ?: "unknown error"}"
        }
    }

    fun ingestFilesInto(case: CaseEntity, uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (case.id == 0L) {
            _ingestMessage.value = "Save the case first, then ingest."
            return
        }
        if (uris.size > 1) {
            runCatching {
                IngestUriPermissions.persistUris(context, uris)
                ingestCoordinator.enqueueUrisIngest(case.id, uris)
            }.onSuccess {
                _ingestMessage.value = context.getString(R.string.brokkr_forge_started)
            }.onFailure {
                _ingestMessage.value = "Could not start Brokkr Forge: ${it.message ?: "unknown error"}"
            }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { ingest.ingestUris(case, uris) }
                .onSuccess { _ingestMessage.value = it.summary() }
                .onFailure { _ingestMessage.value = "Ingest failed: ${it.message ?: "unknown error"}" }
        }
    }

    /**
     * Ingest a Brokkr-Forge folder while the user is still in the "new case" dialog.
     * Saves the draft so a real workspace exists, then routes every file into it.
     */
    fun saveAndIngestFolder(
        case: CaseEntity,
        treeUri: Uri,
        onSaved: (Long) -> Unit,
        onIngestStarted: () -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val newId = repository.upsertCase(case)
                    val stored = case.copy(id = if (case.id == 0L) newId else case.id)
                    val root = runCatching { folders.ensureCaseRoot(stored) }.getOrNull()
                        ?: error("Case workspace could not be created. Free device storage and try again.")
                    if (!IngestUriPermissions.persistTree(context, treeUri)) {
                        error("Could not keep read access to the folder. Pick the folder again.")
                    }
                    ingestCoordinator.enqueueTreeIngest(stored.id, treeUri)
                    stored
                }
            }.onSuccess { stored ->
                onSaved(stored.id)
                onIngestStarted()
                _ingestMessage.value = context.getString(R.string.brokkr_forge_started)
            }.onFailure {
                _ingestMessage.value = it.message ?: "Ingest could not start."
            }
        }
    }

    fun saveAndIngestFiles(
        case: CaseEntity,
        uris: List<Uri>,
        onSaved: (Long) -> Unit,
        onIngestStarted: () -> Unit = {}
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val newId = repository.upsertCase(case)
                    val stored = case.copy(id = if (case.id == 0L) newId else case.id)
                    val root = runCatching { folders.ensureCaseRoot(stored) }.getOrNull()
                        ?: error("Case workspace could not be created. Free device storage and try again.")
                    if (uris.size > 1) {
                        IngestUriPermissions.persistUris(context, uris)
                        ingestCoordinator.enqueueUrisIngest(stored.id, uris)
                    } else {
                        val report = ingest.ingestUris(stored, uris)
                        return@withContext Pair(stored, report.summary())
                    }
                    stored to null
                }
            }.onSuccess { (stored, inlineSummary) ->
                onSaved(stored.id)
                if (inlineSummary != null) {
                    _ingestMessage.value = inlineSummary
                } else {
                    onIngestStarted()
                    _ingestMessage.value = context.getString(R.string.brokkr_forge_started)
                }
            }.onFailure {
                _ingestMessage.value = it.message ?: "Ingest could not start."
            }
        }
    }

    val cases: StateFlow<List<CaseEntity>> = repository.observeCases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun upsert(case: CaseEntity, onSaved: (CaseEntity) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val id = repository.upsertCase(case)
                    val stored = case.copy(id = if (case.id == 0L) id else case.id)
                    val root = runCatching { folders.ensureCaseRoot(stored) }.getOrNull()
                    stored to root
                }
            }.onSuccess { (stored, root) ->
                if (root == null) {
                    _ingestMessage.value =
                        "Case saved but workspace could not be created. Free storage on the device and edit the case again."
                }
                onSaved(stored)
            }.onFailure {
                _ingestMessage.value = "Could not save case: ${it.message ?: "unknown error"}"
            }
        }
    }

    fun delete(case: CaseEntity) {
        if (case.id == 0L) return
        viewModelScope.launch {
            ingestCoordinator.cancelForCase(case.id)
            runCatching { repository.deleteCase(case) }
            withContext(Dispatchers.IO) {
                runCatching { folders.deleteCaseWorkspace(case) }
            }
            _ingestMessage.value = "Deleted ${case.title.ifBlank { "case" }} and its workspace."
        }
    }

    fun showFolderPath(case: CaseEntity) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                folders.ensureCaseRoot(case)?.let { folders.displayPath(it) } ?: "Unavailable"
            }
            _ingestMessage.value = path
        }
    }

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
