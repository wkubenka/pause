package com.astute.pause.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationConfig(
    val remindersEnabled: Boolean = true,
    val nudgeEnabled: Boolean = true,
    val followUpEnabled: Boolean = true,
)

interface NotificationSettings {
    val config: Flow<NotificationConfig>
    suspend fun update(transform: (NotificationConfig) -> NotificationConfig)
}

@Singleton
class DataStoreNotificationSettings @Inject constructor(
    private val store: DataStore<Preferences>,
) : NotificationSettings {
    override val config: Flow<NotificationConfig> = store.data.map { it.toConfig() }

    override suspend fun update(transform: (NotificationConfig) -> NotificationConfig) {
        store.edit { prefs ->
            val next = transform(prefs.toConfig())
            prefs[REMINDERS] = next.remindersEnabled
            prefs[NUDGE] = next.nudgeEnabled
            prefs[FOLLOW_UP] = next.followUpEnabled
        }
    }

    private fun Preferences.toConfig() = NotificationConfig(
        remindersEnabled = this[REMINDERS] ?: true,
        nudgeEnabled = this[NUDGE] ?: true,
        followUpEnabled = this[FOLLOW_UP] ?: true,
    )

    private companion object {
        val REMINDERS = booleanPreferencesKey("reminders_enabled")
        val NUDGE = booleanPreferencesKey("nudge_enabled")
        val FOLLOW_UP = booleanPreferencesKey("follow_up_enabled")
    }
}
