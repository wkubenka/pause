package com.astute.pause.ui.followup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.pause.data.local.entity.Item
import com.astute.pause.data.repository.PurchaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FollowUpState(val item: Item? = null, val remembers: Boolean? = null, val happy: Boolean? = null, val loaded: Boolean = false) {
    val missing: Boolean get() = loaded && item == null
    val canSubmit: Boolean get() = item != null && remembers != null && happy != null && !alreadyAnswered
    val alreadyAnswered: Boolean get() = item?.followupAnsweredAt != null
}

@HiltViewModel
class FollowUpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PurchaseRepository,
) : ViewModel() {
    private val itemId: Long = checkNotNull(savedStateHandle["itemId"])
    private val remembers = MutableStateFlow<Boolean?>(null)
    private val happy = MutableStateFlow<Boolean?>(null)
    private var submitting = false

    val state: StateFlow<FollowUpState> = combine(repo.observeItem(itemId), remembers, happy) { item, r, h ->
        FollowUpState(item, r, h, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FollowUpState())

    fun setRemembers(value: Boolean) { remembers.value = value }
    fun setHappy(value: Boolean) { happy.value = value }

    fun submit(onDone: () -> Unit) {
        val r = remembers.value ?: return
        val h = happy.value ?: return
        if (submitting) return
        submitting = true
        viewModelScope.launch { repo.answerFollowUp(itemId, r, h); onDone() }
    }
}
