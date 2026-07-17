package com.episode6.headachetracker.data

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

interface SecondPillReminderScheduler {
    suspend fun schedule(fireAtMillis: Long)
    suspend fun cancel()
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AlarmManagerSecondPillReminderScheduler(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) : SecondPillReminderScheduler {

    override suspend fun schedule(fireAtMillis: Long) {
        settingsRepository.setPendingSecondPillReminderAt(fireAtMillis)
        context.setExactAlarmCompat(fireAtMillis, SecondPillReminderReceiver.pendingIntent(context))
    }

    override suspend fun cancel() {
        settingsRepository.setPendingSecondPillReminderAt(null)
        context.cancelAlarm(SecondPillReminderReceiver.pendingIntent(context))
    }
}
