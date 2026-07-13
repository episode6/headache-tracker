package com.episode6.headachetracker.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.episode6.headachetracker.data.HeadacheDao
import com.episode6.headachetracker.model.HeadacheEntry
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditState(
    val date: String,
    val intensity: Int = 0,
    val pillsTaken: Int = 0,
    val firstPillTime: Long? = null,
    val secondPillTime: Long? = null,
    val isSaving: Boolean = false
)

class EditViewModel @AssistedInject constructor(
    @Assisted private val date: String,
    private val dao: HeadacheDao
) : ViewModel() {

    internal var clock: () -> Long = System::currentTimeMillis

    @AssistedFactory
    fun interface Factory {
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
                    secondPillTime = entry.secondPillTime
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

    fun saveEntry(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            dao.upsertEntry(
                HeadacheEntry(
                    date = date,
                    intensity = _state.value.intensity,
                    pillsTaken = _state.value.pillsTaken,
                    firstPillTime = _state.value.firstPillTime.takeIf { _state.value.pillsTaken >= 1 },
                    secondPillTime = _state.value.secondPillTime.takeIf { _state.value.pillsTaken >= 2 }
                )
            )
            _state.value = _state.value.copy(isSaving = false)
            onComplete()
        }
    }
}
