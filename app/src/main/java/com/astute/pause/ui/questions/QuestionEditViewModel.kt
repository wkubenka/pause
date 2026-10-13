package com.astute.pause.ui.questions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.pause.data.local.SeedData
import com.astute.pause.data.local.entity.AnswerType
import com.astute.pause.data.local.entity.Question
import com.astute.pause.data.local.entity.QuestionGroup
import com.astute.pause.data.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestionEditState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val text: String = "",
    val group: QuestionGroup = QuestionGroup.NEED_CLARITY,
    val answerType: AnswerType = AnswerType.YES_NO,
    val optionsText: String = "",
    val warningAnswers: Set<String> = emptySet(),
    /** Choice -> optional follow-up prompt. Entries for choices no longer listed are dropped on save. */
    val followUps: Map<String, String> = emptyMap(),
    val inRecheck: Boolean = false,
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val error: String? = null,
) {
    val options: List<String> get() = optionsText.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    val warningChoices: List<String>
        get() = when (answerType) {
            AnswerType.YES_NO -> listOf(SeedData.YES, SeedData.NO)
            AnswerType.CHOICE -> options
            AnswerType.TEXT -> emptyList()
        }
}

@HiltViewModel
class QuestionEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val config: ConfigRepository,
) : ViewModel() {
    private val questionId: Long = savedStateHandle["questionId"] ?: 0L
    private val _state = MutableStateFlow(QuestionEditState())
    val state: StateFlow<QuestionEditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val q = if (questionId != 0L) config.question(questionId) else null
            _state.value = if (q == null) QuestionEditState(loading = false)
            else QuestionEditState(
                loading = false, isNew = false, text = q.text, group = q.group, answerType = q.answerType,
                optionsText = q.options.joinToString("\n"), warningAnswers = q.warningAnswers.toSet(), followUps = q.followUps,
                inRecheck = q.inRecheck, active = q.active, sortOrder = q.sortOrder,
            )
        }
    }

    fun update(transform: (QuestionEditState) -> QuestionEditState) = _state.update { transform(it).copy(error = null) }
    fun setType(type: AnswerType) = update { it.copy(answerType = type, warningAnswers = emptySet()) }
    fun toggleWarning(answer: String) = update {
        it.copy(warningAnswers = if (answer in it.warningAnswers) it.warningAnswers - answer else it.warningAnswers + answer)
    }

    fun setFollowUp(option: String, prompt: String) = update { it.copy(followUps = it.followUps + (option to prompt)) }

    /** Main-thread confined; a save in flight (or finished) blocks a second one, so onDone runs once. */
    private var finishing = false

    fun save(onDone: () -> Unit) {
        if (finishing) return
        val s = _state.value
        if (s.text.isBlank()) return _state.update { it.copy(error = "Write the question.") }
        if (s.answerType == AnswerType.CHOICE && s.options.size < 2) {
            return _state.update { it.copy(error = "Add at least two choices, one per line.") }
        }
        finishing = true
        viewModelScope.launch {
            try {
                config.saveQuestion(
                    Question(
                        id = questionId, text = s.text, group = s.group, active = s.active, sortOrder = s.sortOrder,
                        answerType = s.answerType, options = s.options,
                        warningAnswers = s.warningAnswers.filter { it in s.warningChoices }, inRecheck = s.inRecheck,
                        followUps = if (s.answerType == AnswerType.CHOICE) s.followUps.filterKeys { it in s.options } else emptyMap(),
                    )
                )
            } catch (e: Throwable) {
                finishing = false
                throw e
            }
            onDone()
        }
    }
}
