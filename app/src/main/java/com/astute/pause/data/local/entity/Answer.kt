package com.astute.pause.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "answers",
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["itemId", "questionId", "phase"], unique = true)],
)
data class Answer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val questionId: Long,
    val questionText: String,
    val answerType: AnswerType,
    val answer: String,
    val note: String = "",
    val answeredAt: Instant,
    val phase: AnswerPhase,
    val flagged: Boolean = false,
    /** Snapshot of the follow-up prompt for the chosen option; blank when it had none. */
    @ColumnInfo(defaultValue = "") val followUpQuestion: String = "",
    @ColumnInfo(defaultValue = "") val followUp: String = "",
    /** Follow-ups are free text: never auto-flagged, only by the user on the summary. */
    @ColumnInfo(defaultValue = "0") val followUpFlagged: Boolean = false,
)
