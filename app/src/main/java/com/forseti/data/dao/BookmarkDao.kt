package com.forseti.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.forseti.data.entities.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE ruleAnchor = :anchor")
    suspend fun removeByAnchor(anchor: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE ruleAnchor = :anchor)")
    fun isBookmarked(anchor: String): Flow<Boolean>
}
