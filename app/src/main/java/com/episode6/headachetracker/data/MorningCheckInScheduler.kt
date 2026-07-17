package com.episode6.headachetracker.data

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

interface MorningCheckInScheduler {
    fun scheduleNext(timeOfDayMinutes: Int)
    fun cancel()
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class WorkManagerMorningCheckInScheduler(
    private val context: Context,
) : MorningCheckInScheduler {

    override fun scheduleNext(timeOfDayMinutes: Int) {
        enqueue(timeOfDayMinutes, ExistingWorkPolicy.REPLACE)
    }

    // for the worker's self-rechain: REPLACE would cancel-and-delete the very work
    // that is running; APPEND_OR_REPLACE chains tomorrow's run after it instead
    internal fun scheduleNextAppended(timeOfDayMinutes: Int) {
        enqueue(timeOfDayMinutes, ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    private fun enqueue(timeOfDayMinutes: Int, policy: ExistingWorkPolicy) {
        val delayMillis = nextOccurrenceDelayMillis(
            nowMillis = System.currentTimeMillis(),
            zone = ZoneId.systemDefault(),
            timeOfDayMinutes = timeOfDayMinutes,
        )
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            policy,
            OneTimeWorkRequestBuilder<MorningCheckInWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build(),
        )
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "morning_check_in"
    }
}

// targets the local wall-clock time; if a DST gap swallows it, atZone() resolves
// to the first valid instant after the gap
internal fun nextOccurrenceDelayMillis(nowMillis: Long, zone: ZoneId, timeOfDayMinutes: Int): Long {
    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
    val time = LocalTime.of(timeOfDayMinutes / 60, timeOfDayMinutes % 60)
    val todayTarget = now.toLocalDate().atTime(time).atZone(zone)
    val next = if (todayTarget.toInstant().toEpochMilli() > nowMillis) {
        todayTarget
    } else {
        now.toLocalDate().plusDays(1).atTime(time).atZone(zone)
    }
    return next.toInstant().toEpochMilli() - nowMillis
}
