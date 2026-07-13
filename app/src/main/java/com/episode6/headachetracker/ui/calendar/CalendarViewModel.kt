package com.episode6.headachetracker.ui.calendar

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.episode6.headachetracker.data.AutoExportManager
import com.episode6.headachetracker.data.BackupResult
import com.episode6.headachetracker.data.HeadacheBackupManager
import com.episode6.headachetracker.data.HeadacheDao
import com.episode6.headachetracker.data.SettingsRepository
import com.episode6.headachetracker.model.HeadacheEntry
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.time.YearMonth

private const val YEAR_DROPDOWN_RANGE_EACH_DIRECTION = 5
private const val CALENDAR_YEAR_LIMIT = 50

internal fun visibleYearsFor(selectedYear: Int): List<Int> {
    val now = YearMonth.now()
    val minYear = now.year - CALENDAR_YEAR_LIMIT
    val maxYear = now.plusMonths(1).year
    val windowSize = YEAR_DROPDOWN_RANGE_EACH_DIRECTION * 2

    var start = selectedYear - YEAR_DROPDOWN_RANGE_EACH_DIRECTION
    var end = start + windowSize - 1

    if (start < minYear) {
        start = minYear
        end = (start + windowSize - 1).coerceAtMost(maxYear)
    }
    if (end > maxYear) {
        end = maxYear
        start = (end - windowSize + 1).coerceAtLeast(minYear)
    }

    return (start..end).toList()
}

data class CalendarState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val entries: Map<String, HeadacheEntry> = emptyMap(),
    val years: List<Int> = visibleYearsFor(YearMonth.now().year),
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
class CalendarViewModel(
    private val dao: HeadacheDao,
    private val backupManager: HeadacheBackupManager,
    private val settingsRepository: SettingsRepository,
    private val autoExportManager: AutoExportManager,
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val _isTransferInProgress = MutableStateFlow(false)
    private val _dataTransferMessages = MutableSharedFlow<DataTransferMessage>()
    val dataTransferMessages: SharedFlow<DataTransferMessage> = _dataTransferMessages.asSharedFlow()

    private val _triggerAutoExportFilePicker = MutableSharedFlow<Unit>()
    val triggerAutoExportFilePicker: SharedFlow<Unit> = _triggerAutoExportFilePicker.asSharedFlow()

    val state: StateFlow<CalendarState> = combine(
        _selectedMonth,
        _isTransferInProgress,
        dao.getAllEntries(),
        settingsRepository.autoExportUri,
        settingsRepository.lastAutoExportTimestamp,
    ) { month, isTransferInProgress, entries, autoExportUri, lastAutoExportTimestamp ->
        val lastExportStr = if (lastAutoExportTimestamp != null) {
            java.time.format.DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT)
                .withZone(java.time.ZoneId.systemDefault())
                .format(java.time.Instant.ofEpochMilli(lastAutoExportTimestamp))
        } else {
            null
        }

        CalendarState(
            selectedMonth = month,
            entries = entries.associateBy { it.date },
            years = visibleYearsFor(month.year),
            isTransferInProgress = isTransferInProgress,
            autoExportEnabled = autoExportUri != null,
            lastAutoExportStatus = lastExportStr,
        )
    }.stateIn(
        scope = viewModelScope + Dispatchers.IO,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarState()
    )

    fun onMonthChanged(month: YearMonth) {
        if (month <= YearMonth.now().plusMonths(1)) {
            _selectedMonth.value = month
        }
    }

    fun onYearSelected(year: Int) {
        val maxAllowed = YearMonth.now().plusMonths(1)
        var newMonth = _selectedMonth.value.withYear(year)
        if (newMonth > maxAllowed) {
            newMonth = maxAllowed
        }
        _selectedMonth.value = newMonth
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
}
