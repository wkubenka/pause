package com.astute.pause.reminders

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeepLinkTest {
    @Test fun parses_targets() {
        val intent = Intent().putExtra(DeepLink.EXTRA_ITEM_ID, 3L).putExtra(DeepLink.EXTRA_TARGET, DeepLink.TARGET_FOLLOWUP)
        assertEquals(DeepLinkTarget.FollowUp(3), DeepLink.parse(intent))
    }

    @Test fun ignores_missing_or_unknown() {
        assertNull(DeepLink.parse(null))
        assertNull(DeepLink.parse(Intent()))
        assertNull(DeepLink.parse(Intent().putExtra(DeepLink.EXTRA_ITEM_ID, 3L).putExtra(DeepLink.EXTRA_TARGET, "bogus")))
    }

    @Test fun ignores_intents_replayed_from_history() {
        val intent = Intent().putExtra(DeepLink.EXTRA_ITEM_ID, 3L).putExtra(DeepLink.EXTRA_TARGET, DeepLink.TARGET_RECHECK)
            .addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        assertNull(DeepLink.parse(intent))
    }
}
