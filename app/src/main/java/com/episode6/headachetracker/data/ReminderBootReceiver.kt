package com.episode6.headachetracker.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// AlarmManager alarms don't survive reboots (or app updates), so re-arm both
// reminders from persisted state
class ReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                handle(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    internal suspend fun handle(context: Context) {
        val settings = SettingsRepository(context.settingsDataStore)

        if (settings.morningCheckInEnabled.first()) {
            AlarmManagerMorningCheckInScheduler(context)
                .scheduleNext(settings.morningCheckInTimeMinutes.first())
        }

        val pendingAt = settings.pendingSecondPillReminderAt.first()
        if (pendingAt != null) {
            if (pendingAt > System.currentTimeMillis()) {
                AlarmManagerSecondPillReminderScheduler(context, settings).schedule(pendingAt)
            } else {
                settings.setPendingSecondPillReminderAt(null)
            }
        }
    }
}
