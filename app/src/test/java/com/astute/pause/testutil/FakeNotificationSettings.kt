package com.astute.pause.testutil

import com.astute.pause.data.settings.NotificationConfig
import com.astute.pause.data.settings.NotificationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeNotificationSettings(initial: NotificationConfig = NotificationConfig()) : NotificationSettings {
    private val state = MutableStateFlow(initial)
    override val config: Flow<NotificationConfig> = state
    override suspend fun update(transform: (NotificationConfig) -> NotificationConfig) { state.value = transform(state.value) }
}
