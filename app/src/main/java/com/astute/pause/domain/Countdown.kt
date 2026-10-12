package com.astute.pause.domain

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Countdown {
    fun describe(now: Instant, until: Instant): String {
        val remaining = Duration.between(now, until)
        if (remaining.isNegative || remaining.isZero) return "Ready to decide"
        val days = remaining.toDays()
        val hours = remaining.minusDays(days).toHours()
        val minutes = remaining.toMinutes() % 60
        return when {
            days > 0 -> "${days}d ${hours}h left"
            hours > 0 -> "${hours}h ${minutes}m left"
            else -> "${maxOf(minutes, 1)}m left"
        }
    }

    fun decideOn(at: Instant, zone: ZoneId, locale: Locale = Locale.getDefault()): String =
        DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a", locale).format(at.atZone(zone))

    fun waitLength(hours: Int): String = when {
        hours <= 0 -> "No wait"
        hours % 24 == 0 -> plural(hours / 24, "day")
        else -> plural(hours, "hour")
    }

    private fun plural(n: Int, unit: String) = if (n == 1) "1 $unit" else "$n ${unit}s"
}
