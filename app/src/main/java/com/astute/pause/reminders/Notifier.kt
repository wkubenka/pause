package com.astute.pause.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.astute.pause.MainActivity
import com.astute.pause.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PauseNotification {
    val itemId: Long
    val itemName: String

    data class Decide(override val itemId: Long, override val itemName: String) : PauseNotification
    data class Nudge(override val itemId: Long, override val itemName: String) : PauseNotification
    data class FollowUp(override val itemId: Long, override val itemName: String) : PauseNotification
}

interface Notifier {
    fun show(notification: PauseNotification)
}

@Singleton
class AndroidNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : Notifier {

    override fun show(notification: PauseNotification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannels()

        val (channel, title, text, target, slot) = when (notification) {
            is PauseNotification.Decide -> Spec(
                CHANNEL_DECIDE, "Still want the ${notification.itemName}?",
                "Your wait is over. Take a look whenever you're ready.", DeepLink.TARGET_RECHECK, 0,
            )
            is PauseNotification.Nudge -> Spec(
                CHANNEL_DECIDE, "${notification.itemName} is still here",
                "No rush. Not now doesn't have to mean never.", DeepLink.TARGET_RECHECK, 1,
            )
            is PauseNotification.FollowUp -> Spec(
                CHANNEL_FOLLOWUP, "Looking back at ${notification.itemName}",
                "Two quick questions about something you bought 90 days ago.", DeepLink.TARGET_FOLLOWUP, 2,
            )
        }
        val requestCode = (notification.itemId * 3 + slot).toInt()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(DeepLink.EXTRA_ITEM_ID, notification.itemId)
            putExtra(DeepLink.EXTRA_TARGET, target)
        }
        val pending = PendingIntent.getActivity(
            context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val built = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(requestCode, built)
    }

    private data class Spec(val channel: String, val title: String, val text: String, val target: String, val slot: Int)

    private fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DECIDE, "Decision reminders", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "When a wait ends on something you're thinking of buying" }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_FOLLOWUP, "90-day look back", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Two quick questions about past purchases" }
        )
    }

    companion object {
        const val CHANNEL_DECIDE = "decide"
        const val CHANNEL_FOLLOWUP = "followup"
    }
}
