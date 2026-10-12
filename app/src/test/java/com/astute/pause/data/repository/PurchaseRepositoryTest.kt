package com.astute.pause.data.repository

import com.astute.pause.data.local.PauseDatabase
import com.astute.pause.data.local.entity.AnswerPhase
import com.astute.pause.data.local.entity.CurrencyTotal
import com.astute.pause.data.local.entity.ItemStatus
import com.astute.pause.data.local.entity.boughtEarly
import com.astute.pause.testutil.FakeReminderScheduler
import com.astute.pause.testutil.MutableClock
import com.astute.pause.testutil.TestDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class PurchaseRepositoryTest {
    private lateinit var db: PauseDatabase
    private lateinit var repo: PurchaseRepository
    private val scheduler = FakeReminderScheduler()
    private val clock = MutableClock()

    @Before fun setUp() {
        db = TestDb.create()
        repo = PurchaseRepository(db, scheduler, clock)
    }
    @After fun tearDown() { db.close() }

    @Test fun start_item_trims_name_and_stamps_first_considered() = runBlocking {
        val id = repo.startItem("  Headphones ", "USD")
        val item = repo.item(id)!!
        assertEquals("Headphones", item.name)
        assertEquals(clock.now, item.firstConsideredAt)
        assertEquals(ItemStatus.CONSIDERING, item.status)
    }

    @Test(expected = IllegalArgumentException::class)
    fun start_item_rejects_blank_name(): Unit = runBlocking { repo.startItem("   ", "USD") }

    @Test fun cost_assigns_tier_and_wait_from_first_considered() = runBlocking {
        val id = repo.startItem("Jacket", "USD")
        val firstConsidered = clock.now
        clock.advance(Duration.ofHours(2))
        repo.setCost(id, 15_000)
        val item = repo.item(id)!!
        assertEquals("Large", item.tierName)
        assertEquals(15_000L, item.costCents)
        assertEquals(firstConsidered.plus(Duration.ofHours(72)), item.waitUntil)
    }

    @Test fun small_purchase_has_no_wait_and_three_questions() = runBlocking {
        val id = repo.startItem("Socks", "USD")
        repo.setCost(id, 2_499)
        assertNull(repo.item(id)!!.waitUntil)
        assertEquals(listOf(1L, 5L, 14L), repo.questionsFor(id, AnswerPhase.INITIAL).map { it.id })
        assertEquals(listOf(4L, 8L, 15L), repo.questionsFor(id, AnswerPhase.RECHECK).map { it.id })
    }

    @Test fun set_cost_keeps_notes_unless_given() = runBlocking {
        val id = repo.startItem("Lamp", "USD")
        repo.setCost(id, 3_000, notes = "the brass one")
        repo.setCost(id, 3_500)
        assertEquals("the brass one", repo.item(id)!!.notes)
    }

    @Test fun save_answer_flags_warnings_and_updates_in_place() = runBlocking {
        val id = repo.startItem("Socks", "USD")
        repo.setCost(id, 1_000)
        val own = repo.questionsFor(id, AnswerPhase.INITIAL).first { it.id == 5L }
        repo.saveAnswer(id, own, "yes", "", AnswerPhase.INITIAL)
        repo.saveAnswer(id, own, "no", "checked drawer", AnswerPhase.INITIAL)
        val answers = repo.answersFor(id, AnswerPhase.INITIAL)
        assertEquals(1, answers.size)
        assertEquals("no", answers[0].answer)
        assertEquals("checked drawer", answers[0].note)
        assertFalse(answers[0].flagged)
    }

    @Test fun text_answer_keeps_user_flag_when_re_saved() = runBlocking {
        val id = repo.startItem("Bike", "USD")
        repo.setCost(id, 50_000)
        val problem = repo.questionsFor(id, AnswerPhase.INITIAL).first { it.id == 2L }
        repo.saveAnswer(id, problem, "commute", "", AnswerPhase.INITIAL)
        repo.setFlagged(repo.answersFor(id, AnswerPhase.INITIAL).single().id, true)
        repo.saveAnswer(id, problem, "commute, mostly", "", AnswerPhase.INITIAL)
        assertTrue(repo.answersFor(id, AnswerPhase.INITIAL).single().flagged)
    }

    @Test fun answers_keep_question_text_after_question_is_edited() = runBlocking {
        val id = repo.startItem("Bike", "USD")
        repo.setCost(id, 50_000)
        val q = repo.questionsFor(id, AnswerPhase.INITIAL).first { it.id == 7L }
        repo.saveAnswer(id, q, "yes", "", AnswerPhase.INITIAL)
        db.questionDao().update(q.copy(text = "Edited later?"))
        assertEquals("Is this the right time to buy this?", repo.answersFor(id, AnswerPhase.INITIAL).single().questionText)
    }

    @Test fun start_waiting_schedules_reminder_at_wait_end() = runBlocking {
        val id = repo.startItem("Bike", "USD")
        repo.setCost(id, 50_000)
        assertTrue(repo.startWaiting(id))
        assertEquals(ItemStatus.WAITING, repo.item(id)!!.status)
        assertEquals(repo.item(id)!!.waitUntil, scheduler.decide[id])
    }

    @Test fun start_waiting_is_refused_without_a_wait() = runBlocking {
        val id = repo.startItem("Socks", "USD")
        repo.setCost(id, 500)
        assertFalse(repo.startWaiting(id))
        assertEquals(ItemStatus.CONSIDERING, repo.item(id)!!.status)
    }

    @Test fun buying_early_is_logged_cancels_reminder_and_schedules_one_follow_up() = runBlocking {
        val id = repo.startItem("Bike", "USD")
        repo.setCost(id, 50_000)
        repo.startWaiting(id)
        clock.advance(Duration.ofHours(1))
        assertTrue(repo.markBought(id, 48_000))
        assertFalse(repo.markBought(id, 48_000))   // double tap
        val item = repo.item(id)!!
        assertEquals(ItemStatus.BOUGHT, item.status)
        assertEquals(48_000L, item.finalCostCents)
        assertTrue(item.boughtEarly)
        assertNull(scheduler.decide[id])
        assertEquals(clock.now.plus(Duration.ofDays(90)), scheduler.followUp[id])
        assertEquals(1, scheduler.followUpCalls)
    }

    @Test fun skipping_cancels_reminder_and_counts_toward_money_not_spent() = runBlocking {
        val id = repo.startItem("Bike", "USD")
        repo.setCost(id, 50_000)
        repo.startWaiting(id)
        assertTrue(repo.markSkipped(id))
        assertNull(scheduler.decide[id])
        assertEquals(listOf(CurrencyTotal("USD", 50_000)), repo.observeSkippedTotals().first())
    }

    @Test fun follow_up_is_due_after_ninety_days_and_can_be_answered_once() = runBlocking {
        val id = repo.startItem("Bike", "USD")
        repo.setCost(id, 500)
        repo.markBought(id, 500)
        assertTrue(repo.observeFollowUpsDue(clock.now).first().isEmpty())
        clock.advance(Duration.ofDays(90))
        assertEquals(listOf(id), repo.observeFollowUpsDue(clock.now).first().map { it.id })
        assertTrue(repo.answerFollowUp(id, remembersIt = true, happyWithIt = false))
        assertFalse(repo.answerFollowUp(id, remembersIt = false, happyWithIt = false))
        val item = repo.item(id)!!
        assertEquals(true, item.remembersIt)
        assertEquals(false, item.happyWithIt)
        assertTrue(repo.observeFollowUpsDue(clock.now).first().isEmpty())
    }

    @Test fun delete_removes_item_answers_and_reminders() = runBlocking {
        val id = repo.startItem("Bike", "USD")
        repo.setCost(id, 50_000)
        repo.saveAnswer(id, repo.questionsFor(id, AnswerPhase.INITIAL).first(), "I need it", "", AnswerPhase.INITIAL)
        repo.startWaiting(id)
        repo.deleteItem(id)
        assertNull(repo.item(id))
        assertEquals(0, db.answerDao().countForItem(id))
        assertEquals(listOf(id), scheduler.cancelledAll)
    }
}
