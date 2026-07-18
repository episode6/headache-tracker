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

data class NotesSummaryState(
    val entries: List<HeadacheEntry> = emptyList(),
    val isLoading: Boolean = true,
)

@Inject
class NotesSummaryViewModel(
    dao: HeadacheDao,
) : ViewModel() {

    val state: StateFlow<NotesSummaryState> = dao.getAllEntries()
        .map { entries ->
            NotesSummaryState(
                entries = entries
                    .filter { !it.notes.isNullOrBlank() }
                    .sortedByDescending { it.date },
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotesSummaryState(),
        )
}
