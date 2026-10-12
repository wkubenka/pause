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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
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

    /** Discarded items awaiting Undo, by id; hidden from Home but not yet deleted. Main-thread confined writes. */
    private val pending = MutableStateFlow<Map<Long, Item>>(emptyMap())
    private val _pendingDiscard = MutableStateFlow<Item?>(null)
    /** The most recent discard, for the Undo snackbar. */
    val pendingDiscard: StateFlow<Item?> = _pendingDiscard.asStateFlow()
    private val committing = mutableSetOf<Long>()

    val state: StateFlow<HomeState> = combine(
        combine(repo.observeInProgress(), pending) { items, hidden -> items.filter { it.id !in hidden } },
        combine(repo.observeWaiting(), ticker.now) { items, now -> items.map { waitingRowModel(it, now, clock.zone) } },
        ticker.now.flatMapLatest { repo.observeFollowUpsDue(it) },
    ) { inProgress, waiting, followUps -> HomeState(inProgress, waiting, followUps) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    /** Main-thread confined; blocks a second creation while one is in flight. */
    private var creating = false

    /** When start last succeeded; a repeat within START_COOLDOWN is ignored (a late second tap before navigation lands). */
    @Volatile private var createdAt: Instant? = null

    fun start(name: String, onCreated: (Long) -> Unit) {
        if (name.isBlank() || creating) return
        createdAt?.let { if (clock.instant().isBefore(it.plus(START_COOLDOWN))) return }
        creating = true
        viewModelScope.launch {
            try {
                val id = repo.startItem(name)
                createdAt = clock.instant()
                creating = false
                onCreated(id)
            } finally {
                creating = false
            }
        }
    }

    fun discard(item: Item) {
        pending.value = pending.value + (item.id to item)
        _pendingDiscard.value = item
    }

    fun undoDiscard(itemId: Long) {
        if (itemId in committing || itemId !in pending.value) return
        pending.value = pending.value - itemId
        clearLatest(itemId)
    }

    fun commitDiscard(itemId: Long) {
        if (itemId !in pending.value || !committing.add(itemId)) return
        clearLatest(itemId)
        viewModelScope.launch {
            try {
                repo.deleteItem(itemId)
            } finally {
                committing.remove(itemId)
                pending.value = pending.value - itemId
            }
        }
    }

    private fun clearLatest(itemId: Long) {
        if (_pendingDiscard.value?.id == itemId) _pendingDiscard.value = null
    }

    private companion object {
        val START_COOLDOWN: Duration = Duration.ofMillis(500)
    }
}
