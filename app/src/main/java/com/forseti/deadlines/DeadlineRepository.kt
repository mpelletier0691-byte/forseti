package com.forseti.deadlines

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.forseti.data.dao.CaseDao
import com.forseti.data.dao.DeadlineDao
import com.forseti.data.entities.CaseEntity
import com.forseti.data.entities.DeadlineEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeadlineRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val caseDao: CaseDao,
    private val deadlineDao: DeadlineDao
) {
    fun observeCases(): Flow<List<CaseEntity>> = caseDao.observeAll()
    fun observeDeadlines(caseId: Long): Flow<List<DeadlineEntity>> = deadlineDao.observeForCase(caseId)
    fun observeUpcoming(): Flow<List<DeadlineEntity>> = deadlineDao.observeUpcoming()

    suspend fun findCase(caseId: Long): CaseEntity? = caseDao.byId(caseId)

    /** One-shot snapshot used by share-receiver to populate the case picker. */
    suspend fun allCasesSnapshot(): List<CaseEntity> = caseDao.allSnapshot()

    suspend fun upsertCase(case: CaseEntity): Long {
        val stamped = if (case.createdAt == 0L) {
            case.copy(createdAt = System.currentTimeMillis())
        } else {
            case
        }
        return if (stamped.id == 0L) caseDao.insert(stamped) else {
            caseDao.update(stamped)
            stamped.id
        }
    }

    suspend fun upsertDeadline(deadline: DeadlineEntity): Long {
        val id = deadlineDao.upsert(deadline)
        scheduleNotification(deadline.copy(id = id))
        return id
    }

    suspend fun toggleComplete(deadline: DeadlineEntity) {
        deadlineDao.update(deadline.copy(completed = !deadline.completed))
    }

    suspend fun delete(deadline: DeadlineEntity) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(deadline.id))
        deadlineDao.delete(deadline.id)
    }

    /**
     * Cascade-deletes a case: cancels every scheduled reminder for its
     * deadlines, drops all deadline rows for that case, then removes the case
     * row itself. The on-disk workspace folder is removed separately by
     * [com.forseti.casefiles.CaseFolderService.deleteCaseWorkspace].
     */
    suspend fun deleteCase(case: CaseEntity) {
        val wm = WorkManager.getInstance(context)
        val deadlines = deadlineDao.snapshotForCase(case.id)
        deadlines.forEach { wm.cancelUniqueWork(workName(it.id)) }
        deadlineDao.deleteForCase(case.id)
        caseDao.delete(case)
    }

    private fun scheduleNotification(deadline: DeadlineEntity) {
        val notifyAt = deadline.notifyAt ?: return
        val delayMs = (notifyAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val req = OneTimeWorkRequestBuilder<DeadlineNotificationWorker>()
            .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putLong(DeadlineNotificationWorker.KEY_ID, deadline.id).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(deadline.id),
            androidx.work.ExistingWorkPolicy.REPLACE,
            req
        )
    }

    private fun workName(id: Long) = "deadline-$id"
}
