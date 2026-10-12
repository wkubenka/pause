package com.astute.pause.domain

import com.astute.pause.data.local.SeedData
import com.astute.pause.data.local.entity.Tier
import com.astute.pause.data.local.entity.TierMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.util.Locale

class TierResolverTest {
    private val tiers = SeedData.tiers
    private fun nameFor(cents: Long) = TierResolver.resolve(tiers, cents)?.name

    @Test fun boundaries_follow_spec_table() {
        assertEquals("Small", nameFor(0))
        assertEquals("Small", nameFor(2_499))
        assertEquals("Medium", nameFor(2_500))
        assertEquals("Medium", nameFor(10_000))
        assertEquals("Large", nameFor(10_001))
        assertEquals("Large", nameFor(50_000))
        assertEquals("Huge", nameFor(50_001))
        assertEquals("Huge", nameFor(1_000_000))
        assertEquals("Life changing", nameFor(1_000_001))
    }

    @Test fun order_of_input_list_does_not_matter() {
        assertEquals("Medium", TierResolver.resolve(tiers.reversed(), 5_000)?.name)
    }

    @Test fun no_matching_tier_returns_null() {
        assertNull(TierResolver.resolve(emptyList(), 100))
        val startsAtTen = listOf(Tier(id = 9, name = "Ten+", minCents = 1_000, mode = TierMode.FULL, waitHours = 0))
        assertNull(TierResolver.resolve(startsAtTen, 999))
    }

    @Test fun wait_runs_from_first_considered() {
        val first = Instant.parse("2026-01-01T10:00:00Z")
        assertEquals(Instant.parse("2026-01-04T10:00:00Z"), TierResolver.waitUntil(first, tiers[2]))
        assertNull(TierResolver.waitUntil(first, tiers[0]))
        assertNull(TierResolver.waitUntil(first, null))
    }

    @Test fun short_mode_uses_tier_questions_in_sort_order_and_skips_archived() {
        val questions = SeedData.questions.map { if (it.id == 5L) it.copy(active = false) else it }
        assertEquals(listOf(1L, 14L), TierResolver.initialQuestions(tiers[0], questions).map { it.id })
    }

    @Test fun full_mode_or_no_tier_uses_all_active_questions() {
        assertEquals(15, TierResolver.initialQuestions(tiers[1], SeedData.questions).size)
        assertEquals(15, TierResolver.initialQuestions(null, SeedData.questions).size)
    }

    @Test fun recheck_questions() {
        assertEquals(listOf(4L, 8L, 15L), TierResolver.recheckQuestions(SeedData.questions).map { it.id })
    }

    @Test fun describes_ranges() {
        val us = Locale.US
        assertEquals("Under $25.00", TierResolver.describeRange(tiers, tiers[0], "USD", us))
        assertEquals("$25.00 – $100.00", TierResolver.describeRange(tiers, tiers[1], "USD", us))
        assertEquals("$10,000.01 and up", TierResolver.describeRange(tiers, tiers[4], "USD", us))
    }
}
