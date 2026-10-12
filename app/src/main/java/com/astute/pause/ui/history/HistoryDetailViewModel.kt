package com.astute.pause.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.pause.data.local.entity.Answer
import com.astute.pause.data.local.entity.AnswerPhase
import com.astute.pause.data.local.entity.Item
import com.astute.pause.data.repository.PurchaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryDetailState(
    val item: Item? = null,
    val initial: List<Answer> = emptyList(),
    val recheck: List<Answer> = emptyList(),
)

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PurchaseRepository,
) : ViewModel() {
    private val itemId: Long = checkNotNull(savedStateHandle["itemId"])

    val state: StateFlow<HistoryDetailState> = combine(
        repo.observeItem(itemId),
        repo.observeAnswers(itemId, AnswerPhase.INITIAL),
        repo.observeAnswers(itemId, AnswerPhase.RECHECK),
    ) { item, initial, recheck -> HistoryDetailState(item, initial, recheck) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryDetailState())

    /** Main-thread confined; a second tap must not run onDone (a pop) twice. */
    private var deleting = false

    fun delete(onDone: () -> Unit) {
        if (deleting) return
        deleting = true
        viewModelScope.launch {
            try {
                repo.deleteItem(itemId)
            } catch (e: Throwable) {
                deleting = false
                throw e
            }
            onDone()
        }
    }
}
