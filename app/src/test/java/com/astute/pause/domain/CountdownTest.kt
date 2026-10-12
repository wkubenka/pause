package com.astute.pause.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

class CountdownTest {
    private val now = Instant.parse("2026-01-05T15:00:00Z")

    @Test fun describes_remaining_time() {
        assertEquals("2d 4h left", Countdown.describe(now, now.plus(Duration.ofHours(52))))
        assertEquals("3h 15m left", Countdown.describe(now, now.plus(Duration.ofMinutes(195))))
        assertEquals("1m left", Countdown.describe(now, now.plusSeconds(20)))
        assertEquals("Ready to decide", Countdown.describe(now, now))
        assertEquals("Ready to decide", Countdown.describe(now, now.minusSeconds(60)))
    }

    @Test fun formats_decide_on_date() {
        assertEquals("Mon, Jan 5 at 3:00 PM", Countdown.decideOn(now, ZoneOffset.UTC, Locale.US))
    }

    @Test fun describes_wait_lengths() {
        assertEquals("No wait", Countdown.waitLength(0))
        assertEquals("1 day", Countdown.waitLength(24))
        assertEquals("3 days", Countdown.waitLength(72))
        assertEquals("1 hour", Countdown.waitLength(1))
        assertEquals("36 hours", Countdown.waitLength(36))
    }
}
