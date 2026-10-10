package com.almamun252.nikhuthisab.worker

import java.time.Instant
import java.time.ZoneId

// Use local calendar dates rather than a rolling 24-hour repeat interval.
object DailyReminderTime {
    fun nextRun(now: Instant, zone: ZoneId): Instant {
        val today = now.atZone(zone).toLocalDate()
        val target = today.atTime(20, 0).atZone(zone).toInstant()
        return if (target.isAfter(now)) target else today.plusDays(1).atTime(20, 0).atZone(zone).toInstant()
    }
}
