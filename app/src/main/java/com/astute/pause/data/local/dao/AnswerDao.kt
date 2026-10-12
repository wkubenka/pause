package com.astute.pause.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.astute.pause.data.local.entity.Answer
import com.astute.pause.data.local.entity.AnswerPhase
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerDao {
    @Insert suspend fun insert(answer: Answer): Long
    @Update suspend fun update(answer: Answer)

    @Query("SELECT * FROM answers WHERE itemId = :itemId AND questionId = :questionId AND phase = :phase LIMIT 1")
    suspend fun find(itemId: Long, questionId: Long, phase: AnswerPhase): Answer?

    @Query("SELECT * FROM answers WHERE itemId = :itemId AND phase = :phase ORDER BY id")
    fun observeForItem(itemId: Long, phase: AnswerPhase): Flow<List<Answer>>

    @Query("SELECT * FROM answers WHERE itemId = :itemId AND phase = :phase ORDER BY id")
    suspend fun forItem(itemId: Long, phase: AnswerPhase): List<Answer>

    @Query("SELECT COUNT(*) FROM answers WHERE itemId = :itemId")
    suspend fun countForItem(itemId: Long): Int

    @Query("UPDATE answers SET flagged = :flagged WHERE id = :id")
    suspend fun setFlagged(id: Long, flagged: Boolean)
}
