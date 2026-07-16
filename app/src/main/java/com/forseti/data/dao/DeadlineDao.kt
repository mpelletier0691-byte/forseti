package com.forseti.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.forseti.data.entities.DeadlineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeadlineDao {
    @Query("SELECT * FROM deadlines WHERE caseId = :caseId ORDER BY dueAt ASC")
    fun observeForCase(caseId: Long): Flow<List<DeadlineEntity>>

    @Query("SELECT * FROM deadlines WHERE completed = 0 ORDER BY dueAt ASC")
    fun observeUpcoming(): Flow<List<DeadlineEntity>>

    @Query("SELECT * FROM deadlines WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): DeadlineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(deadline: DeadlineEntity): Long

    @Update suspend fun update(deadline: DeadlineEntity)

    @Query("DELETE FROM deadlines WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Snapshot for cascading clean-up (e.g. cancelling WorkManager reminders
     * when the parent case is deleted). Not a Flow — callers want a one-shot
     * list, not a subscription.
     */
    @Query("SELECT * FROM deadlines WHERE caseId = :caseId")
    suspend fun snapshotForCase(caseId: Long): List<DeadlineEntity>

    @Query("DELETE FROM deadlines WHERE caseId = :caseId")
    suspend fun deleteForCase(caseId: Long)
}
