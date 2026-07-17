package com.episode6.headachetracker.data

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

interface MorningCheckInScheduler {
    fun scheduleNext(timeOfDayMinutes: Int)
    fun cancel()
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AlarmManagerMorningCheckInScheduler(
    private val context: Context,
) : MorningCheckInScheduler {

    // an alarm set with the same PendingIntent replaces the previous one, so
    // repeated calls are idempotent without any unique-work bookkeeping
    override fun scheduleNext(timeOfDayMinutes: Int) {
        val triggerAt = nextOccurrenceMillis(
            nowMillis = System.currentTimeMillis(),
            zone = ZoneId.systemDefault(),
            timeOfDayMinutes = timeOfDayMinutes,
        )
        context.setExactAlarmCompat(triggerAt, MorningCheckInReceiver.pendingIntent(context))
    }

    override fun cancel() {
        context.cancelAlarm(MorningCheckInReceiver.pendingIntent(context))
    }
}

// targets the local wall-clock time; if a DST gap swallows it, atZone() resolves
// to the first valid instant after the gap
internal fun nextOccurrenceMillis(nowMillis: Long, zone: ZoneId, timeOfDayMinutes: Int): Long {
    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
    val time = LocalTime.of(timeOfDayMinutes / 60, timeOfDayMinutes % 60)
    val todayTarget = now.toLocalDate().atTime(time).atZone(zone)
    val next = if (todayTarget.toInstant().toEpochMilli() > nowMillis) {
        todayTarget
    } else {
        now.toLocalDate().plusDays(1).atTime(time).atZone(zone)
    }
    return next.toInstant().toEpochMilli()
}
