package com.astute.pause.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data class CostRoute(val itemId: Long)
@Serializable data class WalkthroughRoute(val itemId: Long, val recheck: Boolean = false)
@Serializable data class SummaryRoute(val itemId: Long, val recheck: Boolean = false)
@Serializable data object WaitingRoute
@Serializable data class FollowUpRoute(val itemId: Long)
@Serializable data object HistoryRoute
@Serializable data class HistoryDetailRoute(val itemId: Long)
@Serializable data object SettingsRoute
@Serializable data object QuestionsRoute
/** questionId 0 = new question. */
@Serializable data class QuestionEditRoute(val questionId: Long)
/** tierId 0 = new tier. */
@Serializable data class TierEditRoute(val tierId: Long)
