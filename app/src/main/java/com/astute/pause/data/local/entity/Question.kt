package com.astute.pause.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    @ColumnInfo(name = "question_group") val group: QuestionGroup,
    val active: Boolean = true,
    val sortOrder: Int,
    val answerType: AnswerType,
    val options: List<String> = emptyList(),
    val warningAnswers: List<String> = emptyList(),
    val inRecheck: Boolean = false,
)
