package com.astute.pause.testutil

import com.astute.pause.reminders.Notifier
import com.astute.pause.reminders.PauseNotification

class FakeNotifier : Notifier {
    val shown = mutableListOf<PauseNotification>()
    override fun show(notification: PauseNotification) { shown += notification }
}
