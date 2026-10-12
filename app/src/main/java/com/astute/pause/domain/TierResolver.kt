package com.astute.pause.domain

import com.astute.pause.data.local.entity.Question
import com.astute.pause.data.local.entity.Tier
import com.astute.pause.data.local.entity.TierMode
import java.time.Duration
import java.time.Instant
import java.util.Locale

object TierResolver {
    private val order = compareBy<Tier>({ it.minCents }, { it.id })

    /** The tier with the highest minimum that the price reaches. Null if no tier starts low enough. */
    fun resolve(tiers: List<Tier>, cents: Long): Tier? =
        tiers.sortedWith(order).lastOrNull { it.minCents <= cents }

    fun waitUntil(firstConsideredAt: Instant, tier: Tier?): Instant? =
        tier?.waitHours?.takeIf { it > 0 }?.let { firstConsideredAt.plus(Duration.ofHours(it.toLong())) }

    fun initialQuestions(tier: Tier?, all: List<Question>): List<Question> {
        val active = all.filter { it.active }.sortedBy { it.sortOrder }
        return if (tier == null || tier.mode == TierMode.FULL) active
        else active.filter { it.id in tier.questionIds }
    }

    fun recheckQuestions(all: List<Question>): List<Question> =
        all.filter { it.active && it.inRecheck }.sortedBy { it.sortOrder }

    fun describeRange(tiers: List<Tier>, tier: Tier, currency: String, locale: Locale = Locale.getDefault()): String {
        val sorted = tiers.sortedWith(order)
        val next = sorted.getOrNull(sorted.indexOfFirst { it.id == tier.id } + 1)
        val fmt = { cents: Long -> Money.format(cents, currency, locale) }
        return when {
            next == null -> "${fmt(tier.minCents)} and up"
            tier.minCents == 0L -> "Under ${fmt(next.minCents)}"
            else -> "${fmt(tier.minCents)} – ${fmt(next.minCents - 1)}"
        }
    }
}
