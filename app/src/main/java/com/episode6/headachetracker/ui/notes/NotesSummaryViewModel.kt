package com.episode6.headachetracker.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.episode6.headachetracker.data.HeadacheDao
import com.episode6.headachetracker.model.HeadacheEntry
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// entries are newest-first within a group; groups are newest-year-first
data class NotesYearGroup(
    val year: Int,
    val entries: List<HeadacheEntry>,
)

data class NotesSummaryState(
    val yearGroups: List<NotesYearGroup> = emptyList(),
    val isLoading: Boolean = true,
)

@Inject
class NotesSummaryViewModel(
    dao: HeadacheDao,
) : ViewModel() {

    val state: StateFlow<NotesSummaryState> = dao.getAllEntries()
        .map { entries ->
            NotesSummaryState(
                yearGroups = entries
                    .filter { !it.notes.isNullOrBlank() }
                    .sortedByDescending { it.date }
                    .groupBy { it.date.substringBefore('-').toInt() }
                    .map { (year, yearEntries) -> NotesYearGroup(year, yearEntries) }
                    .sortedByDescending { it.year },
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotesSummaryState(),
        )
}
