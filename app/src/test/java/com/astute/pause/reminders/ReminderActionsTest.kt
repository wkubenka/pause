package com.astute.pause.reminders

import com.astute.pause.data.local.PauseDatabase
import com.astute.pause.data.repository.PurchaseRepository
import com.astute.pause.data.settings.NotificationConfig
import com.astute.pause.testutil.FakeNotificationSettings
import com.astute.pause.testutil.FakeNotifier
import com.astute.pause.testutil.FakeReminderScheduler
import com.astute.pause.testutil.MutableClock
import com.astute.pause.testutil.TestDb
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class ReminderActionsTest {
    private lateinit var db: PauseDatabase
    private lateinit var repo: PurchaseRepository
    private val scheduler = FakeReminderScheduler()
    private val notifier = FakeNotifier()
    private val settings = FakeNotificationSettings()
    private val clock = MutableClock()
    private lateinit var actions: ReminderActions

    @Before fun setUp() {
        db = TestDb.create()
        repo = PurchaseRepository(db, scheduler, clock)
        actions = ReminderActions(repo, settings, notifier, scheduler, clock)
    }
    @After fun tearDown() { db.close() }

    private suspend fun waitingItem(name: String = "Bike"): Long {
        val id = repo.startItem(name, "USD")
        repo.setCost(id, 50_000)
        repo.startWaiting(id)
        return id
    }

    @Test fun decide_due_notifies_and_schedules_one_nudge_a_day_later() = runBlocking {
        val id = waitingItem()
        actions.onDecideDue(id)
        assertEquals(listOf(PauseNotification.Decide(id, "Bike")), notifier.shown)
        assertEquals(clock.now.plus(Duration.ofDays(1)), scheduler.nudge[id])
    }

    @Test fun nothing_fires_for_item_already_bought_skipped_or_deleted() = runBlocking {
        val bought = waitingItem("A"); repo.markBought(bought, 1)
        val skipped = waitingItem("B"); repo.markSkipped(skipped)
        val deleted = waitingItem("C"); repo.deleteItem(deleted)
        listOf(bought, skipped, deleted).forEach {
            actions.onDecideDue(it)
            actions.onNudgeDue(it)
        }
        assertTrue(notifier.shown.isEmpty())
        assertTrue(scheduler.nudge.isEmpty())
    }

    @Test fun nudge_fires_only_while_still_waiting() = runBlocking {
        val id = waitingItem()
        actions.onNudgeDue(id)
        assertEquals(listOf(PauseNotification.Nudge(id, "Bike")), notifier.shown)
    }

    @Test fun disabled_settings_suppress_notifications() = runBlocking {
        settings.update { NotificationConfig(remindersEnabled = false, nudgeEnabled = false, followUpEnabled = false) }
        val id = waitingItem()
        actions.onDecideDue(id)
        actions.onNudgeDue(id)
        repo.markBought(id, 1)
        actions.onFollowUpDue(id)
        assertTrue(notifier.shown.isEmpty())
        assertNull(scheduler.nudge[id])
    }

    @Test fun follow_up_fires_once_for_bought_item() = runBlocking {
        val id = waitingItem()
        repo.markBought(id, 1)
        actions.onFollowUpDue(id)
        repo.answerFollowUp(id, remembersIt = true, happyWithIt = true)
        actions.onFollowUpDue(id)
        assertEquals(listOf(PauseNotification.FollowUp(id, "Bike")), notifier.shown)
    }
}
