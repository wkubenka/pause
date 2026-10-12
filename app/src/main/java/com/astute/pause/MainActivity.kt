package com.astute.pause

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astute.pause.reminders.DeepLink
import com.astute.pause.reminders.DeepLinkTarget
import com.astute.pause.ui.navigation.PauseNavHost
import com.astute.pause.ui.theme.PauseTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val deepLinks = MutableStateFlow<DeepLinkTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) deepLinks.value = DeepLink.parse(intent)
        setContent {
            PauseTheme {
                val deepLink by deepLinks.collectAsStateWithLifecycle()
                PauseNavHost(deepLink = deepLink, onDeepLinkHandled = { deepLinks.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinks.value = DeepLink.parse(intent)
    }
}
