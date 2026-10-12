package com.astute.pause.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.astute.pause.reminders.DeepLinkTarget
import com.astute.pause.ui.cost.CostScreen
import com.astute.pause.ui.home.HomeScreen
import com.astute.pause.ui.walkthrough.WalkthroughScreen

@Composable
fun PauseNavHost(deepLink: DeepLinkTarget?, onDeepLinkHandled: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onItemCreated = { id -> navController.navigate(CostRoute(id)) },
                onResume = { item -> navController.navigate(if (item.costCents == null) CostRoute(item.id) else WalkthroughRoute(item.id)) },
                onRecheck = { id -> navController.navigate(WalkthroughRoute(id, recheck = true)) },
                onFollowUp = {},
                onSeeAllWaiting = {},
                onHistory = {},
                onSettings = {},
            )
        }
        composable<CostRoute> { entry ->
            val itemId = entry.toRoute<CostRoute>().itemId
            CostScreen(
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(WalkthroughRoute(itemId)) },
            )
        }
        composable<WalkthroughRoute> { entry ->
            val route = entry.toRoute<WalkthroughRoute>()
            WalkthroughScreen(
                recheck = route.recheck,
                onExit = { navController.popBackStack() },
                onFinished = {},
            )
        }
    }
}
