package com.astute.pause.ui.summary

import androidx.lifecycle.SavedStateHandle
import com.astute.pause.data.local.PauseDatabase
import com.astute.pause.data.local.entity.AnswerPhase
import com.astute.pause.data.local.entity.ItemStatus
import com.astute.pause.data.repository.PurchaseRepository
import com.astute.pause.testutil.FakeReminderScheduler
import com.astute.pause.testutil.MainDispatcherRule
import com.astute.pause.testutil.MutableClock
import com.astute.pause.testutil.TestDb
import com.astute.pause.testutil.awaitFirst
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class SummaryViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private lateinit var db: PauseDatabase
    private lateinit var repo: PurchaseRepository
    private val scheduler = FakeReminderScheduler()
    private val clock = MutableClock()

    @Before fun setUp() { db = TestDb.create(); repo = PurchaseRepository(db, scheduler, clock) }
    @After fun tearDown() { db.close() }

    private fun vm(id: Long, recheck: Boolean = false) =
        SummaryViewModel(SavedStateHandle(mapOf("itemId" to id, "recheck" to recheck)), repo, clock)

    private suspend fun answered(cents: Long): Long {
        val id = repo.startItem("Desk", "USD")
        repo.setCost(id, cents)
        val qs = repo.questionsFor(id, AnswerPhase.INITIAL)
        repo.saveAnswer(id, qs.first { it.id == 1L }, "I want the feeling", "", AnswerPhase.INITIAL)  // warning
        repo.saveAnswer(id, qs.first { it.id == 5L }, "no", "", AnswerPhase.INITIAL)                  // fine
        repo.saveAnswer(id, qs.first { it.id == 14L }, "no", "", AnswerPhase.INITIAL)                 // warning
        return id
    }

    @Test fun counts_warning_signs() = runBlocking<Unit> {
        val state = vm(answered(1_000)).state.awaitFirst { it.answers.size == 3 }
        assertEquals(2, state.warningCount)
    }

    @Test fun only_text_answers_can_be_flagged_by_user() = runBlocking<Unit> {
        val id = repo.startItem("Desk", "USD").also { repo.setCost(it, 50_000) }
        val qs = repo.questionsFor(id, AnswerPhase.INITIAL)
        repo.saveAnswer(id, qs.first { it.id == 2L }, "not sure", "", AnswerPhase.INITIAL)
        repo.saveAnswer(id, qs.first { it.id == 7L }, "yes", "", AnswerPhase.INITIAL)
        val vm = vm(id)
        val s = vm.state.awaitFirst { it.answers.size == 2 }
        s.answers.forEach { vm.toggleFlag(it) }
        val after = vm.state.awaitFirst { it.warningCount == 1 }
        assertTrue(after.answers.first { it.questionId == 2L }.flagged)
        assertFalse(after.answers.first { it.questionId == 7L }.flagged)
    }

    @Test fun can_start_wait_while_wait_is_ahead() = runBlocking<Unit> {
        val id = answered(50_000)
        val s = vm(id).state.awaitFirst { it.item != null }
        assertTrue(s.canStartWait)
        assertFalse(s.waitAlreadyOver)
        assertNotNull(s.decideOn)
    }

    @Test fun wait_already_elapsed_hides_start_wait() = runBlocking<Unit> {
        val id = answered(50_000)
        clock.advance(Duration.ofDays(4))     // Large tier = 3 days from first considered
        val s = vm(id).state.awaitFirst { it.item != null }
        assertFalse(s.canStartWait)
        assertTrue(s.waitAlreadyOver)
    }

    @Test fun no_wait_tier_cannot_start_wait() = runBlocking<Unit> {
        val s = vm(answered(1_000)).state.awaitFirst { it.item != null }
        assertFalse(s.canStartWait)
        assertFalse(s.waitAlreadyOver)
    }

    @Test fun start_waiting_moves_item_to_waiting() = runBlocking<Unit> {
        val id = answered(50_000)
        val vm = vm(id).also { v -> v.state.awaitFirst { it.item != null } }
        val done = CompletableDeferred<Unit>()
        vm.startWaiting { done.complete(Unit) }
        done.await()
        assertEquals(ItemStatus.WAITING, repo.item(id)!!.status)
    }

    @Test fun bought_rejects_bad_final_cost_and_accepts_good_one() = runBlocking<Unit> {
        val id = answered(50_000)
        val vm = vm(id).also { v -> v.state.awaitFirst { it.item != null } }
        vm.markBought("lots") { error("should not finish") }
        assertNotNull(vm.state.awaitFirst { it.error != null }.error)
        val done = CompletableDeferred<Unit>()
        vm.markBought("480") { done.complete(Unit) }
        done.await()
        assertEquals(48_000L, repo.item(id)!!.finalCostCents)
    }

    @Test fun skipped() = runBlocking<Unit> {
        val id = answered(50_000)
        val vm = vm(id).also { v -> v.state.awaitFirst { it.item != null } }
        val done = CompletableDeferred<Unit>()
        vm.markSkipped { done.complete(Unit) }
        done.await()
        assertEquals(ItemStatus.SKIPPED, repo.item(id)!!.status)
    }
}
