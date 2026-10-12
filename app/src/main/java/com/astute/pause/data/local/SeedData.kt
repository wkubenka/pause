package com.astute.pause.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.astute.pause.data.local.entity.AnswerType
import com.astute.pause.data.local.entity.AnswerType.CHOICE
import com.astute.pause.data.local.entity.AnswerType.TEXT
import com.astute.pause.data.local.entity.AnswerType.YES_NO
import com.astute.pause.data.local.entity.Question
import com.astute.pause.data.local.entity.QuestionGroup
import com.astute.pause.data.local.entity.QuestionGroup.ALTERNATIVES_TIMING
import com.astute.pause.data.local.entity.QuestionGroup.COSTS_TRADEOFF
import com.astute.pause.data.local.entity.QuestionGroup.NEED_CLARITY
import com.astute.pause.data.local.entity.QuestionGroup.USEFULNESS_QUALITY
import com.astute.pause.data.local.entity.Tier
import com.astute.pause.data.local.entity.TierMode

/**
 * Default questions: "The Smart Spending Checklist" by Sunny Kind Journey, used for personal use only.
 * If this app is ever shared or published, replace the text with original questions or get permission.
 */
object SeedData {
    const val YES = "yes"
    const val NO = "no"

    val questions: List<Question> = listOf(
        q(1, NEED_CLARITY, "Do I actually need this, or do I just want the feeling of getting something new?",
            CHOICE, options = listOf("I need it", "I want the feeling"), warning = listOf("I want the feeling")),
        q(2, NEED_CLARITY, "What problem will this solve in my real life, specifically?", TEXT),
        q(3, NEED_CLARITY, "Am I buying for my actual life, or for an ideal version of my life?",
            CHOICE, options = listOf("My actual life", "An ideal version"), warning = listOf("An ideal version")),
        q(4, NEED_CLARITY, "Am I calm enough to make this decision clearly?", YES_NO,
            warning = listOf(NO), recheck = true),
        q(5, ALTERNATIVES_TIMING, "Do I already own something that does this well enough?", YES_NO,
            warning = listOf(YES)),
        q(6, ALTERNATIVES_TIMING, "Could I borrow it, buy it second-hand, or get a simpler version?", CHOICE,
            options = listOf("Borrow it", "Buy second-hand", "Get a simpler version", "None of these"),
            warning = listOf("Borrow it", "Buy second-hand", "Get a simpler version")),
        q(7, ALTERNATIVES_TIMING, "Is this the right time to buy this?", YES_NO, warning = listOf(NO)),
        q(8, ALTERNATIVES_TIMING, "Would I still want this at full price, with no sale or hype around it?", YES_NO,
            warning = listOf(NO), recheck = true),
        q(9, USEFULNESS_QUALITY, "Will this make my life simpler, or just add one more thing to manage?", CHOICE,
            options = listOf("Simpler", "One more thing to manage"), warning = listOf("One more thing to manage")),
        q(10, USEFULNESS_QUALITY,
            "Will buying the better version help me buy this once instead of replacing it again later?", YES_NO),
        q(11, USEFULNESS_QUALITY, "Will I use this enough for higher quality to really matter?", YES_NO,
            warning = listOf(NO)),
        q(12, COSTS_TRADEOFF, "Did I research enough to know if this is good value for money?", YES_NO,
            warning = listOf(NO)),
        q(13, COSTS_TRADEOFF, "What will this really cost once everything is included?", TEXT),
        q(14, COSTS_TRADEOFF, "Can I afford this comfortably, not just technically?", YES_NO, warning = listOf(NO)),
        q(15, COSTS_TRADEOFF, "What am I saying no to if I buy this?", TEXT, recheck = true),
    )

    /** Spec §3 defaults. "over $100" means $100.01 and up, so $100.00 is Medium. */
    val tiers: List<Tier> = listOf(
        Tier(id = 1, name = "Small", minCents = 0, mode = TierMode.SHORT, waitHours = 0, questionIds = listOf(1, 5, 14)),
        Tier(id = 2, name = "Medium", minCents = 2_500, mode = TierMode.FULL, waitHours = 24),
        Tier(id = 3, name = "Large", minCents = 10_001, mode = TierMode.FULL, waitHours = 72),
        Tier(id = 4, name = "Huge", minCents = 50_001, mode = TierMode.FULL, waitHours = 168),
        Tier(id = 5, name = "Life changing", minCents = 1_000_001, mode = TierMode.FULL, waitHours = 720),
    )

    /** Runs inside Room's onCreate, so it must write synchronously through the raw database (no DAOs). */
    fun insertInto(db: SupportSQLiteDatabase) {
        val converters = Converters()
        questions.forEach { q ->
            db.insert("questions", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                put("id", q.id)
                put("text", q.text)
                put("question_group", q.group.name)
                put("active", if (q.active) 1 else 0)
                put("sortOrder", q.sortOrder)
                put("answerType", q.answerType.name)
                put("options", converters.stringsToJson(q.options))
                put("warningAnswers", converters.stringsToJson(q.warningAnswers))
                put("inRecheck", if (q.inRecheck) 1 else 0)
            })
        }
        tiers.forEach { t ->
            db.insert("tiers", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                put("id", t.id)
                put("name", t.name)
                put("minCents", t.minCents)
                put("mode", t.mode.name)
                put("waitHours", t.waitHours)
                put("questionIds", converters.longsToJson(t.questionIds))
            })
        }
    }

    private fun q(
        id: Long,
        group: QuestionGroup,
        text: String,
        type: AnswerType,
        options: List<String> = emptyList(),
        warning: List<String> = emptyList(),
        recheck: Boolean = false,
    ) = Question(
        id = id, text = text, group = group, sortOrder = id.toInt(), answerType = type,
        options = options, warningAnswers = warning, inRecheck = recheck,
    )
}
