package com.astute.pause.testutil

import com.astute.pause.reminders.ReminderScheduler
import java.time.Instant

class FakeReminderScheduler : ReminderScheduler {
    val decide = mutableMapOf<Long, Instant>()
    val nudge = mutableMapOf<Long, Instant>()
    val followUp = mutableMapOf<Long, Instant>()
    val cancelledAll = mutableListOf<Long>()
    var followUpCalls = 0

    override fun scheduleDecide(itemId: Long, at: Instant) { decide[itemId] = at }
    override fun scheduleNudge(itemId: Long, at: Instant) { nudge[itemId] = at }
    override fun cancelDecide(itemId: Long) { decide.remove(itemId); nudge.remove(itemId) }
    override fun scheduleFollowUp(itemId: Long, at: Instant) { followUp[itemId] = at; followUpCalls++ }
    override fun cancelAll(itemId: Long) { cancelDecide(itemId); followUp.remove(itemId); cancelledAll += itemId }
}
