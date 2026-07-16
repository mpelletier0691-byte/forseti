package com.forseti.casefiles

import android.content.Context
import android.net.Uri
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.forseti.util.IngestUriPermissions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enqueues background Brokkr Forge ingest per case. Only one ingest runs per case
 * at a time; a new request while work is in flight is ignored ([ExistingWorkPolicy.KEEP]).
 */
@Singleton
class CaseIngestCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val brokkrProgress: BrokkrForgeProgress
) {
    fun enqueueTreeIngest(caseId: Long, treeUri: Uri) {
        if (caseId <= 0L) return
        IngestUriPermissions.persistTree(context, treeUri)
        val data = Data.Builder()
            .putLong(CaseIngestWorker.KEY_CASE_ID, caseId)
            .putString(CaseIngestWorker.KEY_MODE, CaseIngestWorker.MODE_TREE)
            .putString(CaseIngestWorker.KEY_TREE_URI, treeUri.toString())
            .build()
        enqueue(caseId, data)
    }

    fun enqueueUrisIngest(caseId: Long, uris: List<Uri>) {
        if (caseId <= 0L || uris.isEmpty()) return
        IngestUriPermissions.persistUris(context, uris)
        val data = Data.Builder()
            .putLong(CaseIngestWorker.KEY_CASE_ID, caseId)
            .putString(CaseIngestWorker.KEY_MODE, CaseIngestWorker.MODE_URIS)
            .putStringArray(CaseIngestWorker.KEY_URIS, uris.map { it.toString() }.toTypedArray())
            .build()
        enqueue(caseId, data)
    }

    fun cancelForCase(caseId: Long) {
        if (caseId <= 0L) return
        WorkManager.getInstance(context).cancelUniqueWork(workName(caseId))
        brokkrProgress.clear(caseId)
    }

    private fun enqueue(caseId: Long, data: Data) {
        brokkrProgress.markCollecting(caseId)
        val req = OneTimeWorkRequestBuilder<CaseIngestWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(caseId),
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    private fun workName(caseId: Long) = "ingest-case-$caseId"
}
