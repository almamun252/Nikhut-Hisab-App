package com.almamun252.nikhuthisab.worker

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

// A stable per-installation/per-date offset prevents app reopening from moving the job.
object NightlyBackupTime {
    fun targetForDate(date: LocalDate, zone: ZoneId, seed: Long): Instant {
        val seconds = Random(seed xor date.toEpochDay()).nextInt(2 * 60 * 60)
        return date.atStartOfDay(zone).plusSeconds(seconds.toLong()).toInstant()
    }

    fun nextRun(now: Instant, zone: ZoneId, seed: Long): Instant {
        val today = now.atZone(zone).toLocalDate()
        val target = targetForDate(today, zone, seed)
        return if (target.isAfter(now)) target else targetForDate(today.plusDays(1), zone, seed)
    }
}
