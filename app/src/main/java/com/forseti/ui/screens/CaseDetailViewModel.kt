package com.forseti.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forseti.casefiles.BrokkrForgeProgress
import com.forseti.casefiles.CaseFolderService
import com.forseti.data.entities.CaseEntity
import com.forseti.deadlines.DeadlineRepository
import com.forseti.idp.IngestMetaStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Backs the in-app file browser for a single case workspace. Operates on the
 * app-private folder tree maintained by [CaseFolderService] so users can drill
 * into phase / sub folders, rename files, share them, delete them, or import
 * outside files via SAF.
 */
@HiltViewModel
class CaseDetailViewModel @Inject constructor(
    val folderService: CaseFolderService,
    private val repository: DeadlineRepository,
    private val brokkrProgress: BrokkrForgeProgress
) : ViewModel() {

    private val folders get() = folderService

    data class FolderNode(
        val phase: File,
        val subfolders: List<SubfolderNode>
    )

    data class SubfolderNode(
        val folder: File,
        val files: List<File>
    )

    data class State(
        val case: CaseEntity? = null,
        val folders: List<FolderNode> = emptyList(),
        val workspaceRoot: String = "",
        val forgeProgress: BrokkrForgeProgress.State? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var loadedCaseId: Long = 0L
    private var lastForgeRefreshAt: Int = 0

    init {
        brokkrProgress.states.onEach { map ->
            val progress = map[loadedCaseId]
            _state.update { it.copy(forgeProgress = progress) }
            when (progress?.phase) {
                BrokkrForgeProgress.Phase.SORTING -> {
                    if (progress.processed > lastForgeRefreshAt) {
                        lastForgeRefreshAt = progress.processed
                        _state.value.case?.let { case ->
                            viewModelScope.launch { refresh(case) }
                        }
                    }
                }
                BrokkrForgeProgress.Phase.DONE -> {
                    lastForgeRefreshAt = 0
                    _state.value.case?.let { case ->
                        viewModelScope.launch { refresh(case) }
                    }
                }
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun load(caseId: Long) {
        loadedCaseId = caseId
        _state.update { it.copy(forgeProgress = brokkrProgress.stateFor(caseId)) }
        viewModelScope.launch {
            val case = repository.findCase(caseId) ?: return@launch
            refresh(case)
        }
    }

    fun refresh() {
        val case = _state.value.case ?: return
        viewModelScope.launch { refresh(case) }
    }

    private suspend fun refresh(case: CaseEntity) {
        val tree = withContext(Dispatchers.IO) {
            val phases = folders.listPhases(case)
            val nodes = phases.map { phase ->
                val subs = folders.listSubfolders(phase).map { sub ->
                    SubfolderNode(sub, folders.listFiles(sub))
                }
                // Files dropped directly under the phase folder
                val rootFiles = folders.listFiles(phase)
                if (rootFiles.isNotEmpty()) {
                    FolderNode(phase, listOf(SubfolderNode(phase, rootFiles)) + subs)
                } else {
                    FolderNode(phase, subs)
                }
            }
            val root = folders.ensureCaseRoot(case)?.let { folders.displayPath(it) } ?: ""
            nodes to root
        }
        _state.update {
            it.copy(case = case, folders = tree.first, workspaceRoot = tree.second)
        }
    }

    fun rename(file: File, newName: String, onRenamed: (File) -> Unit = {}) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { folders.renameFile(file, newName) }
            if (ok != null) {
                _message.value = "Renamed to ${ok.name}"
                refresh()
                onRenamed(ok)
            } else {
                _message.value = "Could not rename (name in use?)"
            }
        }
    }

    fun move(file: File, destFolder: File) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { folders.moveFile(file, destFolder) }
            _message.value = if (ok != null) "Moved to ${destFolder.name}/" else "Could not move (file in use?)"
            refresh()
        }
    }

    fun delete(file: File) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { folders.deleteFile(file) }
            _message.value = if (ok) "Deleted ${file.name}" else "Could not delete ${file.name}"
            refresh()
        }
    }

    fun deleteAllInFolder(folder: File) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) { folders.deleteAllFilesIn(folder) }
            _message.value = if (count > 0) {
                "Deleted $count file${if (count == 1) "" else "s"} from ${folder.name}/"
            } else {
                "No files to delete in ${folder.name}/"
            }
            refresh()
        }
    }

    fun importInto(uri: Uri, folder: File, displayName: String?) {
        viewModelScope.launch {
            val out = withContext(Dispatchers.IO) {
                folders.importContent(uri, folder, displayName ?: uri.lastPathSegment.orEmpty())
            }
            _message.value = if (out != null) "Imported ${out.name}" else "Could not import file"
            refresh()
        }
    }

    fun moveToSuggestedFolder(file: File, relativeFolder: String) {
        viewModelScope.launch {
            val case = _state.value.case ?: return@launch
            val root = withContext(Dispatchers.IO) { folders.ensureCaseRoot(case) } ?: return@launch
            val dest = File(root, relativeFolder).apply { mkdirs() }
            val moved = withContext(Dispatchers.IO) {
                val result = folders.moveFile(file, dest)
                if (result != null) {
                    IngestMetaStore.markFiled(result, relativeFolder)
                }
                result
            }
            _message.value = if (moved != null) {
                "Filed to ${dest.name}/"
            } else {
                "Could not move (file in use?)"
            }
            refresh()
        }
    }

    fun shareableUri(file: File): Uri = folders.shareableUri(file)

    fun consumeMessage() { _message.value = null }
}
