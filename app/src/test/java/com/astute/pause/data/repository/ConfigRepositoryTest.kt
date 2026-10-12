package com.astute.pause.data.repository

import com.astute.pause.data.local.PauseDatabase
import com.astute.pause.data.local.entity.AnswerType
import com.astute.pause.data.local.entity.Question
import com.astute.pause.data.local.entity.QuestionGroup
import com.astute.pause.data.local.entity.Tier
import com.astute.pause.data.local.entity.TierMode
import com.astute.pause.testutil.TestDb
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigRepositoryTest {
    private lateinit var db: PauseDatabase
    private lateinit var config: ConfigRepository

    @Before fun setUp() { db = TestDb.create(); config = ConfigRepository(db) }
    @After fun tearDown() { db.close() }

    @Test fun new_question_goes_to_the_end() = runBlocking {
        val id = config.saveQuestion(
            Question(text = "Will I still love it in a year?", group = QuestionGroup.NEED_CLARITY, sortOrder = 0, answerType = AnswerType.YES_NO)
        )
        val all = config.questions()
        assertEquals(id, all.last().id)
        assertEquals(16, all.last().sortOrder)
    }

    @Test fun save_question_sanitizes_options_and_warnings() = runBlocking {
        val id = config.saveQuestion(
            Question(
                text = "  Which store?  ", group = QuestionGroup.ALTERNATIVES_TIMING, sortOrder = 0,
                answerType = AnswerType.CHOICE, options = listOf(" A ", "", "B", "A"), warningAnswers = listOf("B", "Z"),
            )
        )
        val q = config.question(id)!!
        assertEquals("Which store?", q.text)
        assertEquals(listOf("A", "B"), q.options)
        assertEquals(listOf("B"), q.warningAnswers)
    }

    @Test fun non_choice_question_drops_options_and_text_drops_warnings() = runBlocking {
        val yn = config.question(config.saveQuestion(
            Question(text = "Q", group = QuestionGroup.NEED_CLARITY, sortOrder = 0, answerType = AnswerType.YES_NO,
                options = listOf("x"), warningAnswers = listOf("no", "maybe"))
        ))!!
        assertEquals(emptyList<String>(), yn.options)
        assertEquals(listOf("no"), yn.warningAnswers)
        val text = config.question(config.saveQuestion(yn.copy(id = 0, answerType = AnswerType.TEXT)))!!
        assertEquals(emptyList<String>(), text.warningAnswers)
    }

    @Test fun move_swaps_with_neighbour_and_ignores_edges() = runBlocking {
        config.move(2, up = true)
        assertEquals(listOf(2L, 1L, 3L), config.questions().take(3).map { it.id })
        config.move(2, up = true)   // already first
        assertEquals(listOf(2L, 1L, 3L), config.questions().take(3).map { it.id })
        config.move(15, up = false) // already last
        assertEquals(15L, config.questions().last().id)
    }

    @Test fun archive_question() = runBlocking {
        config.setActive(3, false)
        assertFalse(config.question(3)!!.active)
    }

    @Test fun save_and_delete_tier() = runBlocking {
        val id = config.saveTier(Tier(name = "Tiny", minCents = 0, mode = TierMode.SHORT, waitHours = 0, questionIds = listOf(1)))
        assertEquals("Tiny", config.tier(id)!!.name)
        config.saveTier(config.tier(id)!!.copy(name = "Teeny"))
        assertEquals("Teeny", config.tier(id)!!.name)
        config.deleteTier(config.tier(id)!!)
        assertEquals(5, config.tiers().size)
    }
}
