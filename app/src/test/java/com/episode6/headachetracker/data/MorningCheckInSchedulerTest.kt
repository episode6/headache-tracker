package com.episode6.headachetracker.data

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class MorningCheckInSchedulerTest {

    private val zone = ZoneId.of("America/New_York")
    private val eightAm = 8 * 60

    private fun millisAt(hour: Int, minute: Int, day: Int = 15, month: Int = 7): Long =
        ZonedDateTime.of(2026, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `before the target time schedules for later today`() {
        val triggerAt = nextOccurrenceMillis(millisAt(6, 30), zone, eightAm)

        assertEquals(millisAt(8, 0), triggerAt)
    }

    @Test
    fun `after the target time schedules for tomorrow`() {
        val triggerAt = nextOccurrenceMillis(millisAt(9, 0), zone, eightAm)

        assertEquals(millisAt(8, 0, day = 16), triggerAt)
    }

    @Test
    fun `exactly at the target time schedules for tomorrow`() {
        val triggerAt = nextOccurrenceMillis(millisAt(8, 0), zone, eightAm)

        assertEquals(millisAt(8, 0, day = 16), triggerAt)
    }

    @Test
    fun `spring-forward DST day still targets 8am local`() {
        // 2026-03-08 02:00 EST -> 03:00 EDT; day is only 23h long
        val nowMillis = millisAt(9, 0, day = 7, month = 3)

        val triggerAt = nextOccurrenceMillis(nowMillis, zone, eightAm)

        assertEquals(millisAt(8, 0, day = 8, month = 3), triggerAt)
        assertEquals(22 * 60 * 60_000L, triggerAt - nowMillis)
    }
}
