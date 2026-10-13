package com.astute.pause.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.pause.data.local.entity.Answer
import com.astute.pause.data.local.entity.AnswerPhase
import com.astute.pause.data.local.entity.AnswerType
import com.astute.pause.data.local.entity.Item
import com.astute.pause.data.local.entity.ItemStatus
import com.astute.pause.data.repository.PurchaseRepository
import com.astute.pause.domain.Countdown
import com.astute.pause.domain.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

data class SummaryState(
    val item: Item? = null,
    val answers: List<Answer> = emptyList(),
    val recheck: Boolean = false,
    val now: Instant = Instant.EPOCH,
    val decideOn: String? = null,
    val error: String? = null,
) {
    val warningCount: Int get() = answers.count { it.flagged || it.followUpFlagged }
    val isOpen: Boolean get() = item?.status == ItemStatus.CONSIDERING || item?.status == ItemStatus.WAITING

    val canStartWait: Boolean
        get() {
            val i = item ?: return false
            val until = i.waitUntil ?: return false
            return !recheck && i.status == ItemStatus.CONSIDERING && until.isAfter(now)
        }

    val waitAlreadyOver: Boolean
        get() {
            val until = item?.waitUntil ?: return false
            return !until.isAfter(now)
        }
}

@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PurchaseRepository,
    private val clock: Clock,
) : ViewModel() {
    private val itemId: Long = checkNotNull(savedStateHandle["itemId"])
    private val recheck: Boolean = savedStateHandle["recheck"] ?: false
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<SummaryState> = combine(
        repo.observeItem(itemId),
        repo.observeAnswers(itemId, if (recheck) AnswerPhase.RECHECK else AnswerPhase.INITIAL),
        error,
    ) { item, answers, err ->
        SummaryState(
            item = item, answers = answers, recheck = recheck, now = clock.instant(),
            decideOn = item?.waitUntil?.let { Countdown.decideOn(it, clock.zone) }, error = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryState(recheck = recheck))

    fun toggleFlag(answer: Answer) {
        if (answer.answerType != AnswerType.TEXT) return
        viewModelScope.launch { repo.setFlagged(answer.id, !answer.flagged) }
    }

    fun toggleFollowUpFlag(answer: Answer) {
        if (answer.followUp.isBlank()) return
        viewModelScope.launch { repo.setFollowUpFlagged(answer.id, !answer.followUpFlagged) }
    }

    fun startWaiting(onDone: () -> Unit) {
        viewModelScope.launch { if (repo.startWaiting(itemId)) onDone() }
    }

    fun markBought(finalCostInput: String, onDone: () -> Unit) {
        val cents = Money.parseToCents(finalCostInput)
        if (cents == null) {
            error.value = "Enter what you paid as a number, like 49.99."
            return
        }
        error.value = null
        viewModelScope.launch { repo.markBought(itemId, cents); onDone() }
    }

    fun markSkipped(onDone: () -> Unit) {
        viewModelScope.launch { repo.markSkipped(itemId); onDone() }
    }
}
