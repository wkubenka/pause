package com.astute.pause.data.local.dao

import com.astute.pause.data.local.PauseDatabase
import com.astute.pause.data.local.entity.Answer
import com.astute.pause.data.local.entity.AnswerPhase
import com.astute.pause.data.local.entity.AnswerType
import com.astute.pause.data.local.entity.CurrencyTotal
import com.astute.pause.data.local.entity.Item
import com.astute.pause.data.local.entity.ItemStatus
import com.astute.pause.data.local.entity.boughtEarly
import com.astute.pause.testutil.TestDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class ItemDaoTest {
    private lateinit var db: PauseDatabase
    private lateinit var dao: ItemDao
    private val t0 = Instant.parse("2026-01-01T00:00:00Z")

    @Before fun setUp() { db = TestDb.create(); dao = db.itemDao() }
    @After fun tearDown() { db.close() }

    private fun item(name: String, status: ItemStatus, cost: Long? = 1000, currency: String = "USD") =
        Item(name = name, firstConsideredAt = t0, costCents = cost, currency = currency, status = status)

    @Test fun waiting_is_sorted_by_decide_on_date() = runBlocking {
        dao.insert(item("late", ItemStatus.WAITING).copy(waitUntil = t0.plusSeconds(500)))
        dao.insert(item("soon", ItemStatus.WAITING).copy(waitUntil = t0.plusSeconds(100)))
        dao.insert(item("other", ItemStatus.CONSIDERING))
        assertEquals(listOf("soon", "late"), dao.observeWaiting().first().map { it.name })
    }

    @Test fun follow_ups_due_only_when_due_and_unanswered() = runBlocking {
        val due = t0.plusSeconds(1000)
        dao.insert(item("due", ItemStatus.BOUGHT).copy(followupDueAt = due))
        dao.insert(item("answered", ItemStatus.BOUGHT).copy(followupDueAt = due, followupAnsweredAt = due))
        dao.insert(item("future", ItemStatus.BOUGHT).copy(followupDueAt = due.plusSeconds(1)))
        assertEquals(listOf("due"), dao.observeFollowUpsDue(due).first().map { it.name })
    }

    @Test fun skipped_totals_group_by_currency() = runBlocking {
        dao.insert(item("a", ItemStatus.SKIPPED, 1000))
        dao.insert(item("b", ItemStatus.SKIPPED, 2550))
        dao.insert(item("c", ItemStatus.SKIPPED, 700, "EUR"))
        dao.insert(item("d", ItemStatus.BOUGHT, 99999))
        dao.insert(item("e", ItemStatus.SKIPPED, null))
        assertEquals(
            listOf(CurrencyTotal("EUR", 700), CurrencyTotal("USD", 3550)),
            dao.observeSkippedTotals().first(),
        )
    }

    @Test fun deleting_item_cascades_to_answers() = runBlocking {
        val id = dao.insert(item("x", ItemStatus.CONSIDERING))
        db.answerDao().insert(
            Answer(itemId = id, questionId = 1, questionText = "Q", answerType = AnswerType.YES_NO,
                answer = "yes", answeredAt = t0, phase = AnswerPhase.INITIAL)
        )
        dao.delete(id)
        assertEquals(0, db.answerDao().countForItem(id))
    }

    @Test fun bought_early_is_derived_from_decision_and_wait() {
        val base = item("x", ItemStatus.BOUGHT).copy(waitUntil = t0.plusSeconds(100))
        assertTrue(base.copy(decidedAt = t0.plusSeconds(50)).boughtEarly)
        assertFalse(base.copy(decidedAt = t0.plusSeconds(100)).boughtEarly)
        assertFalse(base.copy(decidedAt = t0.plusSeconds(50), status = ItemStatus.SKIPPED).boughtEarly)
        assertFalse(base.copy(waitUntil = null, decidedAt = t0).boughtEarly)
    }

    @Test fun answers_are_unique_per_item_question_and_phase() = runBlocking {
        val id = dao.insert(item("x", ItemStatus.CONSIDERING))
        fun answer(phase: AnswerPhase) = Answer(
            itemId = id, questionId = 1, questionText = "Q", answerType = AnswerType.YES_NO,
            answer = "yes", answeredAt = t0, phase = phase,
        )
        db.answerDao().insert(answer(AnswerPhase.INITIAL))
        try {
            db.answerDao().insert(answer(AnswerPhase.INITIAL))
            org.junit.Assert.fail("expected SQLiteConstraintException")
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            // expected
        }
        db.answerDao().insert(answer(AnswerPhase.RECHECK))
        assertEquals(2, db.answerDao().countForItem(id))
    }
}
