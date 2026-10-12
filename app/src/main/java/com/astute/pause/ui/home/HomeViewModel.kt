package com.astute.pause.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.pause.data.local.entity.Item
import com.astute.pause.data.repository.PurchaseRepository
import com.astute.pause.domain.NowTicker
import com.astute.pause.ui.components.WaitingRowModel
import com.astute.pause.ui.components.waitingRowModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

data class HomeState(
    val inProgress: List<Item> = emptyList(),
    val waiting: List<WaitingRowModel> = emptyList(),
    val followUpsDue: List<Item> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: PurchaseRepository,
    ticker: NowTicker,
    private val clock: Clock,
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(
        repo.observeInProgress(),
        combine(repo.observeWaiting(), ticker.now) { items, now -> items.map { waitingRowModel(it, now, clock.zone) } },
        ticker.now.flatMapLatest { repo.observeFollowUpsDue(it) },
    ) { inProgress, waiting, followUps -> HomeState(inProgress, waiting, followUps) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun start(name: String, onCreated: (Long) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch { onCreated(repo.startItem(name)) }
    }

    fun discard(itemId: Long) {
        viewModelScope.launch { repo.deleteItem(itemId) }
    }
}
