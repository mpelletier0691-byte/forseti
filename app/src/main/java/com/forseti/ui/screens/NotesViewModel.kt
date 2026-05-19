package com.forseti.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forseti.data.dao.BookmarkDao
import com.forseti.data.dao.NoteDao
import com.forseti.data.entities.BookmarkEntity
import com.forseti.data.entities.NoteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val bookmarkDao: BookmarkDao,
    private val noteDao: NoteDao
) : ViewModel() {

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = noteDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun upsertNote(anchor: String, body: String) {
        viewModelScope.launch {
            noteDao.upsert(
                NoteEntity(ruleAnchor = anchor, body = body, updatedAt = System.currentTimeMillis())
            )
        }
    }

    fun deleteNote(id: Long) { viewModelScope.launch { noteDao.delete(id) } }

    fun addBookmark(anchor: String, label: String) {
        viewModelScope.launch {
            bookmarkDao.upsert(
                BookmarkEntity(
                    ruleAnchor = anchor,
                    displayLabel = label,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun removeBookmark(anchor: String) { viewModelScope.launch { bookmarkDao.removeByAnchor(anchor) } }

    /**
     * Convenience for the Quick Jump bookmark icon: flips state without the
     * caller having to read the current value.
     */
    fun toggleBookmark(anchor: String, label: String) {
        viewModelScope.launch {
            val existing = bookmarks.value.firstOrNull { it.ruleAnchor == anchor }
            if (existing == null) {
                bookmarkDao.upsert(
                    BookmarkEntity(
                        ruleAnchor = anchor,
                        displayLabel = label,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else {
                bookmarkDao.removeByAnchor(anchor)
            }
        }
    }
}
