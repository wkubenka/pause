package com.astute.pause.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Migrations hold frozen literals on purpose: they must keep doing exactly this even after SeedData changes. */
object Migrations {
    /**
     * Adds per-option follow-ups and turns default questions 1, 3 and 9 into three-way choices.
     * A seeded question is only rewritten if it still matches its v1 default exactly, so the user's own
     * edits survive. Saved answers keep their snapshot of the old text and choice.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE questions ADD COLUMN followUps TEXT NOT NULL DEFAULT '{}'")
            db.execSQL("ALTER TABLE answers ADD COLUMN followUpQuestion TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE answers ADD COLUMN followUp TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE answers ADD COLUMN followUpFlagged INTEGER NOT NULL DEFAULT 0")

            val need = "Something I need (what I have is broken, missing, or unsafe)"
            val upgrade = "An upgrade to something I use a lot (it would make a real, regular difference)"
            val feeling = "Mostly the feeling of getting something new"
            db.rewriteDefault(
                id = 1,
                oldText = "Do I actually need this, or do I just want the feeling of getting something new?",
                oldOptions = listOf("I need it", "I want the feeling"),
                oldWarnings = listOf("I want the feeling"),
                newText = "Why do I want this?",
                newOptions = listOf(need, upgrade, feeling),
                newWarnings = listOf(feeling),
                followUps = mapOf(upgrade to "What specifically gets better, and how often will I notice?"),
            )

            val now = "For how I actually live now"
            val started = "For something I've already started or scheduled"
            val ideal = "For a version of me I haven't started being yet"
            val actualLife = "Am I buying for my actual life, or for an ideal version of my life?"
            db.rewriteDefault(
                id = 3,
                oldText = actualLife,
                oldOptions = listOf("My actual life", "An ideal version"),
                oldWarnings = listOf("An ideal version"),
                newText = actualLife,
                newOptions = listOf(now, started, ideal),
                newWarnings = listOf(ideal),
                followUps = mapOf(started to "What have I already done toward it?"),
            )

            val oneMore = "Adds one more thing to manage"
            val simpler = "Will this make my life simpler, or just add one more thing to manage?"
            db.rewriteDefault(
                id = 9,
                oldText = simpler,
                oldOptions = listOf("Simpler", "One more thing to manage"),
                oldWarnings = listOf("One more thing to manage"),
                newText = simpler,
                newOptions = listOf("Simplifies things (fewer steps, less upkeep)", "Replaces something I already have", oneMore),
                newWarnings = listOf(oneMore),
                followUps = emptyMap(),
            )
        }
    }

    private fun SupportSQLiteDatabase.rewriteDefault(
        id: Long,
        oldText: String,
        oldOptions: List<String>,
        oldWarnings: List<String>,
        newText: String,
        newOptions: List<String>,
        newWarnings: List<String>,
        followUps: Map<String, String>,
    ) {
        val c = Converters()
        val values = ContentValues().apply {
            put("text", newText)
            put("options", c.stringsToJson(newOptions))
            put("warningAnswers", c.stringsToJson(newWarnings))
            put("followUps", c.stringMapToJson(followUps))
        }
        update(
            "questions", SQLiteDatabase.CONFLICT_ABORT, values,
            "id = ? AND answerType = 'CHOICE' AND text = ? AND options = ? AND warningAnswers = ?",
            arrayOf<Any?>(id, oldText, c.stringsToJson(oldOptions), c.stringsToJson(oldWarnings)),
        )
    }
}
