package com.episode6.headachetracker.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.episode6.headachetracker.data.HeadacheDao
import com.episode6.headachetracker.data.SecondPillReminderScheduler
import com.episode6.headachetracker.data.SettingsRepository
import com.episode6.headachetracker.model.HeadacheEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class EditState(
    val date: String,
    val intensity: Int = 0,
    val pillsTaken: Int = 0,
    val firstPillTime: Long? = null,
    val secondPillTime: Long? = null,
    val notes: String = "",
    val isSaving: Boolean = false
)

class EditViewModel @AssistedInject constructor(
    @Assisted private val date: String,
    private val dao: HeadacheDao,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: SecondPillReminderScheduler,
) : ViewModel() {

    internal var clock: () -> Long = System::currentTimeMillis
    internal var today: () -> LocalDate = LocalDate::now

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(date: String): EditViewModel
    }

    private val _state = MutableStateFlow(EditState(date = date))
    val state: StateFlow<EditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val entry = dao.getEntryByDate(date)
            if (entry != null) {
                _state.value = _state.value.copy(
                    intensity = entry.intensity,
                    pillsTaken = entry.pillsTaken,
                    firstPillTime = entry.firstPillTime,
                    secondPillTime = entry.secondPillTime,
                    notes = entry.notes.orEmpty()
                )
            }
        }
    }

    fun onIntensityChanged(intensity: Int) {
        _state.value = _state.value.copy(intensity = intensity)
    }

    fun onPillsTakenChanged(pillsTaken: Int) {
        val current = _state.value
        _state.value = current.copy(
            pillsTaken = pillsTaken,
            firstPillTime = when {
                pillsTaken >= 1 && current.firstPillTime == null -> clock()
                else -> current.firstPillTime
            },
            secondPillTime = when {
                pillsTaken >= 2 && current.secondPillTime == null -> clock()
                else -> current.secondPillTime
            }
        )
    }

    fun onFirstPillTimeChanged(time: Long) {
        _state.value = _state.value.copy(firstPillTime = time)
    }

    fun onSecondPillTimeChanged(time: Long) {
        _state.value = _state.value.copy(secondPillTime = time)
    }

    fun onNotesChanged(notes: String) {
        _state.value = _state.value.copy(notes = notes)
    }

    fun saveEntry(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            val entry = HeadacheEntry(
                date = date,
                intensity = _state.value.intensity,
                pillsTaken = _state.value.pillsTaken,
                firstPillTime = _state.value.firstPillTime.takeIf { _state.value.pillsTaken >= 1 },
                secondPillTime = _state.value.secondPillTime.takeIf { _state.value.pillsTaken >= 2 },
                notes = _state.value.notes.trim().takeIf { it.isNotEmpty() }
            )
            dao.upsertEntry(entry)
            updateSecondPillReminder(entry)
            _state.value = _state.value.copy(isSaving = false)
            onComplete()
        }
    }

    // Only today's entry can own the pending 2nd-pill reminder; edits to other days
    // must never schedule or clear it.
    private suspend fun updateSecondPillReminder(entry: HeadacheEntry) {
        if (entry.date != today().toString()) return
        val firstPillTime = entry.firstPillTime
        if (entry.pillsTaken == 1 && firstPillTime != null) {
            val delayMinutes = settingsRepository.secondPillReminderMinutes.first()
            val fireAt = firstPillTime + delayMinutes * 60_000L
            if (fireAt > clock()) {
                reminderScheduler.schedule(fireAt)
            } else {
                reminderScheduler.cancel()
            }
        } else {
            reminderScheduler.cancel()
        }
    }
}
