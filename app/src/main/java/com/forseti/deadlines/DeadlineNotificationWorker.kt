package com.forseti.deadlines

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.forseti.MainActivity
import com.forseti.R
import com.forseti.data.dao.CaseDao
import com.forseti.data.dao.DeadlineDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Fires when a deadline's notify time is reached.
 * One-shot: scheduled by [DeadlineRepository] when a deadline is added.
 */
@HiltWorker
class DeadlineNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val deadlineDao: DeadlineDao,
    private val caseDao: CaseDao
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1L)
        if (id <= 0) return Result.failure()
        val deadlines = deadlineDao.observeUpcoming().first()
        val deadline = deadlines.firstOrNull { it.id == id } ?: return Result.success()
        val case = caseDao.byId(deadline.caseId) ?: return Result.success()

        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Deadlines", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Court deadlines tracked by Forseti" }
        )

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            applicationContext, deadline.id.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = buildString {
            append(case.title)
            if (case.caseNumber.isNotBlank()) append(" - ").append(case.caseNumber)
            deadline.ruleCitation?.let { append(" (").append(it).append(')') }
        }

        val n = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(deadline.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(deadline.id.toInt(), n)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "forseti_deadlines"
        const val KEY_ID = "deadline_id"
    }
}
