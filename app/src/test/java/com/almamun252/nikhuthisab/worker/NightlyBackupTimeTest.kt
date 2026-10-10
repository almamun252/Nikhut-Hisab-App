package com.almamun252.nikhuthisab.worker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class NightlyBackupTimeTest {
    private val zone = ZoneId.of("Asia/Dhaka")
    private val date = LocalDate.of(2026, 10, 11)

    @Test fun nightlyTargetsStayWithinWindowAndSpreadAcrossInstallations() {
        val targets = (1L..500L).map { NightlyBackupTime.targetForDate(date, zone, it) }
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.atStartOfDay(zone).plusHours(2).toInstant()
        assertTrue(targets.all { !it.isBefore(start) && it.isBefore(end) })
        assertTrue(targets.distinct().size > 400)
    }

    @Test fun reopeningAppBeforeTargetKeepsSameTime() {
        val target = NightlyBackupTime.targetForDate(date, zone, 42L)
        assertEquals(target, NightlyBackupTime.nextRun(target.minusSeconds(60), zone, 42L))
        assertEquals(target, NightlyBackupTime.nextRun(target.minusSeconds(1), zone, 42L))
    }

    @Test fun runningAtTargetSchedulesFollowingNight() {
        val target = NightlyBackupTime.targetForDate(date, zone, 42L)
        assertEquals(date.plusDays(1), NightlyBackupTime.nextRun(target, zone, 42L).atZone(zone).toLocalDate())
    }

    @Test fun enablingDuringDaySchedulesNextMidnightWindow() {
        val now = date.atTime(17, 30).atZone(zone).toInstant()
        val target = NightlyBackupTime.nextRun(now, zone, 42L)
        assertEquals(date.plusDays(1), target.atZone(zone).toLocalDate())
        assertTrue(target.isAfter(now))
    }

    @Test fun delayedExecutionStillSchedulesAFutureNight() {
        val now = date.plusDays(3).atTime(4, 0).atZone(zone).toInstant()
        val target = NightlyBackupTime.nextRun(now, zone, 42L)
        assertEquals(date.plusDays(4), target.atZone(zone).toLocalDate())
        assertTrue(target.isAfter(now))
    }

    @Test fun monthAndYearBoundariesUseLocalCalendar() {
        val now = Instant.parse("2026-12-31T15:00:00Z")
        assertEquals(LocalDate.of(2027, 1, 1), NightlyBackupTime.nextRun(now, zone, 42L).atZone(zone).toLocalDate())
    }
}
