package com.astute.pause.ui.walkthrough

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.pause.data.local.entity.AnswerPhase
import com.astute.pause.data.local.entity.AnswerType
import com.astute.pause.data.local.entity.ItemStatus
import com.astute.pause.data.local.entity.Question
import com.astute.pause.data.repository.PurchaseRepository
import com.astute.pause.domain.AnswerEvaluator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

data class WalkthroughState(
    val loading: Boolean = true,
    val itemName: String = "",
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val answer: String = "",
    val note: String = "",
    val followUp: String = "",
    val finished: Boolean = false,
    val alreadyDecided: Boolean = false,
) {
    val current: Question? get() = questions.getOrNull(index)
    /** Shown under the choices once an option with a follow-up is picked. Answering it is optional. */
    val followUpPrompt: String? get() = current?.let { AnswerEvaluator.followUpPrompt(it, answer) }
    val canContinue: Boolean get() = current?.let { it.answerType == AnswerType.TEXT || answer.isNotBlank() } ?: false
    val progress: Float get() = if (questions.isEmpty()) 1f else (index + 1f) / questions.size
}

@HiltViewModel
class WalkthroughViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PurchaseRepository,
    private val clock: Clock,
) : ViewModel() {
    private val itemId: Long = checkNotNull(savedStateHandle["itemId"])
    private val phase = if (savedStateHandle.get<Boolean>("recheck") == true) AnswerPhase.RECHECK else AnswerPhase.INITIAL
    private val drafts = mutableMapOf<Long, Draft>()

    /** Main-thread confined; ignores next() while a save is in flight so one tap advances one question. */
    private var saving = false

    /** When next() last moved to a new question; taps within ADVANCE_COOLDOWN of it are ignored. Cleared by back(). */
    private var advancedAt: Instant? = null

    private val _state = MutableStateFlow(WalkthroughState())
    val state: StateFlow<WalkthroughState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val item = repo.item(itemId)
            if (item == null || item.status == ItemStatus.BOUGHT || item.status == ItemStatus.SKIPPED) {
                _state.value = WalkthroughState(loading = false, itemName = item?.name.orEmpty(), alreadyDecided = true)
                return@launch
            }
            val questions = repo.questionsFor(itemId, phase)
            val byId = questions.associateBy { it.id }
            repo.answersFor(itemId, phase).forEach { a ->
                // A choice saved before its options were reworded is no longer valid: ask it again.
                val q = byId[a.questionId]
                if (q != null && q.answerType == AnswerType.CHOICE && a.answer !in q.options) return@forEach
                drafts[a.questionId] = Draft(a.answer, a.note, a.followUp)
            }
            val start = questions.indexOfFirst { it.id !in drafts }.takeIf { it >= 0 }
                ?: questions.lastIndex.coerceAtLeast(0) // all answered: land on the last one so "See summary" is one tap
            _state.value = WalkthroughState(
                loading = false, itemName = item.name, questions = questions, index = start,
                finished = questions.isEmpty(),
            ).withDraft()
        }
    }

    fun setAnswer(value: String) = edit { it.copy(answer = value) }
    fun setNote(value: String) = edit { it.copy(note = value) }
    fun setFollowUp(value: String) = edit { it.copy(followUp = value) }

    fun next() {
        val s = _state.value
        val q = s.current ?: return
        if (!s.canContinue || saving) return
        advancedAt?.let { if (clock.instant().isBefore(it.plus(ADVANCE_COOLDOWN))) return }
        saving = true
        viewModelScope.launch {
            try {
                repo.saveAnswer(itemId, q, s.answer, s.note, phase, s.followUp)
                val nextIndex = s.index + 1
                _state.value = if (nextIndex >= s.questions.size) s.copy(finished = true)
                else s.copy(index = nextIndex).withDraft().also { advancedAt = clock.instant() }
            } finally {
                saving = false
            }
        }
    }

    fun back(): Boolean {
        val s = _state.value
        if (s.index == 0) return false
        advancedAt = null
        _state.value = s.copy(index = s.index - 1).withDraft()
        return true
    }

    private fun edit(transform: (WalkthroughState) -> WalkthroughState) {
        val next = transform(_state.value)
        next.current?.let { drafts[it.id] = Draft(next.answer, next.note, next.followUp) }
        _state.value = next
    }

    private fun WalkthroughState.withDraft(): WalkthroughState {
        val q = current ?: return this
        val draft = drafts[q.id] ?: Draft()
        return copy(answer = draft.answer, note = draft.note, followUp = draft.followUp)
    }

    private data class Draft(val answer: String = "", val note: String = "", val followUp: String = "")

    private companion object {
        val ADVANCE_COOLDOWN: Duration = Duration.ofMillis(500)
    }
}
