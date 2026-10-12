package com.astute.pause.reminders

import com.astute.pause.data.local.entity.ItemStatus
import com.astute.pause.data.repository.PurchaseRepository
import com.astute.pause.data.settings.NotificationSettings
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/** Worker-side logic. Every action re-reads the item so stale reminders stay silent. */
class ReminderActions @Inject constructor(
    private val repo: PurchaseRepository,
    private val settings: NotificationSettings,
    private val notifier: Notifier,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
) {
    suspend fun onDecideDue(itemId: Long) {
        val item = repo.item(itemId) ?: return
        if (item.status != ItemStatus.WAITING) return
        val config = settings.config.first()
        if (config.remindersEnabled) notifier.show(PauseNotification.Decide(item.id, item.name))
        if (config.nudgeEnabled) scheduler.scheduleNudge(item.id, clock.instant().plus(NUDGE_AFTER))
    }

    suspend fun onNudgeDue(itemId: Long) {
        val item = repo.item(itemId) ?: return
        if (item.status != ItemStatus.WAITING) return
        if (settings.config.first().nudgeEnabled) notifier.show(PauseNotification.Nudge(item.id, item.name))
    }

    suspend fun onFollowUpDue(itemId: Long) {
        val item = repo.item(itemId) ?: return
        if (item.status != ItemStatus.BOUGHT || item.followupAnsweredAt != null) return
        if (settings.config.first().followUpEnabled) notifier.show(PauseNotification.FollowUp(item.id, item.name))
    }

    private companion object {
        val NUDGE_AFTER: Duration = Duration.ofDays(1)
    }
}
