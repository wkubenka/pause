package com.astute.pause.reminders

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val clock: Clock,
    private val notifier: Notifier,
) : ReminderScheduler {

    override fun scheduleDecide(itemId: Long, at: Instant) = enqueue(ReminderWorker.KIND_DECIDE, itemId, at)
    override fun scheduleNudge(itemId: Long, at: Instant) = enqueue(ReminderWorker.KIND_NUDGE, itemId, at)
    override fun scheduleFollowUp(itemId: Long, at: Instant) = enqueue(ReminderWorker.KIND_FOLLOWUP, itemId, at)

    override fun cancelDecide(itemId: Long) {
        workManager.cancelUniqueWork(workName(ReminderWorker.KIND_DECIDE, itemId))
        workManager.cancelUniqueWork(workName(ReminderWorker.KIND_NUDGE, itemId))
        notifier.cancel(itemId)
    }

    override fun cancelAll(itemId: Long) {
        cancelDecide(itemId)
        workManager.cancelUniqueWork(workName(ReminderWorker.KIND_FOLLOWUP, itemId))
    }

    private fun enqueue(kind: String, itemId: Long, at: Instant) {
        val delayMillis = Duration.between(clock.instant(), at).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_ITEM_ID to itemId, ReminderWorker.KEY_KIND to kind))
            .build()
        workManager.enqueueUniqueWork(workName(kind, itemId), ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        fun workName(kind: String, itemId: Long) = "$kind-$itemId"
    }
}
