package com.astute.pause.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NotificationSettingsTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun defaults_on_and_updates_persist() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tmp.root, "settings.preferences_pb")
        }
        val settings = DataStoreNotificationSettings(store)
        assertEquals(NotificationConfig(), settings.config.first())
        settings.update { it.copy(nudgeEnabled = false) }
        assertEquals(NotificationConfig(nudgeEnabled = false), settings.config.first())
    }
}
