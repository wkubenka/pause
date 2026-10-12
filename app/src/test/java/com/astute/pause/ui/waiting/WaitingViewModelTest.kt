package com.astute.pause.ui.waiting

import com.astute.pause.data.local.PauseDatabase
import com.astute.pause.data.repository.PurchaseRepository
import com.astute.pause.testutil.FakeReminderScheduler
import com.astute.pause.testutil.FixedTicker
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
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class WaitingViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private lateinit var db: PauseDatabase
    private lateinit var repo: PurchaseRepository
    private val clock = MutableClock()

    @Before fun setUp() { db = TestDb.create(); repo = PurchaseRepository(db, FakeReminderScheduler(), clock) }
    @After fun tearDown() { db.close() }

    @Test fun sorted_by_decide_on_with_countdowns_that_tick() = runBlocking<Unit> {
        val huge = repo.startItem("Sofa", "USD").also { repo.setCost(it, 60_000); repo.startWaiting(it) }   // 7 days
        val large = repo.startItem("Boots", "USD").also { repo.setCost(it, 20_000); repo.startWaiting(it) } // 3 days
        val ticker = FixedTicker(clock.now)
        val vm = WaitingViewModel(repo, ticker, clock)

        val rows = vm.rows.awaitFirst { it.size == 2 }
        assertEquals(listOf(large, huge), rows.map { it.item.id })
        assertEquals("3d 0h left", rows[0].countdown)

        ticker.now.value = clock.now.plus(Duration.ofDays(3))
        val later = vm.rows.awaitFirst { it.first().ready }
        assertEquals("Ready to decide", later[0].countdown)
        assertEquals("4d 0h left", later[1].countdown)
    }
}
