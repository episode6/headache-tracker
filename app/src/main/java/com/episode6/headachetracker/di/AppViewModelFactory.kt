package com.episode6.headachetracker.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.episode6.headachetracker.ui.calendar.CalendarViewModel
import com.episode6.headachetracker.ui.calendar.FullYearViewModel
import com.episode6.headachetracker.ui.edit.EditViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AppViewModelFactory(
    private val calendarViewModelProvider: ()->CalendarViewModel,
    private val editViewModelFactory: ()->EditViewModel.Factory,
    private val fullYearViewModelFactory: ()->FullYearViewModel.Factory,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(CalendarViewModel::class.java) ->
                calendarViewModelProvider() as T
            modelClass.isAssignableFrom(EditViewModel::class.java) -> {
                val date = extras[EditDateKey]
                    ?: throw IllegalArgumentException("EditViewModel requires a date in CreationExtras")
                editViewModelFactory().create(date) as T
            }
            modelClass.isAssignableFrom(FullYearViewModel::class.java) -> {
                val year = extras[FullYearYearKey]
                    ?: throw IllegalArgumentException("FullYearViewModel requires a year in CreationExtras")
                fullYearViewModelFactory().create(year) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        val EditDateKey = object : CreationExtras.Key<String> {}
        val FullYearYearKey = object : CreationExtras.Key<Int> {}
    }
}
