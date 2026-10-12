package com.astute.pause.data.repository

import androidx.room.withTransaction
import com.astute.pause.data.local.PauseDatabase
import com.astute.pause.data.local.entity.Answer
import com.astute.pause.data.local.entity.AnswerPhase
import com.astute.pause.data.local.entity.AnswerType
import com.astute.pause.data.local.entity.CurrencyTotal
import com.astute.pause.data.local.entity.Item
import com.astute.pause.data.local.entity.ItemStatus
import com.astute.pause.data.local.entity.Question
import com.astute.pause.domain.AnswerEvaluator
import com.astute.pause.domain.Money
import com.astute.pause.domain.TierResolver
import com.astute.pause.reminders.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseRepository @Inject constructor(
    private val db: PauseDatabase,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
) {
    private val items = db.itemDao()
    private val answers = db.answerDao()
    private val questions = db.questionDao()
    private val tiers = db.tierDao()

    suspend fun startItem(name: String, currency: String = Money.defaultCurrency()): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Item name is blank" }
        return items.insert(
            Item(name = trimmed, firstConsideredAt = clock.instant(), currency = currency, status = ItemStatus.CONSIDERING)
        )
    }

    suspend fun item(id: Long): Item? = items.get(id)
    fun observeItem(id: Long): Flow<Item?> = items.observe(id)

    suspend fun setCost(itemId: Long, cents: Long, notes: String? = null) {
        val item = items.get(itemId) ?: return
        val tier = TierResolver.resolve(tiers.all(), cents)
        items.update(
            item.copy(
                costCents = cents,
                notes = notes?.trim() ?: item.notes,
                tierId = tier?.id,
                tierName = tier?.name,
                waitUntil = TierResolver.waitUntil(item.firstConsideredAt, tier),
            )
        )
    }

    suspend fun questionsFor(itemId: Long, phase: AnswerPhase): List<Question> {
        val all = questions.all()
        return when (phase) {
            AnswerPhase.RECHECK -> TierResolver.recheckQuestions(all)
            AnswerPhase.INITIAL -> {
                val tier = items.get(itemId)?.tierId?.let { tiers.get(it) }
                TierResolver.initialQuestions(tier, all)
            }
        }
    }

    suspend fun saveAnswer(itemId: Long, question: Question, answer: String, note: String, phase: AnswerPhase) {
        db.withTransaction {
            val existing = answers.find(itemId, question.id, phase)
            val flagged = if (question.answerType == AnswerType.TEXT) existing?.flagged ?: false
            else AnswerEvaluator.isWarning(question, answer)
            val row = Answer(
                id = existing?.id ?: 0,
                itemId = itemId,
                questionId = question.id,
                questionText = question.text,
                answerType = question.answerType,
                answer = answer.trim(),
                note = note.trim(),
                answeredAt = clock.instant(),
                phase = phase,
                flagged = flagged,
            )
            if (existing == null) answers.insert(row) else answers.update(row)
        }
    }

    fun observeAnswers(itemId: Long, phase: AnswerPhase): Flow<List<Answer>> = answers.observeForItem(itemId, phase)
    suspend fun answersFor(itemId: Long, phase: AnswerPhase): List<Answer> = answers.forItem(itemId, phase)
    suspend fun setFlagged(answerId: Long, flagged: Boolean) = answers.setFlagged(answerId, flagged)

    suspend fun startWaiting(itemId: Long): Boolean {
        val item = items.get(itemId) ?: return false
        val until = item.waitUntil ?: return false
        if (item.status != ItemStatus.CONSIDERING) return false
        items.update(item.copy(status = ItemStatus.WAITING))
        scheduler.scheduleDecide(itemId, until)
        return true
    }

    suspend fun markBought(itemId: Long, finalCents: Long): Boolean {
        val item = items.get(itemId) ?: return false
        if (item.status !in OPEN) return false
        val now = clock.instant()
        val due = now.plus(FOLLOW_UP_DELAY)
        items.update(item.copy(status = ItemStatus.BOUGHT, decidedAt = now, finalCostCents = finalCents, followupDueAt = due))
        scheduler.cancelDecide(itemId)
        scheduler.scheduleFollowUp(itemId, due)
        return true
    }

    suspend fun markSkipped(itemId: Long): Boolean {
        val item = items.get(itemId) ?: return false
        if (item.status !in OPEN) return false
        items.update(item.copy(status = ItemStatus.SKIPPED, decidedAt = clock.instant()))
        scheduler.cancelDecide(itemId)
        return true
    }

    suspend fun answerFollowUp(itemId: Long, remembersIt: Boolean, happyWithIt: Boolean): Boolean {
        val item = items.get(itemId) ?: return false
        if (item.status != ItemStatus.BOUGHT || item.followupAnsweredAt != null) return false
        items.update(item.copy(remembersIt = remembersIt, happyWithIt = happyWithIt, followupAnsweredAt = clock.instant()))
        return true
    }

    suspend fun deleteItem(itemId: Long) {
        items.delete(itemId)
        scheduler.cancelAll(itemId)
    }

    fun observeInProgress(): Flow<List<Item>> = items.observeInProgress()
    fun observeWaiting(): Flow<List<Item>> = items.observeWaiting()
    fun observeDecided(): Flow<List<Item>> = items.observeDecided()
    fun observeFollowUpsDue(now: Instant): Flow<List<Item>> = items.observeFollowUpsDue(now)
    fun observeSkippedTotals(): Flow<List<CurrencyTotal>> = items.observeSkippedTotals()

    companion object {
        val FOLLOW_UP_DELAY: Duration = Duration.ofDays(90)
        private val OPEN = setOf(ItemStatus.CONSIDERING, ItemStatus.WAITING)
    }
}
