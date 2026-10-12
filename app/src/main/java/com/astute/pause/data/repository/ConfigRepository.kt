package com.astute.pause.data.repository

import androidx.room.withTransaction
import com.astute.pause.data.local.PauseDatabase
import com.astute.pause.data.local.SeedData
import com.astute.pause.data.local.entity.AnswerType
import com.astute.pause.data.local.entity.Question
import com.astute.pause.data.local.entity.Tier
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(private val db: PauseDatabase) {
    private val tierDao = db.tierDao()
    private val questionDao = db.questionDao()

    fun observeTiers(): Flow<List<Tier>> = tierDao.observeAll()
    suspend fun tiers(): List<Tier> = tierDao.all()
    suspend fun tier(id: Long): Tier? = tierDao.get(id)

    suspend fun saveTier(tier: Tier): Long {
        val clean = tier.copy(name = tier.name.trim())
        return if (clean.id == 0L) tierDao.insert(clean) else clean.id.also { tierDao.update(clean) }
    }

    suspend fun deleteTier(tier: Tier) = tierDao.delete(tier)

    fun observeQuestions(): Flow<List<Question>> = questionDao.observeAll()
    suspend fun questions(): List<Question> = questionDao.all()
    suspend fun question(id: Long): Question? = questionDao.get(id)

    suspend fun saveQuestion(question: Question): Long = db.withTransaction {
        val clean = sanitize(question)
        if (clean.id == 0L) questionDao.insert(clean.copy(sortOrder = questionDao.maxSortOrder() + 1))
        else clean.id.also { questionDao.update(clean) }
    }

    suspend fun move(questionId: Long, up: Boolean) {
        db.withTransaction {
            val all = questionDao.all()
            val i = all.indexOfFirst { it.id == questionId }
            val j = if (up) i - 1 else i + 1
            if (i < 0 || j !in all.indices) return@withTransaction
            val a = all[i]
            val b = all[j]
            questionDao.updateAll(listOf(a.copy(sortOrder = b.sortOrder), b.copy(sortOrder = a.sortOrder)))
        }
    }

    suspend fun setActive(questionId: Long, active: Boolean) {
        val q = questionDao.get(questionId) ?: return
        questionDao.update(q.copy(active = active))
    }

    private fun sanitize(q: Question): Question {
        val options = if (q.answerType == AnswerType.CHOICE) q.options.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        else emptyList()
        val valid = when (q.answerType) {
            AnswerType.YES_NO -> listOf(SeedData.YES, SeedData.NO)
            AnswerType.CHOICE -> options
            AnswerType.TEXT -> emptyList()
        }
        return q.copy(text = q.text.trim(), options = options, warningAnswers = q.warningAnswers.filter { it in valid }.distinct())
    }
}
