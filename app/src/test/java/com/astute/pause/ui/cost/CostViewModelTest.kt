package com.astute.pause.ui.cost

import androidx.lifecycle.SavedStateHandle
import com.astute.pause.data.local.PauseDatabase
import com.astute.pause.data.repository.ConfigRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class CostViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private lateinit var db: PauseDatabase
    private lateinit var repo: PurchaseRepository
    private val clock = MutableClock()

    @Before fun setUp() { db = TestDb.create(); repo = PurchaseRepository(db, FakeReminderScheduler(), clock) }
    @After fun tearDown() { db.close() }

    private suspend fun vm(): Pair<CostViewModel, Long> {
        val id = repo.startItem("Chair", "USD")
        val vm = CostViewModel(SavedStateHandle(mapOf("itemId" to id)), repo, ConfigRepository(db), clock)
        vm.state.awaitFirst { !it.loading }
        return vm to id
    }

    @Test fun preview_follows_input() = runBlocking<Unit> {
        val (vm, _) = vm()
        vm.setInput("150")
        val preview = vm.state.awaitFirst { it.preview != null }.preview!!
        assertEquals("Large", preview.tierName)
        assertEquals(15, preview.questionCount)
        assertEquals("3 days", preview.waitLabel)
        assertNotNull(preview.decideOn)
        vm.setInput("nope")
        assertNull(vm.state.value.preview)
    }

    @Test fun invalid_price_shows_error_and_does_not_save() = runBlocking<Unit> {
        val (vm, id) = vm()
        vm.setInput("12.345")
        vm.submit { error("should not continue") }
        assertNotNull(vm.state.value.error)
        assertNull(repo.item(id)!!.costCents)
    }

    @Test fun valid_price_saves_cost_tier_and_notes() = runBlocking<Unit> {
        val (vm, id) = vm()
        vm.setInput("$1,299.99")
        vm.setNotes("ergonomic")
        val done = CompletableDeferred<Unit>()
        vm.submit { done.complete(Unit) }
        done.await()
        val item = repo.item(id)!!
        assertEquals(129_999L, item.costCents)
        assertEquals("Huge", item.tierName)
        assertEquals("ergonomic", item.notes)
    }

    @Test fun existing_cost_prefills_input() = runBlocking<Unit> {
        val id = repo.startItem("Chair", "USD")
        repo.setCost(id, 2_500)
        val vm = CostViewModel(SavedStateHandle(mapOf("itemId" to id)), repo, ConfigRepository(db), clock)
        assertEquals("25.00", vm.state.awaitFirst { !it.loading }.input)
    }

    @Test fun double_submit_continues_once() = runBlocking<Unit> {
        val (vm, _) = vm()
        vm.setInput("40")
        var calls = 0
        val done = CompletableDeferred<Unit>()
        vm.submit { calls++; done.complete(Unit) }
        vm.submit { calls++ }
        done.await()
        vm.submit { calls++ }
        assertEquals(1, calls)
    }

    @Test fun late_second_submit_after_completion_is_ignored() = runBlocking<Unit> {
        val (vm, _) = vm()
        vm.setInput("40")
        var calls = 0
        val done = CompletableDeferred<Unit>()
        vm.submit { calls++; done.complete(Unit) }
        done.await()
        vm.submit { calls++ }
        assertEquals(1, calls)
    }

    @Test fun submit_works_again_after_cooldown() = runBlocking<Unit> {
        val (vm, _) = vm()
        vm.setInput("40")
        val first = CompletableDeferred<Unit>()
        vm.submit { first.complete(Unit) }
        first.await()
        clock.advance(Duration.ofMillis(500))
        val second = CompletableDeferred<Unit>()
        vm.submit { second.complete(Unit) }
        second.await()
    }
}
