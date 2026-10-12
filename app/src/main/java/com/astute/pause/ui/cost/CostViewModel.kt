package com.astute.pause.ui.cost

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.pause.data.local.entity.Item
import com.astute.pause.data.local.entity.Question
import com.astute.pause.data.local.entity.Tier
import com.astute.pause.data.repository.ConfigRepository
import com.astute.pause.data.repository.PurchaseRepository
import com.astute.pause.domain.Countdown
import com.astute.pause.domain.Money
import com.astute.pause.domain.TierResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

data class TierPreview(val tierName: String?, val questionCount: Int, val waitLabel: String, val decideOn: String?)

data class CostState(
    val loading: Boolean = true,
    val itemName: String = "",
    val input: String = "",
    val notes: String = "",
    val preview: TierPreview? = null,
    val error: String? = null,
)

@HiltViewModel
class CostViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PurchaseRepository,
    private val config: ConfigRepository,
    private val clock: Clock,
) : ViewModel() {
    private val itemId: Long = checkNotNull(savedStateHandle["itemId"])
    private val _state = MutableStateFlow(CostState())
    val state: StateFlow<CostState> = _state.asStateFlow()

    private var item: Item? = null
    private var tiers: List<Tier> = emptyList()
    private var questions: List<Question> = emptyList()

    /** Main-thread confined; ignores a second submit while a save is in flight. */
    private var submitting = false

    init {
        viewModelScope.launch {
            item = repo.item(itemId)
            tiers = config.tiers()
            questions = config.questions()
            val current = item
            val input = current?.costCents?.let(Money::toInput) ?: ""
            _state.value = CostState(
                loading = false,
                itemName = current?.name.orEmpty(),
                input = input,
                notes = current?.notes.orEmpty(),
                preview = preview(input),
            )
        }
    }

    fun setInput(value: String) = _state.update { it.copy(input = value, preview = preview(value), error = null) }
    fun setNotes(value: String) = _state.update { it.copy(notes = value) }

    fun submit(onDone: () -> Unit) {
        if (submitting) return
        val cents = Money.parseToCents(_state.value.input)
        if (cents == null) {
            _state.update { it.copy(error = "Enter the full price as a number, like 49.99.") }
            return
        }
        submitting = true
        viewModelScope.launch {
            try {
                repo.setCost(itemId, cents, _state.value.notes)
                onDone()
            } finally {
                submitting = false
            }
        }
    }

    private fun preview(input: String): TierPreview? {
        val cents = Money.parseToCents(input) ?: return null
        val first = item?.firstConsideredAt ?: return null
        val tier = TierResolver.resolve(tiers, cents)
        val until = TierResolver.waitUntil(first, tier)
        return TierPreview(
            tierName = tier?.name,
            questionCount = TierResolver.initialQuestions(tier, questions).size,
            waitLabel = Countdown.waitLength(tier?.waitHours ?: 0),
            decideOn = until?.let { Countdown.decideOn(it, clock.zone) },
        )
    }
}
