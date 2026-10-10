package com.almamun252.nikhuthisab.worker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DailyReminderTimeTest {
    private val zone = ZoneId.of("Asia/Dhaka")
    private val today = LocalDate.of(2026, 10, 10)

    @Test fun beforeEightSchedulesToday() {
        val now = today.atTime(19, 59).atZone(zone).toInstant()
        assertEquals(today.atTime(20, 0).atZone(zone).toInstant(), DailyReminderTime.nextRun(now, zone))
    }
    @Test fun atEightSchedulesTomorrow() {
        val now = today.atTime(20, 0).atZone(zone).toInstant()
        assertEquals(today.plusDays(1).atTime(20, 0).atZone(zone).toInstant(), DailyReminderTime.nextRun(now, zone))
    }
    @Test fun reopeningBeforeEightDoesNotMoveTarget() {
        val a = today.atTime(10, 0).atZone(zone).toInstant()
        val b = today.atTime(18, 45).atZone(zone).toInstant()
        assertEquals(DailyReminderTime.nextRun(a, zone), DailyReminderTime.nextRun(b, zone))
    }
    @Test fun lateExecutionSchedulesNextEvening() {
        val now = today.plusDays(1).atTime(7, 0).atZone(zone).toInstant()
        assertEquals(today.plusDays(1).atTime(20, 0).atZone(zone).toInstant(), DailyReminderTime.nextRun(now, zone))
    }
    @Test fun calendarHandlesYearBoundary() {
        val last = LocalDate.of(2026, 12, 31)
        val now = last.atTime(23, 50).atZone(zone).toInstant()
        assertEquals(LocalDate.of(2027, 1, 1).atTime(20, 0).atZone(zone).toInstant(), DailyReminderTime.nextRun(now, zone))
    }
}
