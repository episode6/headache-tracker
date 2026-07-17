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
    val morningCheckInEnabled: Boolean = true,
    val morningCheckInTimeMinutes: Int = SettingsRepository.DEFAULT_MORNING_CHECK_IN_TIME_MINUTES,
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
            _state.value = SettingsState(
                reminderMinutesText = settingsRepository.secondPillReminderMinutes.first().toString(),
                morningCheckInEnabled = settingsRepository.morningCheckInEnabled.first(),
                morningCheckInTimeMinutes = settingsRepository.morningCheckInTimeMinutes.first(),
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

    fun onMorningCheckInToggled(enabled: Boolean) {
        _state.value = _state.value.copy(morningCheckInEnabled = enabled)
        viewModelScope.launch {
            settingsRepository.setMorningCheckInEnabled(enabled)
        }
    }

    fun onMorningCheckInTimeChanged(hour: Int, minute: Int) {
        val minutesOfDay = hour * 60 + minute
        _state.value = _state.value.copy(morningCheckInTimeMinutes = minutesOfDay)
        viewModelScope.launch {
            settingsRepository.setMorningCheckInTimeMinutes(minutesOfDay)
        }
    }
}
