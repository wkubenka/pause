package com.astute.pause.ui.waiting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.pause.data.repository.PurchaseRepository
import com.astute.pause.domain.NowTicker
import com.astute.pause.ui.components.WaitingRowModel
import com.astute.pause.ui.components.waitingRowModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class WaitingViewModel @Inject constructor(
    repo: PurchaseRepository,
    ticker: NowTicker,
    clock: Clock,
) : ViewModel() {
    val rows: StateFlow<List<WaitingRowModel>> =
        combine(repo.observeWaiting(), ticker.now) { items, now -> items.map { waitingRowModel(it, now, clock.zone) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
