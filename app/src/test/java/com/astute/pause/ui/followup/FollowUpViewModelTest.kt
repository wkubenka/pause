package com.astute.pause.ui.followup

import androidx.lifecycle.SavedStateHandle
import com.astute.pause.data.local.PauseDatabase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FollowUpViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private lateinit var db: PauseDatabase
    private lateinit var repo: PurchaseRepository

    @Before fun setUp() { db = TestDb.create(); repo = PurchaseRepository(db, FakeReminderScheduler(), MutableClock()) }
    @After fun tearDown() { db.close() }

    private suspend fun bought() = repo.startItem("Kettle", "USD").also { repo.setCost(it, 4_000); repo.markBought(it, 4_000) }

    @Test fun needs_both_answers_then_saves() = runBlocking<Unit> {
        val id = bought()
        val vm = FollowUpViewModel(SavedStateHandle(mapOf("itemId" to id)), repo)
        vm.state.awaitFirst { it.item != null }
        vm.setRemembers(true)
        assertFalse(vm.state.value.canSubmit)
        vm.setHappy(false)
        assertTrue(vm.state.value.canSubmit)
        val done = CompletableDeferred<Unit>()
        vm.submit { done.complete(Unit) }
        done.await()
        val item = repo.item(id)!!
        assertEquals(true, item.remembersIt)
        assertEquals(false, item.happyWithIt)
    }

    @Test fun already_answered_is_reported() = runBlocking<Unit> {
        val id = bought().also { repo.answerFollowUp(it, remembersIt = false, happyWithIt = true) }
        val vm = FollowUpViewModel(SavedStateHandle(mapOf("itemId" to id)), repo)
        assertTrue(vm.state.awaitFirst { it.item != null }.alreadyAnswered)
    }

    @Test fun missing_item_is_loaded_and_missing_and_cannot_submit() = runBlocking<Unit> {
        val vm = FollowUpViewModel(SavedStateHandle(mapOf("itemId" to 9_999L)), repo)
        val s = vm.state.awaitFirst { it.loaded }
        assertTrue(s.missing)
        vm.setRemembers(true); vm.setHappy(true)
        assertFalse(vm.state.value.canSubmit)
    }

    @Test fun answered_item_cannot_submit() = runBlocking<Unit> {
        val id = bought().also { repo.answerFollowUp(it, remembersIt = false, happyWithIt = true) }
        val vm = FollowUpViewModel(SavedStateHandle(mapOf("itemId" to id)), repo)
        vm.state.awaitFirst { it.item != null }
        vm.setRemembers(true); vm.setHappy(true)
        assertFalse(vm.state.value.canSubmit)
    }

    @Test fun double_submit_calls_onDone_once() = runBlocking<Unit> {
        val id = bought()
        val vm = FollowUpViewModel(SavedStateHandle(mapOf("itemId" to id)), repo)
        vm.state.awaitFirst { it.item != null }
        vm.setRemembers(true); vm.setHappy(true)
        var calls = 0
        val done = CompletableDeferred<Unit>()
        vm.submit { calls++; done.complete(Unit) }
        vm.submit { calls++; done.complete(Unit) }
        done.await()
        kotlinx.coroutines.delay(100)
        assertEquals(1, calls)
    }
}
