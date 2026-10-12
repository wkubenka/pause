package com.astute.pause.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.pause.data.local.entity.Tier
import com.astute.pause.data.local.entity.TierMode
import com.astute.pause.data.repository.ConfigRepository
import com.astute.pause.data.settings.NotificationConfig
import com.astute.pause.data.settings.NotificationSettings
import com.astute.pause.domain.Countdown
import com.astute.pause.domain.Money
import com.astute.pause.domain.TierResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TierRow(val tier: Tier, val range: String, val summary: String)
data class SettingsState(val tiers: List<TierRow> = emptyList(), val notifications: NotificationConfig = NotificationConfig())

@HiltViewModel
class SettingsViewModel @Inject constructor(
    config: ConfigRepository,
    private val notifications: NotificationSettings,
) : ViewModel() {
    private val currency = Money.defaultCurrency()

    val state: StateFlow<SettingsState> = combine(config.observeTiers(), notifications.config) { tiers, n ->
        SettingsState(
            tiers = tiers.map { t ->
                TierRow(
                    tier = t,
                    range = TierResolver.describeRange(tiers, t, currency),
                    summary = "${if (t.mode == TierMode.SHORT) "Short list" else "Full checklist"} · ${Countdown.waitLength(t.waitHours)}",
                )
            },
            notifications = n,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setReminders(on: Boolean) = viewModelScope.launch { notifications.update { it.copy(remindersEnabled = on) } }
    fun setNudge(on: Boolean) = viewModelScope.launch { notifications.update { it.copy(nudgeEnabled = on) } }
    fun setFollowUp(on: Boolean) = viewModelScope.launch { notifications.update { it.copy(followUpEnabled = on) } }
}
