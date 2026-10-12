package com.astute.pause.reminders

import java.time.Instant

interface ReminderScheduler {
    fun scheduleDecide(itemId: Long, at: Instant)
    fun scheduleNudge(itemId: Long, at: Instant)
    /** Cancels both the decide reminder and its follow-on nudge. */
    fun cancelDecide(itemId: Long)
    fun scheduleFollowUp(itemId: Long, at: Instant)
    fun cancelAll(itemId: Long)
}
