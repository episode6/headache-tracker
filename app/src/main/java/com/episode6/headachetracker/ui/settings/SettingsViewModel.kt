package com.episode6.headachetracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.episode6.headachetracker.data.SettingsRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsState(
    val reminderMinutesText: String = "",
    val isLoaded: Boolean = false,
)

@Inject
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val minutes = settingsRepository.secondPillReminderMinutes.first()
            _state.value = SettingsState(
                reminderMinutesText = minutes.toString(),
                isLoaded = true,
            )
        }
    }

    fun onReminderMinutesChanged(text: String) {
        val digits = text.filter { it.isDigit() }.take(4)
        _state.value = _state.value.copy(reminderMinutesText = digits)
        val minutes = digits.toIntOrNull() ?: return
        viewModelScope.launch {
            settingsRepository.setSecondPillReminderMinutes(minutes)
        }
    }
}
