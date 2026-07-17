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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SecondPillReminderReceiver : BroadcastReceiver() {

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
        ensureNotificationChannel(context)
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            val contentIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pill)
                .setContentTitle(context.getString(R.string.second_pill_notification_title))
                .setContentText(context.getString(R.string.second_pill_notification_text))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
        SettingsRepository(context.settingsDataStore).setPendingSecondPillReminderAt(null)
    }

    companion object {
        const val CHANNEL_ID = "second_pill_reminder"
        const val NOTIFICATION_ID = 1001

        fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, SecondPillReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        fun ensureNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.second_pill_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            )
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }
}
