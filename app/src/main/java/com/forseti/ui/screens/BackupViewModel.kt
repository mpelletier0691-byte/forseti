package com.forseti.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forseti.casefiles.BackupService
import com.forseti.casefiles.CaseFolderService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val service: BackupService,
    folders: CaseFolderService
) : ViewModel() {

    val workspacePath: String = folders.displayPath(folders.caseWorkspaceRoot())

    private val _backups = MutableStateFlow<List<File>>(service.listBackups())
    val backups: StateFlow<List<File>> = _backups.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _lastResult = MutableStateFlow<BackupService.BackupResult?>(null)
    val lastResult: StateFlow<BackupService.BackupResult?> = _lastResult.asStateFlow()

    fun createBackup() {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { service.createBackup() }.getOrNull()
            }
            if (result != null) _lastResult.value = result
            _backups.value = service.listBackups()
            _busy.value = false
        }
    }

    fun delete(file: File) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { service.delete(file) }
            _backups.value = service.listBackups()
        }
    }

    fun shareUri(file: File) = service.shareableUri(file)

    fun consumeResult() { _lastResult.value = null }
}
