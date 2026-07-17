package com.episode6.headachetracker.data

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.episode6.headachetracker.MainActivity
import com.episode6.headachetracker.R

class SecondPillReminderWorker(
    context: Context,
    workerParams: WorkerParameters,
) : Worker(context, workerParams) {

    // guarded by areNotificationsEnabled(); notify() never fires without permission
    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        val context = applicationContext
        ensureNotificationChannel(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return Result.success()
        }

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
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "second_pill_reminder"
        const val NOTIFICATION_ID = 1001

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
