package com.astute.pause.ui.home

import com.astute.pause.data.local.PauseDatabase
import com.astute.pause.data.repository.PurchaseRepository
import com.astute.pause.testutil.FakeReminderScheduler
import com.astute.pause.testutil.FixedTicker
import com.astute.pause.testutil.MainDispatcherRule
import com.astute.pause.testutil.MutableClock
import com.astute.pause.testutil.TestDb
import com.astute.pause.testutil.awaitFirst
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private lateinit var db: PauseDatabase
    private lateinit var repo: PurchaseRepository
    private val clock = MutableClock()

    @Before fun setUp() { db = TestDb.create(); repo = PurchaseRepository(db, FakeReminderScheduler(), clock) }
    @After fun tearDown() { db.close() }

    @Test fun groups_in_progress_waiting_and_follow_ups() = runBlocking {
        val considering = repo.startItem("Lamp", "USD")
        val waiting = repo.startItem("Bike", "USD").also { repo.setCost(it, 50_000); repo.startWaiting(it) }
        val bought = repo.startItem("Mug", "USD").also { repo.setCost(it, 900); repo.markBought(it, 900) }
        clock.advance(Duration.ofDays(91))

        val vm = HomeViewModel(repo, FixedTicker(clock.now), clock)
        val state = vm.state.awaitFirst { it.followUpsDue.isNotEmpty() && it.waiting.isNotEmpty() }

        assertEquals(listOf(considering), state.inProgress.map { it.id })
        assertEquals(listOf(waiting), state.waiting.map { it.item.id })
        assertEquals("Ready to decide", state.waiting.single().countdown)
        assertEquals(listOf(bought), state.followUpsDue.map { it.id })
    }

    @Test fun start_creates_item_and_ignores_blank_names() = runBlocking {
        val vm = HomeViewModel(repo, FixedTicker(clock.now), clock)
        vm.start("   ") { error("should not create") }
        val created = CompletableDeferred<Long>()
        vm.start("Desk") { created.complete(it) }
        assertEquals("Desk", repo.item(created.await())!!.name)
    }

    @Test fun discard_deletes_item() = runBlocking<Unit> {
        val id = repo.startItem("Lamp", "USD")
        val vm = HomeViewModel(repo, FixedTicker(clock.now), clock)
        vm.state.awaitFirst { it.inProgress.isNotEmpty() }
        vm.discard(id)
        vm.state.awaitFirst { it.inProgress.isEmpty() }
    }

    @Test fun double_tap_creates_one_item() = runBlocking<Unit> {
        val vm = HomeViewModel(repo, FixedTicker(clock.now), clock)
        val created = java.util.concurrent.CopyOnWriteArrayList<Long>()
        val first = CompletableDeferred<Long>()
        vm.start("Desk") { created.add(it); first.complete(it) }
        vm.start("Desk") { created.add(it) }
        first.await()
        val items = repo.observeInProgress().first()
        assertEquals(1, items.count { it.name == "Desk" })
        assertEquals(1, created.size)
    }
}
