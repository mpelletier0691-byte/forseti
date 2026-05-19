package com.forseti.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.forseti.data.dao.BookmarkDao
import com.forseti.data.dao.CaseDao
import com.forseti.data.dao.DeadlineDao
import com.forseti.data.dao.NoteDao
import com.forseti.data.entities.BookmarkEntity
import com.forseti.data.entities.CaseEntity
import com.forseti.data.entities.DeadlineEntity
import com.forseti.data.entities.NoteEntity

@Database(
    entities = [
        BookmarkEntity::class,
        NoteEntity::class,
        CaseEntity::class,
        DeadlineEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ForsetiDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun noteDao(): NoteDao
    abstract fun caseDao(): CaseDao
    abstract fun deadlineDao(): DeadlineDao
}

// Reserved for future schema changes; v1 ships with no migrations.
val MIGRATIONS: Array<Migration> = emptyArray()
