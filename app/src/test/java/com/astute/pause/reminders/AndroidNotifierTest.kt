package com.astute.pause.reminders

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AndroidNotifierTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Test fun decide_notification_uses_spec_text_and_deep_links_to_recheck() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        AndroidNotifier(app).show(PauseNotification.Decide(42, "standing desk"))

        val manager = app.getSystemService(NotificationManager::class.java)
        val notification = shadowOf(manager).allNotifications.single()
        assertEquals("Still want the standing desk?", notification.extras.getString(Notification.EXTRA_TITLE))
        val intent = shadowOf(notification.contentIntent).savedIntent
        assertEquals(DeepLinkTarget.Recheck(42), DeepLink.parse(intent))
    }

    @Test fun follow_up_deep_links_to_follow_up() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        AndroidNotifier(app).show(PauseNotification.FollowUp(5, "bike"))
        val manager = app.getSystemService(NotificationManager::class.java)
        val intent = shadowOf(shadowOf(manager).allNotifications.single().contentIntent).savedIntent
        assertEquals(DeepLinkTarget.FollowUp(5), DeepLink.parse(intent))
    }

    @Test fun nothing_is_posted_without_permission() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        AndroidNotifier(app).show(PauseNotification.Nudge(1, "x"))
        val manager = app.getSystemService(NotificationManager::class.java)
        assertEquals(0, shadowOf(manager).allNotifications.size)
    }
}
