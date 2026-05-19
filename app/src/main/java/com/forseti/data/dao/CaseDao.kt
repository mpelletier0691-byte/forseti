package com.forseti.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.forseti.data.entities.CaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {
    @Query("SELECT * FROM cases ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CaseEntity>>

    @Query("SELECT * FROM cases ORDER BY createdAt DESC")
    suspend fun allSnapshot(): List<CaseEntity>

    @Query("SELECT * FROM cases WHERE id = :id")
    suspend fun byId(id: Long): CaseEntity?

    // Parameter names below MUST NOT be `case` — Room emits the parameter name
    // verbatim into the generated Java *_Impl.java file, and `case` is a Java
    // reserved word, which makes the generated code fail to compile.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CaseEntity): Long

    @Update suspend fun update(entity: CaseEntity)
    @Delete suspend fun delete(entity: CaseEntity)
}
