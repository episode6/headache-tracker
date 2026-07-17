package com.episode6.headachetracker.data

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.episode6.headachetracker.MainActivity
import com.episode6.headachetracker.R
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MorningCheckInReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                handle(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    // guarded by areNotificationsEnabled(); notify() never fires without permission
    @SuppressLint("MissingPermission")
    internal suspend fun handle(context: Context) {
        val settings = SettingsRepository(context.settingsDataStore)
        if (!settings.morningCheckInEnabled.first()) {
            return
        }

        ensureNotificationChannel(context)
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            val yesterday = LocalDate.now().minusDays(1).toString()
            val contentIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_EDIT_DATE, yesterday),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_head_circuit)
                .setContentTitle(context.getString(R.string.morning_check_in_notification_title))
                .setContentText(context.getString(R.string.morning_check_in_notification_text))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        // re-arm for tomorrow so the daily check-in survives without app launches
        AlarmManagerMorningCheckInScheduler(context)
            .scheduleNext(settings.morningCheckInTimeMinutes.first())
    }

    companion object {
        const val CHANNEL_ID = "morning_check_in"
        const val NOTIFICATION_ID = 1002
        private const val REQUEST_CODE = 1

        fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, MorningCheckInReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        fun ensureNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.morning_check_in_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }
}
