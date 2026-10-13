package com.astute.pause.ui.history

import androidx.lifecycle.SavedStateHandle
import com.astute.pause.data.local.PauseDatabase
import com.astute.pause.data.local.SeedData
import com.astute.pause.data.local.entity.AnswerPhase
import com.astute.pause.data.repository.ConfigRepository
import com.astute.pause.data.repository.PurchaseRepository
import com.astute.pause.testutil.FakeReminderScheduler
import com.astute.pause.testutil.MainDispatcherRule
import com.astute.pause.testutil.MutableClock
import com.astute.pause.testutil.TestDb
import com.astute.pause.testutil.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class HistoryViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private lateinit var db: PauseDatabase
    private lateinit var repo: PurchaseRepository
    private val clock = MutableClock()
    private lateinit var previousLocale: Locale

    @Before fun setUp() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        db = TestDb.create()
        repo = PurchaseRepository(db, FakeReminderScheduler(), clock)
    }
    @After fun tearDown() { db.close(); Locale.setDefault(previousLocale) }

    @Test fun money_not_spent_and_follow_up_stats() = runBlocking<Unit> {
        repo.startItem("A", "USD").also { repo.setCost(it, 2_000); repo.markSkipped(it) }
        repo.startItem("B", "USD").also { repo.setCost(it, 3_050); repo.markSkipped(it) }
        repo.startItem("C", "USD").also { repo.setCost(it, 900); repo.markBought(it, 900); repo.answerFollowUp(it, true, false) }
        repo.startItem("D", "USD").also { repo.setCost(it, 900); repo.markBought(it, 900); repo.answerFollowUp(it, true, true) }
        repo.startItem("E", "USD")  // still considering: excluded

        val state = HistoryViewModel(repo).state.awaitFirst { it.items.size == 4 }
        assertEquals(listOf("$50.50"), state.notSpent)
        assertEquals(FollowUpStats(answered = 2, remembered = 2, happy = 1), state.followUps)
    }

    @Test fun detail_shows_original_question_text_after_archive_and_edit() = runBlocking<Unit> {
        val id = repo.startItem("Desk", "USD").also { repo.setCost(it, 1_000) }
        val q = repo.questionsFor(id, AnswerPhase.INITIAL).first()
        repo.saveAnswer(id, q, SeedData.WANT_NEED, "", AnswerPhase.INITIAL)
        repo.markBought(id, 1_000)
        val config = ConfigRepository(db)
        config.saveQuestion(q.copy(text = "Rewritten"))
        config.setActive(q.id, false)

        val vm = HistoryDetailViewModel(SavedStateHandle(mapOf("itemId" to id)), repo)
        val state = vm.state.awaitFirst { it.initial.isNotEmpty() }
        assertEquals(q.text, state.initial.single().questionText)
    }

    @Test fun delete_twice_runs_onDone_once() = runBlocking<Unit> {
        val id = repo.startItem("Desk", "USD").also { repo.setCost(it, 1_000); repo.markSkipped(it) }
        val vm = HistoryDetailViewModel(SavedStateHandle(mapOf("itemId" to id)), repo)
        var done = 0
        vm.delete { done++ }
        vm.delete { done++ }
        vm.state.awaitFirst { it.item == null }
        kotlinx.coroutines.delay(200)
        assertEquals(1, done)
    }
}
