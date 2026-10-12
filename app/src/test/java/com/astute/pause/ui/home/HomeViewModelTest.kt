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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    private suspend fun homeWithItem(): Triple<HomeViewModel, Long, com.astute.pause.data.local.entity.Item> {
        val id = repo.startItem("Lamp", "USD")
        val vm = HomeViewModel(repo, FixedTicker(clock.now), clock)
        vm.state.awaitFirst { it.inProgress.isNotEmpty() }
        return Triple(vm, id, repo.item(id)!!)
    }

    @Test fun discard_hides_item_but_keeps_row() = runBlocking<Unit> {
        val (vm, id, item) = homeWithItem()
        vm.discard(item)
        vm.state.awaitFirst { it.inProgress.isEmpty() }
        assertEquals(id, vm.pendingDiscard.value!!.id)
        assertNotNull(repo.item(id))
    }

    @Test fun undo_restores_item() = runBlocking<Unit> {
        val (vm, id, item) = homeWithItem()
        vm.discard(item)
        vm.state.awaitFirst { it.inProgress.isEmpty() }
        vm.undoDiscard(id)
        vm.state.awaitFirst { it.inProgress.map { i -> i.id } == listOf(id) }
        assertNotNull(repo.item(id))
        assertNull(vm.pendingDiscard.value)
    }

    @Test fun commit_deletes_item() = runBlocking<Unit> {
        val (vm, id, item) = homeWithItem()
        vm.discard(item)
        vm.commitDiscard(id)
        awaitDeleted(id)
        vm.state.awaitFirst { it.inProgress.isEmpty() }
    }

    @Test fun repeated_commit_and_undo_are_safe() = runBlocking<Unit> {
        val (vm, id, item) = homeWithItem()
        vm.discard(item)
        vm.commitDiscard(id)
        vm.commitDiscard(id)
        vm.undoDiscard(id)
        awaitDeleted(id)
        vm.commitDiscard(id)
        vm.undoDiscard(id)
        assertNull(repo.item(id))
        vm.state.awaitFirst { it.inProgress.isEmpty() }

        val other = repo.startItem("Desk", "USD")
        vm.discard(repo.item(other)!!)
        vm.undoDiscard(other)
        vm.commitDiscard(other)
        vm.state.awaitFirst { it.inProgress.map { i -> i.id } == listOf(other) }
        assertNotNull(repo.item(other))
    }

    private suspend fun awaitDeleted(id: Long) {
        repeat(200) { if (repo.item(id) == null) return; kotlinx.coroutines.delay(10) }
        assertNull(repo.item(id))
    }

    @Test fun double_tap_creates_one_item() = runBlocking<Unit> {
        val vm = HomeViewModel(repo, FixedTicker(clock.now), clock)
        val created = java.util.concurrent.CopyOnWriteArrayList<Long>()
        val first = CompletableDeferred<Long>()
        vm.start("Desk") { created.add(it); first.complete(it) }
        vm.start("Desk") { created.add(it) }
        first.await()
        vm.start("Desk") { created.add(it) }
        val items = repo.observeInProgress().first()
        assertEquals(1, items.count { it.name == "Desk" })
        assertEquals(1, created.size)
    }

    @Test fun late_second_tap_after_completion_is_ignored() = runBlocking<Unit> {
        val vm = HomeViewModel(repo, FixedTicker(clock.now), clock)
        val created = java.util.concurrent.CopyOnWriteArrayList<Long>()
        val first = CompletableDeferred<Long>()
        vm.start("Desk") { created.add(it); first.complete(it) }
        first.await()
        vm.start("Desk") { created.add(it) }
        assertEquals(1, created.size)
        assertEquals(1, repo.observeInProgress().first().count { it.name == "Desk" })
    }

    @Test fun start_works_again_after_cooldown() = runBlocking<Unit> {
        val vm = HomeViewModel(repo, FixedTicker(clock.now), clock)
        val first = CompletableDeferred<Long>()
        vm.start("Desk") { first.complete(it) }
        first.await()
        clock.advance(Duration.ofMillis(500))
        val second = CompletableDeferred<Long>()
        vm.start("Chair") { second.complete(it) }
        assertEquals("Chair", repo.item(second.await())!!.name)
    }
}
