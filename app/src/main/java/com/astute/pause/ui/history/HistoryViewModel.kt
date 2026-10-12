package com.astute.pause.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.pause.data.local.entity.Item
import com.astute.pause.data.repository.PurchaseRepository
import com.astute.pause.domain.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class FollowUpStats(val answered: Int = 0, val remembered: Int = 0, val happy: Int = 0)

data class HistoryState(
    val items: List<Item> = emptyList(),
    val notSpent: List<String> = emptyList(),
    val followUps: FollowUpStats = FollowUpStats(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(repo: PurchaseRepository) : ViewModel() {
    val state: StateFlow<HistoryState> = combine(repo.observeDecided(), repo.observeSkippedTotals()) { items, totals ->
        val answered = items.filter { it.followupAnsweredAt != null }
        HistoryState(
            items = items,
            notSpent = totals.map { Money.format(it.totalCents, it.currency) },
            followUps = FollowUpStats(
                answered = answered.size,
                remembered = answered.count { it.remembersIt == true },
                happy = answered.count { it.happyWithIt == true },
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())
}
