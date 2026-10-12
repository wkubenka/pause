package com.astute.pause.ui.walkthrough

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.pause.data.local.entity.AnswerPhase
import com.astute.pause.data.local.entity.AnswerType
import com.astute.pause.data.local.entity.ItemStatus
import com.astute.pause.data.local.entity.Question
import com.astute.pause.data.repository.PurchaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalkthroughState(
    val loading: Boolean = true,
    val itemName: String = "",
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val answer: String = "",
    val note: String = "",
    val finished: Boolean = false,
    val alreadyDecided: Boolean = false,
) {
    val current: Question? get() = questions.getOrNull(index)
    val canContinue: Boolean get() = current?.let { it.answerType == AnswerType.TEXT || answer.isNotBlank() } ?: false
    val progress: Float get() = if (questions.isEmpty()) 1f else (index + 1f) / questions.size
}

@HiltViewModel
class WalkthroughViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PurchaseRepository,
) : ViewModel() {
    private val itemId: Long = checkNotNull(savedStateHandle["itemId"])
    private val phase = if (savedStateHandle.get<Boolean>("recheck") == true) AnswerPhase.RECHECK else AnswerPhase.INITIAL
    private val drafts = mutableMapOf<Long, Pair<String, String>>()

    /** Main-thread confined; ignores next() while a save is in flight so one tap advances one question. */
    private var saving = false

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
            repo.answersFor(itemId, phase).forEach { drafts[it.questionId] = it.answer to it.note }
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

    fun next() {
        val s = _state.value
        val q = s.current ?: return
        if (!s.canContinue || saving) return
        saving = true
        viewModelScope.launch {
            try {
                repo.saveAnswer(itemId, q, s.answer, s.note, phase)
                val nextIndex = s.index + 1
                _state.value = if (nextIndex >= s.questions.size) s.copy(finished = true)
                else s.copy(index = nextIndex).withDraft()
            } finally {
                saving = false
            }
        }
    }

    fun back(): Boolean {
        val s = _state.value
        if (s.index == 0) return false
        _state.value = s.copy(index = s.index - 1).withDraft()
        return true
    }

    private fun edit(transform: (WalkthroughState) -> WalkthroughState) {
        val next = transform(_state.value)
        next.current?.let { drafts[it.id] = next.answer to next.note }
        _state.value = next
    }

    private fun WalkthroughState.withDraft(): WalkthroughState {
        val q = current ?: return this
        val (answer, note) = drafts[q.id] ?: ("" to "")
        return copy(answer = answer, note = note)
    }
}
