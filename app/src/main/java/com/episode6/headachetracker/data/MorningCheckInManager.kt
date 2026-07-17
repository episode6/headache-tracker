package com.episode6.headachetracker.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Inject
@SingleIn(AppScope::class)
class MorningCheckInManager(
    private val settingsRepository: SettingsRepository,
    private val scheduler: MorningCheckInScheduler,
    private val appScope: CoroutineScope,
) {

    fun startObserving() {
        appScope.launch {
            combine(
                settingsRepository.morningCheckInEnabled,
                settingsRepository.morningCheckInTimeMinutes,
                ::Pair,
            ).collectLatest { (enabled, timeMinutes) ->
                if (enabled) {
                    scheduler.scheduleNext(timeMinutes)
                } else {
                    scheduler.cancel()
                }
            }
        }
    }
}
