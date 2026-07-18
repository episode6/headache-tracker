package com.episode6.headachetracker.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.episode6.headachetracker.data.HeadacheDao
import com.episode6.headachetracker.model.HeadacheEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
)

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class CalendarViewModel(
    private val dao: HeadacheDao,
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())

    val state: StateFlow<CalendarState> = combine(
        _selectedMonth,
        dao.getAllEntries(),
    ) { month, entries ->
        CalendarState(
            selectedMonth = month,
            entries = entries.associateBy { it.date },
            years = visibleYearsFor(month.year),
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
}
