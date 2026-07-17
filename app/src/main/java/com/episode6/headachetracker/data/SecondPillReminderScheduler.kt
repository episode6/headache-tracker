package com.episode6.headachetracker.data

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.concurrent.TimeUnit

interface SecondPillReminderScheduler {
    fun schedule(fireAtMillis: Long)
    fun cancel()
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class WorkManagerSecondPillReminderScheduler(
    private val context: Context,
) : SecondPillReminderScheduler {

    override fun schedule(fireAtMillis: Long) {
        val delayMillis = (fireAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SecondPillReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build(),
        )
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "second_pill_reminder"
    }
}
