package com.astute.pause.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.astute.pause.reminders.DeepLinkTarget
import com.astute.pause.ui.cost.CostScreen
import com.astute.pause.ui.followup.FollowUpScreen
import com.astute.pause.ui.history.HistoryDetailScreen
import com.astute.pause.ui.history.HistoryScreen
import com.astute.pause.ui.home.HomeScreen
import com.astute.pause.ui.settings.SettingsScreen
import com.astute.pause.ui.settings.TierEditScreen
import com.astute.pause.ui.summary.SummaryScreen
import com.astute.pause.ui.waiting.WaitingScreen
import com.astute.pause.ui.walkthrough.WalkthroughScreen

@Composable
fun PauseNavHost(deepLink: DeepLinkTarget?, onDeepLinkHandled: () -> Unit) {
    val navController = rememberNavController()
    LaunchedEffect(deepLink) {
        when (deepLink) {
            is DeepLinkTarget.Recheck -> navController.navigate(WalkthroughRoute(deepLink.itemId, recheck = true))
            is DeepLinkTarget.FollowUp -> navController.navigate(FollowUpRoute(deepLink.itemId))
            null -> return@LaunchedEffect
        }
        onDeepLinkHandled()
    }

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onItemCreated = { id -> navController.navigate(CostRoute(id)) },
                onResume = { item -> navController.navigate(if (item.costCents == null) CostRoute(item.id) else WalkthroughRoute(item.id)) },
                onRecheck = { id -> navController.navigate(WalkthroughRoute(id, recheck = true)) },
                onFollowUp = { id -> navController.navigate(FollowUpRoute(id)) },
                onSeeAllWaiting = { navController.navigate(WaitingRoute) },
                onHistory = { navController.navigate(HistoryRoute) },
                onSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onEditTier = { id -> navController.navigate(TierEditRoute(id)) },
                onQuestions = {},
            )
        }
        composable<TierEditRoute> {
            TierEditScreen(onDone = { navController.popBackStack() })
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
                onFinished = {
                    navController.navigate(SummaryRoute(route.itemId, route.recheck)) {
                        popUpTo<WalkthroughRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<WaitingRoute> {
            WaitingScreen(
                onBack = { navController.popBackStack() },
                onRecheck = { id -> navController.navigate(WalkthroughRoute(id, recheck = true)) },
            )
        }
        composable<SummaryRoute> {
            SummaryScreen(
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack(HomeRoute, inclusive = false) },
            )
        }
        composable<FollowUpRoute> {
            FollowUpScreen(
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack(HomeRoute, inclusive = false) },
            )
        }
        composable<HistoryRoute> {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpen = { id -> navController.navigate(HistoryDetailRoute(id)) },
            )
        }
        composable<HistoryDetailRoute> {
            HistoryDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
