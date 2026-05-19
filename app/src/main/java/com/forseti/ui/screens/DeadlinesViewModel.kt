package com.forseti.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forseti.casefiles.CaseFolderService
import com.forseti.data.entities.CaseEntity
import com.forseti.data.entities.DeadlineEntity
import com.forseti.deadlines.DeadlineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeadlinesViewModel @Inject constructor(
    private val repository: DeadlineRepository,
    private val caseFolders: CaseFolderService
) : ViewModel() {

    val cases: StateFlow<List<CaseEntity>> = repository.observeCases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deadlinesFor(caseId: Long): Flow<List<DeadlineEntity>> = repository.observeDeadlines(caseId)

    fun addCase(case: CaseEntity) { viewModelScope.launch { repository.upsertCase(case) } }
    fun addDeadline(case: CaseEntity, deadline: DeadlineEntity) {
        viewModelScope.launch {
            repository.upsertDeadline(deadline)
            runCatching { caseFolders.ensureFoldersForDeadline(case, deadline) }
        }
    }
    fun toggle(deadline: DeadlineEntity) { viewModelScope.launch { repository.toggleComplete(deadline) } }
    fun delete(deadline: DeadlineEntity) { viewModelScope.launch { repository.delete(deadline) } }

    /** Returns the on-device folder path for the case workspace, ensuring it exists. */
    fun caseFolderPath(case: CaseEntity): String? =
        caseFolders.ensureCaseRoot(case)?.let { caseFolders.displayPath(it) }

    /** Lightweight completeness used to show a nudge banner. Same heuristic as [CasesViewModel]. */
    fun completeness(case: CaseEntity): Float {
        val checks = listOf(
            case.title.isNotBlank(),
            case.court.isNotBlank(),
            case.caseNumber.isNotBlank(),
            case.role.isNotBlank(),
            case.complaintFiledAt != null
        )
        return checks.count { it } / checks.size.toFloat()
    }
}
