package com.astute.pause.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.astute.pause.reminders.DeepLinkTarget
import com.astute.pause.ui.home.HomeScreen

@Composable
fun PauseNavHost(deepLink: DeepLinkTarget?, onDeepLinkHandled: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onItemCreated = {},
                onResume = {},
                onRecheck = {},
                onFollowUp = {},
                onSeeAllWaiting = {},
                onHistory = {},
                onSettings = {},
            )
        }
    }
}
