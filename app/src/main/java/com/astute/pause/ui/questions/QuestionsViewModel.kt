package com.astute.pause.ui.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.pause.data.local.entity.Question
import com.astute.pause.data.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuestionsViewModel @Inject constructor(private val config: ConfigRepository) : ViewModel() {
    val questions: StateFlow<List<Question>> =
        config.observeQuestions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun move(id: Long, up: Boolean) = viewModelScope.launch { config.move(id, up) }
    fun setActive(id: Long, active: Boolean) = viewModelScope.launch { config.setActive(id, active) }
}
