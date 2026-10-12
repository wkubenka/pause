package com.astute.pause

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BuildSanityTest {
    @Test
    fun package_name_is_astute_pause() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.astute.pause", context.packageName)
    }
}
