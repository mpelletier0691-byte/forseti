package com.forseti.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks", indices = [Index("ruleAnchor", unique = true)])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleAnchor: String,
    val displayLabel: String,
    val createdAt: Long
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleAnchor: String,
    val body: String,
    val updatedAt: Long
)

@Entity(tableName = "cases")
data class CaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val court: String,
    val caseNumber: String,
    val role: String,
    val complaintFiledAt: Long?,
    val createdAt: Long
)

@Entity(
    tableName = "deadlines",
    foreignKeys = [
        ForeignKey(
            entity = CaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("caseId"), Index("dueAt")]
)
data class DeadlineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: Long,
    val title: String,
    val ruleCitation: String?,
    val dueAt: Long,
    val notifyAt: Long?,
    val completed: Boolean = false,
    val createdAt: Long
)
