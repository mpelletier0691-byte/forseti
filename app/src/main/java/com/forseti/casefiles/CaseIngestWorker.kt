package com.forseti.casefiles

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.forseti.MainActivity
import com.forseti.R
import com.forseti.data.dao.CaseDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Background Brokkr Forge ingest. Shows a foreground progress notification while
 * [CaseIngestService] runs, then fires completion only after every file is processed.
 */
@HiltWorker
class CaseIngestWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val caseDao: CaseDao,
    private val ingest: CaseIngestService,
    private val brokkrProgress: BrokkrForgeProgress
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val caseId = inputData.getLong(KEY_CASE_ID, -1L)
        if (caseId <= 0) return Result.failure()
        val case = caseDao.byId(caseId) ?: return Result.failure()

        runCatching { setForeground(buildForegroundInfo(caseId, 0, 0)) }

        return try {
            coroutineScope {
                val progressJob = launch {
                    brokkrProgress.states.collect { map ->
                        val s = map[caseId] ?: return@collect
                        if (s.isActive && s.total > 0) {
                            setForeground(buildForegroundInfo(caseId, s.processed, s.total))
                            setProgressAsync(
                                Data.Builder()
                                    .putInt(KEY_PROGRESS, s.processed)
                                    .putInt(KEY_TOTAL, s.total)
                                    .build()
                            )
                        }
                    }
                }

                val report = when (inputData.getString(KEY_MODE)) {
                    MODE_TREE -> {
                        val uriStr = inputData.getString(KEY_TREE_URI)
                            ?: return@coroutineScope Result.failure()
                        ingest.ingestTree(case, Uri.parse(uriStr))
                    }
                    MODE_URIS -> {
                        val uriStrs = inputData.getStringArray(KEY_URIS)
                            ?: return@coroutineScope Result.failure()
                        if (uriStrs.isEmpty()) return@coroutineScope Result.failure()
                        ingest.ingestUris(case, uriStrs.map { Uri.parse(it) })
                    }
                    else -> return@coroutineScope Result.failure()
                }

                progressJob.cancel()

                if (!report.isFullyProcessed()) {
                    val msg = "Sorting incomplete (${report.imported + report.skipped + report.failed} of ${report.totalDiscovered})."
                    brokkrProgress.markFailed(caseId, msg)
                    clearForegroundNotification(caseId)
                    showFailureNotification(caseId, msg)
                    return@coroutineScope Result.failure()
                }

                clearForegroundNotification(caseId)
                showCompleteNotification(caseId, report)
                Result.success()
            }
        } catch (e: CancellationException) {
            brokkrProgress.markFailed(caseId, "Sorting cancelled.")
            clearForegroundNotification(caseId)
            throw e
        } catch (e: IngestAbortException) {
            brokkrProgress.markFailed(caseId, e.message ?: "Sorting failed.")
            clearForegroundNotification(caseId)
            showFailureNotification(caseId, e.message ?: "Sorting failed.")
            Result.failure()
        } catch (e: Exception) {
            brokkrProgress.markFailed(caseId, e.message ?: "unknown error")
            clearForegroundNotification(caseId)
            showFailureNotification(caseId, e.message ?: "unknown error")
            Result.failure()
        }
    }

    private fun clearForegroundNotification(caseId: Long) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(foregroundNotificationId(caseId))
    }

    private fun buildForegroundInfo(caseId: Long, processed: Int, total: Int): ForegroundInfo {
        ensureChannel()
        val body = if (total > 0) {
            applicationContext.getString(R.string.brokkr_forge_foreground_progress, processed, total)
        } else {
            applicationContext.getString(R.string.brokkr_forge_foreground_body)
        }
        val max = total.coerceAtLeast(1)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(applicationContext.getString(R.string.brokkr_forge_foreground_title))
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(max, processed.coerceAtMost(max), total <= 0)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        val id = foregroundNotificationId(caseId)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    private fun showCompleteNotification(caseId: Long, report: CaseIngestService.Report) {
        ensureChannel()
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val summary = report.summary()
        val text = applicationContext.getString(R.string.brokkr_forge_complete)
        val n = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(applicationContext.getString(R.string.brokkr_forge_complete_title))
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$text\n$summary"
                )
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(mainActivityPendingIntent(caseId))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(completeNotificationId(caseId), n)
    }

    private fun showFailureNotification(caseId: Long, error: String) {
        ensureChannel()
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val text = applicationContext.getString(R.string.brokkr_forge_failed, error)
        val n = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(applicationContext.getString(R.string.brokkr_forge_failed_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(mainActivityPendingIntent(caseId))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(completeNotificationId(caseId), n)
    }

    private fun mainActivityPendingIntent(caseId: Long): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            applicationContext,
            caseId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun ensureChannel() {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Brokkr Forge", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Case folder sorting and ingest completion" }
        )
    }

    companion object {
        const val CHANNEL_ID = "forseti_brokkr_forge"
        const val KEY_CASE_ID = "case_id"
        const val KEY_MODE = "mode"
        const val KEY_TREE_URI = "tree_uri"
        const val KEY_URIS = "uris"
        const val KEY_PROGRESS = "progress"
        const val KEY_TOTAL = "total"
        const val MODE_TREE = "tree"
        const val MODE_URIS = "uris"

        fun foregroundNotificationId(caseId: Long): Int = (caseId % Int.MAX_VALUE).toInt()
        fun completeNotificationId(caseId: Long): Int = (caseId % Int.MAX_VALUE).toInt() + 1_000_000
    }
}
