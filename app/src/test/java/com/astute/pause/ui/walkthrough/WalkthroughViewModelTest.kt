package com.astute.pause.ui.walkthrough

import androidx.lifecycle.SavedStateHandle
import com.astute.pause.data.local.PauseDatabase
import com.astute.pause.data.local.entity.AnswerPhase
import com.astute.pause.data.repository.ConfigRepository
import com.astute.pause.data.repository.PurchaseRepository
import com.astute.pause.testutil.FakeReminderScheduler
import com.astute.pause.testutil.MainDispatcherRule
import com.astute.pause.testutil.MutableClock
import com.astute.pause.testutil.TestDb
import com.astute.pause.testutil.awaitFirst
import kotlinx.coroutines.runBlocking
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WalkthroughViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private lateinit var db: PauseDatabase
    private lateinit var repo: PurchaseRepository
    private val clock = MutableClock()

    @Before fun setUp() { db = TestDb.create(); repo = PurchaseRepository(db, FakeReminderScheduler(), clock) }
    @After fun tearDown() { db.close() }

    /** Lets a rejected (or wrongly accepted) next() finish its Room write before asserting nothing happened. */
    private fun settle() = Thread.sleep(300)

    private suspend fun item(cents: Long): Long = repo.startItem("Thing", "USD").also { repo.setCost(it, cents) }

    private suspend fun vm(id: Long, recheck: Boolean = false) =
        WalkthroughViewModel(SavedStateHandle(mapOf("itemId" to id, "recheck" to recheck)), repo, clock)
            .also { v -> v.state.awaitFirst { !it.loading } }

    @Test fun small_item_walks_three_questions_then_finishes() = runBlocking<Unit> {
        val id = item(1_000)
        val vm = vm(id)
        assertEquals(3, vm.state.value.questions.size)
        vm.setAnswer("I need it"); vm.next()
        vm.state.awaitFirst { it.index == 1 }
        clock.advance(Duration.ofSeconds(1)); vm.setAnswer("no"); vm.next()
        vm.state.awaitFirst { it.index == 2 }
        clock.advance(Duration.ofSeconds(1)); vm.setAnswer("yes"); vm.setNote("budgeted"); vm.next()
        vm.state.awaitFirst { it.finished }
        val saved = repo.answersFor(id, AnswerPhase.INITIAL)
        assertEquals(listOf("I need it", "no", "yes"), saved.map { it.answer })
        assertEquals("budgeted", saved.last().note)
    }

    @Test fun cannot_continue_without_answer_unless_free_text() = runBlocking<Unit> {
        val vm = vm(item(50_000))
        assertFalse(vm.state.value.canContinue)          // Q1 is a choice
        vm.next()
        assertEquals(0, vm.state.value.index)
        vm.setAnswer("I need it"); vm.next()
        vm.state.awaitFirst { it.index == 1 }             // Q2 is free text
        assertTrue(vm.state.value.canContinue)
    }

    @Test fun resumes_at_first_unanswered_with_previous_answers_kept() = runBlocking<Unit> {
        val id = item(1_000)
        val first = vm(id)
        first.setAnswer("I want the feeling"); first.next()
        first.state.awaitFirst { it.index == 1 }

        val reopened = vm(id)       // e.g. after process death
        assertEquals(1, reopened.state.value.index)
        assertTrue(reopened.back())
        assertEquals("I want the feeling", reopened.state.value.answer)
        assertFalse(reopened.back())
    }

    @Test fun fully_answered_item_reopens_at_last_question_with_answer_restored() = runBlocking<Unit> {
        val id = item(1_000)
        val first = vm(id)
        first.setAnswer("I need it"); first.next(); first.state.awaitFirst { it.index == 1 }
        clock.advance(Duration.ofSeconds(1)); first.setAnswer("no"); first.next(); first.state.awaitFirst { it.index == 2 }
        clock.advance(Duration.ofSeconds(1)); first.setAnswer("yes"); first.next(); first.state.awaitFirst { it.finished }

        val reopened = vm(id)
        assertEquals(2, reopened.state.value.index)
        assertEquals("yes", reopened.state.value.answer)
        assertTrue(reopened.back())
        assertEquals("no", reopened.state.value.answer)
    }

    @Test fun back_keeps_unsaved_draft_when_returning() = runBlocking<Unit> {
        val vm = vm(item(1_000))
        vm.setAnswer("I need it"); vm.next()
        vm.state.awaitFirst { it.index == 1 }
        vm.setAnswer("yes")
        vm.back()
        clock.advance(Duration.ofSeconds(1))
        vm.next()
        vm.state.awaitFirst { it.index == 1 }
        assertEquals("yes", vm.state.value.answer)
    }

    @Test fun empty_question_set_finishes_immediately() = runBlocking<Unit> {
        val config = ConfigRepository(db)
        listOf(1L, 5L, 14L).forEach { config.setActive(it, false) }
        assertTrue(vm(item(1_000)).state.value.finished)
    }

    @Test fun recheck_uses_recheck_questions() = runBlocking<Unit> {
        val id = item(50_000).also { repo.startWaiting(it) }
        assertEquals(listOf(4L, 8L, 15L), vm(id, recheck = true).state.value.questions.map { it.id })
    }

    @Test fun decided_item_is_flagged_as_already_decided() = runBlocking<Unit> {
        val id = item(50_000).also { repo.markSkipped(it) }
        assertTrue(vm(id, recheck = true).state.value.alreadyDecided)
    }

    @Test fun double_next_advances_one_question_and_saves_one_answer() = runBlocking<Unit> {
        val id = item(1_000)
        val vm = vm(id)
        vm.setAnswer("I need it")
        vm.next(); vm.next()
        vm.state.awaitFirst { it.index == 1 }
        assertEquals(1, vm.state.value.index)
        assertEquals(1, repo.answersFor(id, AnswerPhase.INITIAL).size)
    }

    @Test fun next_right_after_advancing_to_free_text_is_ignored() = runBlocking<Unit> {
        val id = item(50_000)
        val vm = vm(id)
        vm.setAnswer("I need it"); vm.next()
        vm.state.awaitFirst { it.index == 1 }             // Q2 is free text, blank allowed
        settle()                                          // let the in-flight guard clear; only the cooldown remains
        vm.next()                                         // clock not advanced
        settle()
        assertEquals(1, vm.state.value.index)
        assertEquals(1, repo.answersFor(id, AnswerPhase.INITIAL).size)
    }

    @Test fun next_at_exactly_500ms_after_advancing_is_accepted() = runBlocking<Unit> {
        val id = item(50_000)
        val vm = vm(id)
        vm.setAnswer("I need it"); vm.next()
        vm.state.awaitFirst { it.index == 1 }
        settle()
        clock.advance(Duration.ofMillis(499)); vm.next()
        settle()
        assertEquals(1, vm.state.value.index)
        clock.advance(Duration.ofMillis(1)); vm.setAnswer("no"); vm.next()
        vm.state.awaitFirst { it.index == 2 }
        assertEquals(2, repo.answersFor(id, AnswerPhase.INITIAL).size)
    }
}
