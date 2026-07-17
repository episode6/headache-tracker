package com.episode6.headachetracker.data

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.episode6.headachetracker.MainActivity
import com.episode6.headachetracker.R
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class MorningCheckInWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    // guarded by areNotificationsEnabled(); notify() never fires without permission
    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val context = applicationContext
        val settings = SettingsRepository(context.settingsDataStore)
        if (!settings.morningCheckInEnabled.first()) {
            return Result.success()
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

        // self-rechain for tomorrow so the daily check-in survives without app launches;
        // the manager's REPLACE on app start resets the chain so it can't grow unboundedly
        WorkManagerMorningCheckInScheduler(context)
            .scheduleNextAppended(settings.morningCheckInTimeMinutes.first())
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "morning_check_in"
        const val NOTIFICATION_ID = 1002
        private const val REQUEST_CODE = 1

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
