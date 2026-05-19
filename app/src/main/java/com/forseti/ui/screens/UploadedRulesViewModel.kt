package com.forseti.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forseti.imports.UploadedRulesService
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
class UploadedRulesViewModel @Inject constructor(
    private val service: UploadedRulesService
) : ViewModel() {

    private val _items = MutableStateFlow<List<UploadedRulesService.UploadedRule>>(emptyList())
    val items: StateFlow<List<UploadedRulesService.UploadedRule>> = _items.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = service.list()
            withContext(Dispatchers.Main) { _items.value = list }
        }
    }

    fun import(uri: Uri, title: String, jurisdiction: String, source: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = service.importFromUri(uri, title, jurisdiction, source)
            withContext(Dispatchers.Main) {
                _message.value = if (result == null) {
                    "Could not import that file. Make sure it's a PDF you have permission to read."
                } else {
                    "Imported ${result.name}"
                }
            }
            refresh()
        }
    }

    fun delete(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = service.delete(file)
            withContext(Dispatchers.Main) {
                _message.value = if (ok) "Removed ${file.name}" else "Could not remove ${file.name}"
            }
            refresh()
        }
    }

    fun rename(file: File, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = service.rename(file, title)
            withContext(Dispatchers.Main) {
                _message.value = if (ok) "Renamed to $title" else "Could not rename"
            }
            refresh()
        }
    }

    fun shareableUri(file: File): Uri = service.shareableUri(file)

    fun consumeMessage() { _message.value = null }
}
