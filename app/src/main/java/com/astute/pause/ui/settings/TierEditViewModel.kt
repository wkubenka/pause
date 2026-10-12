package com.astute.pause.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.pause.data.local.entity.Question
import com.astute.pause.data.local.entity.Tier
import com.astute.pause.data.local.entity.TierMode
import com.astute.pause.data.repository.ConfigRepository
import com.astute.pause.domain.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WaitUnit { HOURS, DAYS }

data class TierEditState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val minPrice: String = "",
    val mode: TierMode = TierMode.FULL,
    val waitAmount: String = "0",
    val waitUnit: WaitUnit = WaitUnit.DAYS,
    val selectedQuestionIds: Set<Long> = emptySet(),
    val questions: List<Question> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class TierEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val config: ConfigRepository,
) : ViewModel() {
    private val tierId: Long = savedStateHandle["tierId"] ?: 0L
    private val _state = MutableStateFlow(TierEditState())
    val state: StateFlow<TierEditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val questions = config.questions().filter { it.active }
            val tier = if (tierId != 0L) config.tier(tierId) else null
            _state.value = if (tier == null) TierEditState(loading = false, questions = questions)
            else {
                val days = tier.waitHours > 0 && tier.waitHours % 24 == 0
                TierEditState(
                    loading = false, isNew = false, name = tier.name, minPrice = Money.toInput(tier.minCents),
                    mode = tier.mode,
                    waitAmount = (if (days) tier.waitHours / 24 else tier.waitHours).toString(),
                    waitUnit = if (days || tier.waitHours == 0) WaitUnit.DAYS else WaitUnit.HOURS,
                    selectedQuestionIds = tier.questionIds.toSet(), questions = questions,
                )
            }
        }
    }

    fun update(transform: (TierEditState) -> TierEditState) = _state.update { transform(it).copy(error = null) }

    fun toggleQuestion(id: Long) = update {
        it.copy(selectedQuestionIds = if (id in it.selectedQuestionIds) it.selectedQuestionIds - id else it.selectedQuestionIds + id)
    }

    /** Main-thread confined; a save or delete in flight (or finished) blocks a second one, so onDone runs once. */
    private var finishing = false

    fun save(onDone: () -> Unit) {
        if (finishing) return
        val s = _state.value
        val name = s.name.trim()
        if (name.isEmpty()) return fail("Give the tier a name.")
        val minCents = Money.parseToCents(s.minPrice) ?: return fail("Enter the starting price, like 25.")
        val amount = s.waitAmount.trim().ifEmpty { "0" }.toLongOrNull()?.takeIf { it >= 0 }
            ?: return fail("Enter the wait as a whole number.")
        if (amount > MAX_WAIT_HOURS) return fail("Keep the wait under a year.")
        val hours = amount * if (s.waitUnit == WaitUnit.DAYS) 24 else 1
        if (hours > MAX_WAIT_HOURS) return fail("Keep the wait under a year.")
        if (s.mode == TierMode.SHORT && s.selectedQuestionIds.isEmpty()) return fail("Pick at least one question for the short list.")
        finishing = true
        viewModelScope.launch {
            try {
                if (config.tiers().any { it.id != tierId && it.minCents == minCents }) {
                    finishing = false
                    fail("Another tier already starts at that price.")
                    return@launch
                }
                config.saveTier(
                    Tier(id = tierId, name = name, minCents = minCents, mode = s.mode, waitHours = hours.toInt(),
                        questionIds = s.selectedQuestionIds.sorted())
                )
            } catch (e: Throwable) {
                finishing = false
                throw e
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (finishing) return
        if (tierId == 0L) return onDone()
        finishing = true
        viewModelScope.launch {
            try {
                config.tier(tierId)?.let { config.deleteTier(it) }
            } catch (e: Throwable) {
                finishing = false
                throw e
            }
            onDone()
        }
    }

    private fun fail(message: String) = _state.update { it.copy(error = message) }

    private companion object {
        const val MAX_WAIT_HOURS = 24L * 366
    }
}
