package com.astute.pause.ui.questions

import androidx.lifecycle.SavedStateHandle
import com.astute.pause.data.local.PauseDatabase
import com.astute.pause.data.local.entity.AnswerType
import com.astute.pause.data.repository.ConfigRepository
import com.astute.pause.testutil.MainDispatcherRule
import com.astute.pause.testutil.TestDb
import com.astute.pause.testutil.awaitFirst
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuestionEditViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private lateinit var db: PauseDatabase
    private lateinit var config: ConfigRepository

    @Before fun setUp() { db = TestDb.create(); config = ConfigRepository(db) }
    @After fun tearDown() { db.close() }

    private suspend fun vm(id: Long) =
        QuestionEditViewModel(SavedStateHandle(mapOf("questionId" to id)), config).also { v -> v.state.awaitFirst { !it.loading } }

    @Test fun choice_needs_two_options() = runBlocking<Unit> {
        val vm = vm(0)
        vm.update { it.copy(text = "Where from?") }
        vm.setType(AnswerType.CHOICE)
        vm.update { it.copy(optionsText = "Only one\n\n") }
        vm.save { error("should not save") }
        assertNotNull(vm.state.value.error)
    }

    @Test fun blank_text_is_rejected() = runBlocking<Unit> {
        val vm = vm(0)
        vm.save { error("should not save") }
        assertNotNull(vm.state.value.error)
    }

    @Test fun changing_type_clears_warnings() = runBlocking<Unit> {
        val vm = vm(5)        // yes/no, warning "yes"
        assertEquals(setOf("yes"), vm.state.value.warningAnswers)
        vm.setType(AnswerType.TEXT)
        assertTrue(vm.state.value.warningAnswers.isEmpty())
    }

    @Test fun edits_existing_question_keeping_its_position() = runBlocking<Unit> {
        val vm = vm(6)
        vm.update { it.copy(optionsText = "Borrow it\nRent it\nNone of these") }
        vm.toggleWarning("Rent it")
        val done = CompletableDeferred<Unit>()
        vm.save { done.complete(Unit) }
        done.await()
        val q = config.question(6)!!
        assertEquals(listOf("Borrow it", "Rent it", "None of these"), q.options)
        assertEquals(setOf("Borrow it", "Rent it"), q.warningAnswers.toSet())   // stale "Buy second-hand" pruned
        assertEquals(6, q.sortOrder)
    }

    @Test fun double_tap_on_save_runs_onDone_once_and_creates_one_question() = runBlocking<Unit> {
        val before = config.observeQuestions().first().size
        val vm = vm(0)
        vm.update { it.copy(text = "Do I have room for it?") }
        var done = 0
        vm.save { done++ }
        vm.save { done++ }
        vm.state.awaitFirst { true }
        kotlinx.coroutines.withTimeout(5_000) { while (done == 0) kotlinx.coroutines.delay(10) }
        kotlinx.coroutines.delay(200)
        assertEquals(1, done)
        assertEquals(before + 1, config.observeQuestions().first().size)
    }
}
