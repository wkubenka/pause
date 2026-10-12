package com.astute.pause.testutil

import com.astute.pause.reminders.Notifier
import com.astute.pause.reminders.PauseNotification

class FakeNotifier : Notifier {
    val shown = mutableListOf<PauseNotification>()
    val cancelled = mutableListOf<Long>()
    override fun show(notification: PauseNotification) { shown += notification }
    override fun cancel(itemId: Long) { cancelled += itemId }
}
