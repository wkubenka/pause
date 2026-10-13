package com.astute.pause.data.local

import com.astute.pause.data.local.entity.AnswerType
import com.astute.pause.data.local.entity.QuestionGroup
import com.astute.pause.data.local.entity.TierMode
import com.astute.pause.testutil.TestDb
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeedDataTest {
    private lateinit var db: PauseDatabase

    @Before fun setUp() { db = TestDb.create() }
    @After fun tearDown() { db.close() }

    @Test fun seeds_fifteen_questions_in_four_groups_in_order() = runBlocking {
        val questions = db.questionDao().all()
        assertEquals(15, questions.size)
        assertEquals(
            mapOf(
                QuestionGroup.NEED_CLARITY to 4,
                QuestionGroup.ALTERNATIVES_TIMING to 4,
                QuestionGroup.USEFULNESS_QUALITY to 3,
                QuestionGroup.COSTS_TRADEOFF to 4,
            ),
            questions.groupingBy { it.group }.eachCount(),
        )
        assertEquals((1..15).toList(), questions.map { it.sortOrder })
        assertTrue(questions.all { it.active })
    }

    @Test fun small_tier_uses_need_own_and_afford_questions() = runBlocking {
        val small = db.tierDao().all().first()
        assertEquals("Small", small.name)
        assertEquals(TierMode.SHORT, small.mode)
        val texts = small.questionIds.map { db.questionDao().get(it)!!.text }
        assertEquals("Why do I want this?", texts[0])
        assertTrue(texts[1].startsWith("Do I already own something"))
        assertTrue(texts[2].startsWith("Can I afford this comfortably"))
    }

    @Test fun recheck_set_is_full_price_calm_and_saying_no() = runBlocking {
        val recheck = db.questionDao().all().filter { it.inRecheck }.map { it.id }
        assertEquals(listOf(4L, 8L, 15L), recheck)
    }

    @Test fun default_tiers_match_spec() = runBlocking {
        val tiers = db.tierDao().all()
        assertEquals(listOf("Small", "Medium", "Large", "Huge", "Life changing"), tiers.map { it.name })
        assertEquals(listOf(0L, 2_500L, 10_001L, 50_001L, 1_000_001L), tiers.map { it.minCents })
        assertEquals(listOf(0, 24, 72, 168, 720), tiers.map { it.waitHours })
    }

    @Test fun choice_questions_have_options_and_warnings_are_valid_answers() = runBlocking {
        db.questionDao().all().forEach { q ->
            val valid = when (q.answerType) {
                AnswerType.YES_NO -> listOf(SeedData.YES, SeedData.NO)
                AnswerType.CHOICE -> q.options
                AnswerType.TEXT -> emptyList()
            }
            if (q.answerType == AnswerType.CHOICE) assertTrue(q.options.size >= 2)
            assertTrue("${q.id} warnings ${q.warningAnswers}", valid.containsAll(q.warningAnswers))
        }
    }

    @Test fun reworked_choice_questions_have_three_options_one_warning_and_follow_ups() = runBlocking<Unit> {
        val q = db.questionDao().all().associateBy { it.id }
        assertEquals("Why do I want this?", q.getValue(1).text)
        assertEquals(listOf(SeedData.WANT_NEED, SeedData.WANT_UPGRADE, SeedData.WANT_FEELING), q.getValue(1).options)
        assertEquals(listOf(SeedData.WANT_FEELING), q.getValue(1).warningAnswers)
        assertEquals(mapOf(SeedData.WANT_UPGRADE to "What specifically gets better, and how often will I notice?"), q.getValue(1).followUps)

        assertEquals("Am I buying for my actual life, or for an ideal version of my life?", q.getValue(3).text)
        assertEquals(listOf(SeedData.LIFE_NOW, SeedData.LIFE_STARTED, SeedData.LIFE_IDEAL), q.getValue(3).options)
        assertEquals(listOf(SeedData.LIFE_IDEAL), q.getValue(3).warningAnswers)
        assertEquals(mapOf(SeedData.LIFE_STARTED to "What have I already done toward it?"), q.getValue(3).followUps)

        assertEquals("Will this make my life simpler, or just add one more thing to manage?", q.getValue(9).text)
        assertEquals(listOf(SeedData.SIMPLER, SeedData.REPLACES, SeedData.ONE_MORE_THING), q.getValue(9).options)
        assertEquals(listOf(SeedData.ONE_MORE_THING), q.getValue(9).warningAnswers)
        assertEquals(emptyMap<String, String>(), q.getValue(9).followUps)

        listOf(1L, 3L, 9L).forEach { assertEquals(AnswerType.CHOICE, q.getValue(it).answerType) }
        assertEquals(listOf(1L, 5L, 14L), db.tierDao().all().first().questionIds)
        q.values.filter { it.id !in setOf(1L, 3L) }.forEach { assertEquals(emptyMap<String, String>(), it.followUps) }
    }
}
