package com.episode6.headachetracker.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build

// falls back to an inexact alarm if the user revokes the Alarms & Reminders
// special access (only possible on API 31-32; USE_EXACT_ALARM covers 33+)
internal fun Context.setExactAlarmCompat(triggerAtMillis: Long, operation: PendingIntent) {
    val alarmManager = getSystemService(AlarmManager::class.java)
    val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        alarmManager.canScheduleExactAlarms()
    if (canExact) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
    } else {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
    }
}

internal fun Context.cancelAlarm(operation: PendingIntent) {
    getSystemService(AlarmManager::class.java).cancel(operation)
}
