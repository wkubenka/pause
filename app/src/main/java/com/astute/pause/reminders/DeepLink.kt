package com.astute.pause.reminders

import android.content.Intent

sealed interface DeepLinkTarget {
    data class Recheck(val itemId: Long) : DeepLinkTarget
    data class FollowUp(val itemId: Long) : DeepLinkTarget
}

object DeepLink {
    const val EXTRA_ITEM_ID = "com.astute.pause.ITEM_ID"
    const val EXTRA_TARGET = "com.astute.pause.TARGET"
    const val TARGET_RECHECK = "recheck"
    const val TARGET_FOLLOWUP = "followup"

    fun parse(intent: Intent?): DeepLinkTarget? {
        if (intent == null) return null
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L).takeIf { it > 0 } ?: return null
        return when (intent.getStringExtra(EXTRA_TARGET)) {
            TARGET_RECHECK -> DeepLinkTarget.Recheck(itemId)
            TARGET_FOLLOWUP -> DeepLinkTarget.FollowUp(itemId)
            else -> null
        }
    }
}
