package com.forseti.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forseti.casefiles.CaseFolderService
import com.forseti.data.entities.CaseEntity
import com.forseti.deadlines.DeadlineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val repository: DeadlineRepository
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
        val workspaceRoot: String = ""
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun load(caseId: Long) {
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

    fun importInto(uri: Uri, folder: File, displayName: String?) {
        viewModelScope.launch {
            val out = withContext(Dispatchers.IO) {
                folders.importContent(uri, folder, displayName ?: uri.lastPathSegment.orEmpty())
            }
            _message.value = if (out != null) "Imported ${out.name}" else "Could not import file"
            refresh()
        }
    }

    fun shareableUri(file: File): Uri = folders.shareableUri(file)

    fun consumeMessage() { _message.value = null }
}
