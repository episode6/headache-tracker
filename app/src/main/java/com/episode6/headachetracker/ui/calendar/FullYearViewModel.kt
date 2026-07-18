package com.episode6.headachetracker.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.episode6.headachetracker.data.HeadacheDao
import com.episode6.headachetracker.model.HeadacheEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

import java.time.LocalDate

data class FullYearState(
    val selectedYear: Int,
    val entries: Map<String, HeadacheEntry> = emptyMap(),
)

class FullYearViewModel @AssistedInject constructor(
    @Assisted private val year: Int,
    private val dao: HeadacheDao,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(year: Int): FullYearViewModel
    }

    private val _selectedYear = MutableStateFlow(year)

    val state: StateFlow<FullYearState> = combine(
        _selectedYear,
        dao.getAllEntries(),
    ) { selectedYear, entries ->
        FullYearState(
            selectedYear = selectedYear,
            entries = entries.associateBy { it.date },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FullYearState(selectedYear = year),
    )

    fun onYearChanged(year: Int) {
        if (year <= LocalDate.now().year) {
            _selectedYear.value = year
        }
    }
}
