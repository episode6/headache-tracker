package com.episode6.headachetracker.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.episode6.headachetracker.data.AutoExportManager
import com.episode6.headachetracker.data.BackupResult
import com.episode6.headachetracker.data.HeadacheBackupManager
import com.episode6.headachetracker.data.SettingsRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class SettingsState(
    val reminderMinutesText: String = "",
    val morningCheckInEnabled: Boolean = true,
    val morningCheckInTimeMinutes: Int = SettingsRepository.DEFAULT_MORNING_CHECK_IN_TIME_MINUTES,
    val isLoaded: Boolean = false,
    val isTransferInProgress: Boolean = false,
    val autoExportEnabled: Boolean = false,
    val lastAutoExportStatus: String? = null,
)

sealed interface DataTransferMessage {
    data class ExportSuccess(val entryCount: Int) : DataTransferMessage
    data class ImportSuccess(val entryCount: Int) : DataTransferMessage
    data class Error(val message: String) : DataTransferMessage
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupManager: HeadacheBackupManager,
    private val autoExportManager: AutoExportManager,
) : ViewModel() {

    private val _reminderSettings = MutableStateFlow(SettingsState())
    private val _isTransferInProgress = MutableStateFlow(false)

    private val _dataTransferMessages = MutableSharedFlow<DataTransferMessage>()
    val dataTransferMessages: SharedFlow<DataTransferMessage> = _dataTransferMessages.asSharedFlow()

    private val _triggerAutoExportFilePicker = MutableSharedFlow<Unit>()
    val triggerAutoExportFilePicker: SharedFlow<Unit> = _triggerAutoExportFilePicker.asSharedFlow()

    val state: StateFlow<SettingsState> = combine(
        _reminderSettings,
        _isTransferInProgress,
        settingsRepository.autoExportUri,
        settingsRepository.lastAutoExportTimestamp,
    ) { reminderSettings, isTransferInProgress, autoExportUri, lastAutoExportTimestamp ->
        reminderSettings.copy(
            isTransferInProgress = isTransferInProgress,
            autoExportEnabled = autoExportUri != null,
            lastAutoExportStatus = lastAutoExportTimestamp?.let(::formatTimestamp),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState(),
    )

    init {
        viewModelScope.launch {
            _reminderSettings.value = SettingsState(
                reminderMinutesText = settingsRepository.secondPillReminderMinutes.first().toString(),
                morningCheckInEnabled = settingsRepository.morningCheckInEnabled.first(),
                morningCheckInTimeMinutes = settingsRepository.morningCheckInTimeMinutes.first(),
                isLoaded = true,
            )
        }
    }

    fun onReminderMinutesChanged(text: String) {
        val digits = text.filter { it.isDigit() }.take(4)
        _reminderSettings.update { it.copy(reminderMinutesText = digits) }
        val minutes = digits.toIntOrNull() ?: return
        viewModelScope.launch {
            settingsRepository.setSecondPillReminderMinutes(minutes)
        }
    }

    fun onMorningCheckInToggled(enabled: Boolean) {
        _reminderSettings.update { it.copy(morningCheckInEnabled = enabled) }
        viewModelScope.launch {
            settingsRepository.setMorningCheckInEnabled(enabled)
        }
    }

    fun onMorningCheckInTimeChanged(hour: Int, minute: Int) {
        val minutesOfDay = hour * 60 + minute
        _reminderSettings.update { it.copy(morningCheckInTimeMinutes = minutesOfDay) }
        viewModelScope.launch {
            settingsRepository.setMorningCheckInTimeMinutes(minutesOfDay)
        }
    }

    fun exportTo(uri: Uri) {
        if (_isTransferInProgress.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isTransferInProgress.update { true }
            when (val result = backupManager.exportTo(uri)) {
                is BackupResult.Success -> _dataTransferMessages.emit(
                    DataTransferMessage.ExportSuccess(result.entryCount)
                )
                is BackupResult.Error -> _dataTransferMessages.emit(
                    DataTransferMessage.Error(result.message)
                )
            }
            _isTransferInProgress.update { false }
        }
    }

    fun importFrom(uri: Uri) {
        if (_isTransferInProgress.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isTransferInProgress.update { true }
            when (val result = backupManager.importFrom(uri)) {
                is BackupResult.Success -> _dataTransferMessages.emit(
                    DataTransferMessage.ImportSuccess(result.entryCount)
                )
                is BackupResult.Error -> _dataTransferMessages.emit(
                    DataTransferMessage.Error(result.message)
                )
            }
            _isTransferInProgress.update { false }
        }
    }

    fun onAutoExportToggled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (enabled) {
                _triggerAutoExportFilePicker.emit(Unit)
            } else {
                settingsRepository.clearAutoExportSettings()
            }
        }
    }

    fun onAutoExportFileSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            autoExportManager.takePersistablePermission(uri)
            settingsRepository.setAutoExportUri(uri.toString())
            // Initial export
            exportTo(uri)
        }
    }

    private fun formatTimestamp(timestamp: Long): String =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(timestamp))
}
