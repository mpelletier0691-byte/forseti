package com.forseti.casefiles

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory Brokkr Forge progress per case. Used by [CaseIngestWorker] for
 * foreground notifications and by Case Profile to show a sorting banner.
 */
@Singleton
class BrokkrForgeProgress @Inject constructor() {

    enum class Phase { IDLE, COLLECTING, SORTING, DONE, FAILED }

    data class State(
        val caseId: Long,
        val phase: Phase,
        val total: Int = 0,
        val processed: Int = 0,
        val imported: Int = 0,
        val skipped: Int = 0,
        val message: String? = null
    ) {
        val isActive: Boolean get() = phase == Phase.COLLECTING || phase == Phase.SORTING
    }

    private val _states = MutableStateFlow<Map<Long, State>>(emptyMap())
    val states: StateFlow<Map<Long, State>> = _states.asStateFlow()

    fun stateFor(caseId: Long): State? = _states.value[caseId]

    fun isRunning(caseId: Long): Boolean = stateFor(caseId)?.isActive == true

    fun markCollecting(caseId: Long) {
        _states.update { it + (caseId to State(caseId, Phase.COLLECTING)) }
    }

    fun markSorting(caseId: Long, total: Int) {
        _states.update {
            it + (caseId to State(caseId, Phase.SORTING, total = total, processed = 0))
        }
    }

    fun tick(caseId: Long, processed: Int, imported: Int, skipped: Int, failed: Int) {
        _states.update { map ->
            val prev = map[caseId] ?: return@update map
            map + (caseId to prev.copy(
                processed = processed,
                imported = imported,
                skipped = skipped
            ))
        }
    }

    fun markDone(caseId: Long, report: CaseIngestService.Report) {
        _states.update {
            it + (caseId to State(
                caseId = caseId,
                phase = Phase.DONE,
                total = report.totalDiscovered,
                processed = report.imported + report.skipped + report.failed,
                imported = report.imported,
                skipped = report.skipped,
                message = report.summary()
            ))
        }
    }

    fun markFailed(caseId: Long, message: String) {
        _states.update {
            val prev = it[caseId]
            it + (caseId to State(
                caseId = caseId,
                phase = Phase.FAILED,
                total = prev?.total ?: 0,
                processed = prev?.processed ?: 0,
                imported = prev?.imported ?: 0,
                skipped = prev?.skipped ?: 0,
                message = message
            ))
        }
    }

    fun clear(caseId: Long) {
        _states.update { it - caseId }
    }
}

class IngestAbortException(message: String) : Exception(message)
