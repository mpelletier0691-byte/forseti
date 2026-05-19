package com.forseti.states

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that downloads a state-rule PDF.
 * Triggered by long-press on a row in the State Rules tab.
 */
@HiltWorker
class StateDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val cache: StateCacheManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val abbr = inputData.getString(KEY_ABBR) ?: return Result.failure()
        val all = StateRulesCatalog.states + StateRulesCatalog.circuits
        val rule = all.firstOrNull { it.abbreviation == abbr } ?: return Result.failure()
        return if (cache.download(rule).isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val KEY_ABBR = "abbr"
    }
}
