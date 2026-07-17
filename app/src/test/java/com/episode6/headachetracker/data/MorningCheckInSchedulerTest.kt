package com.episode6.headachetracker.data

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class MorningCheckInSchedulerTest {

    private val zone = ZoneId.of("America/New_York")
    private val eightAm = 8 * 60

    private fun millisAt(hour: Int, minute: Int, day: Int = 15): Long =
        ZonedDateTime.of(2026, 7, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `before the target time schedules for later today`() {
        val delay = nextOccurrenceDelayMillis(millisAt(6, 30), zone, eightAm)

        assertEquals(90 * 60_000L, delay)
    }

    @Test
    fun `after the target time schedules for tomorrow`() {
        val delay = nextOccurrenceDelayMillis(millisAt(9, 0), zone, eightAm)

        assertEquals(23 * 60 * 60_000L, delay)
    }

    @Test
    fun `exactly at the target time schedules for tomorrow`() {
        val delay = nextOccurrenceDelayMillis(millisAt(8, 0), zone, eightAm)

        assertEquals(24 * 60 * 60_000L, delay)
    }

    @Test
    fun `spring-forward DST day still yields a next-day occurrence`() {
        // 2026-03-08 02:00 EST -> 03:00 EDT; day is only 23h long
        val nowMillis = ZonedDateTime.of(2026, 3, 7, 9, 0, 0, 0, zone).toInstant().toEpochMilli()

        val delay = nextOccurrenceDelayMillis(nowMillis, zone, eightAm)

        assertEquals(22 * 60 * 60_000L, delay)
    }
}
