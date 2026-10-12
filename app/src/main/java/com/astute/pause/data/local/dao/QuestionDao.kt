package com.astute.pause.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.astute.pause.data.local.entity.Question
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<Question>>

    @Query("SELECT * FROM questions ORDER BY sortOrder, id")
    suspend fun all(): List<Question>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun get(id: Long): Question?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM questions")
    suspend fun maxSortOrder(): Int

    @Insert suspend fun insert(question: Question): Long
    @Update suspend fun update(question: Question)
    @Update suspend fun updateAll(questions: List<Question>)
}
