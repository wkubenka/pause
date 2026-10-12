package com.astute.pause.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.astute.pause.testutil.MutableClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class WorkManagerReminderSchedulerTest {
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerReminderScheduler
    private val clock = MutableClock()

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context, Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        )
        workManager = WorkManager.getInstance(context)
        scheduler = WorkManagerReminderScheduler(workManager, clock)
    }

    private fun enqueued(kind: String, id: Long) =
        workManager.getWorkInfosForUniqueWork(WorkManagerReminderScheduler.workName(kind, id)).get()
            .filter { it.state == WorkInfo.State.ENQUEUED }

    @Test fun decide_is_delayed_until_wait_end_and_replaced_not_duplicated() {
        scheduler.scheduleDecide(7, clock.now.plus(Duration.ofDays(3)))
        scheduler.scheduleDecide(7, clock.now.plus(Duration.ofDays(3)))
        val infos = enqueued(ReminderWorker.KIND_DECIDE, 7)
        assertEquals(1, infos.size)
        assertEquals(Duration.ofDays(3).toMillis(), infos.single().initialDelayMillis)
    }

    @Test fun past_time_is_scheduled_with_zero_delay() {
        scheduler.scheduleFollowUp(8, clock.now.minus(Duration.ofHours(1)))
        val info = workManager.getWorkInfosForUniqueWork(
            WorkManagerReminderScheduler.workName(ReminderWorker.KIND_FOLLOWUP, 8)
        ).get().single()
        assertEquals(0L, info.initialDelayMillis)
    }

    @Test fun cancel_decide_also_cancels_nudge_but_not_follow_up() {
        scheduler.scheduleDecide(9, clock.now.plus(Duration.ofDays(1)))
        scheduler.scheduleNudge(9, clock.now.plus(Duration.ofDays(2)))
        scheduler.scheduleFollowUp(9, clock.now.plus(Duration.ofDays(90)))
        scheduler.cancelDecide(9)
        assertTrue(enqueued(ReminderWorker.KIND_DECIDE, 9).isEmpty())
        assertTrue(enqueued(ReminderWorker.KIND_NUDGE, 9).isEmpty())
        assertEquals(1, enqueued(ReminderWorker.KIND_FOLLOWUP, 9).size)
        scheduler.cancelAll(9)
        assertTrue(enqueued(ReminderWorker.KIND_FOLLOWUP, 9).isEmpty())
    }
}
