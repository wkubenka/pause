package com.astute.pause.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.astute.pause.data.local.entity.AnswerPhase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), PauseDatabase::class.java)

    private val dbName = "migration-test.db"

    private fun SupportSQLiteDatabase.question(id: Long, text: String, options: String, warnings: String, type: String = "CHOICE",
        group: String = "NEED_CLARITY") =
        insert("questions", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
            put("id", id); put("text", text); put("question_group", group); put("active", 1)
            put("sortOrder", id.toInt()); put("answerType", type); put("options", options)
            put("warningAnswers", warnings); put("inRecheck", 0)
        })

    /** A v1 database as it exists on a phone running 0.1.x: defaults for Q1 and Q3, a user-edited Q9, one old answer. */
    private fun createV1() {
        helper.createDatabase(dbName, 1).apply {
            question(1, "Do I actually need this, or do I just want the feeling of getting something new?",
                """["I need it","I want the feeling"]""", """["I want the feeling"]""")
            question(2, "What problem will this solve in my real life, specifically?", "[]", "[]", type = "TEXT")
            question(3, "Am I buying for my actual life, or for an ideal version of my life?",
                """["My actual life","An ideal version"]""", """["An ideal version"]""")
            question(9, "Will this make my life simpler, or just add one more thing to manage?",
                """["Simpler","Meh","One more thing to manage"]""", """["One more thing to manage"]""")
            insert("items", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                put("id", 1); put("name", "Desk"); put("notes", ""); put("firstConsideredAt", 0L)
                put("currency", "USD"); put("status", "BOUGHT")
            })
            insert("answers", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                put("itemId", 1); put("questionId", 1)
                put("questionText", "Do I actually need this, or do I just want the feeling of getting something new?")
                put("answerType", "CHOICE"); put("answer", "I want the feeling"); put("note", "")
                put("answeredAt", 0L); put("phase", "INITIAL"); put("flagged", 1)
            })
            close()
        }
    }

    @Test fun migrates_1_to_2_and_schema_matches() {
        createV1()
        helper.runMigrationsAndValidate(dbName, 2, true, Migrations.MIGRATION_1_2).close()
    }

    @Test fun rewrites_untouched_defaults_keeps_edits_and_old_answers() = runBlocking<Unit> {
        createV1()
        helper.runMigrationsAndValidate(dbName, 2, true, Migrations.MIGRATION_1_2).close()

        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext<Context>(), PauseDatabase::class.java, dbName)
            .addMigrations(Migrations.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val seed = SeedData.questions.associateBy { it.id }
            val q = db.questionDao().all().associateBy { it.id }
            for (id in listOf(1L, 3L)) {
                assertEquals(seed.getValue(id).text, q.getValue(id).text)
                assertEquals(seed.getValue(id).options, q.getValue(id).options)
                assertEquals(seed.getValue(id).warningAnswers, q.getValue(id).warningAnswers)
                assertEquals(seed.getValue(id).followUps, q.getValue(id).followUps)
                assertEquals(id.toInt(), q.getValue(id).sortOrder)
            }
            assertEquals(listOf("Simpler", "Meh", "One more thing to manage"), q.getValue(9).options)  // user edit kept
            assertEquals(emptyMap<String, String>(), q.getValue(9).followUps)
            assertEquals(seed.getValue(2).text, q.getValue(2).text)

            val old = db.answerDao().forItem(1, AnswerPhase.INITIAL).single()
            assertEquals("I want the feeling", old.answer)
            assertEquals("Do I actually need this, or do I just want the feeling of getting something new?", old.questionText)
            assertEquals(true, old.flagged)
            assertEquals("", old.followUpQuestion)
            assertFalse(old.followUpFlagged)
        } finally {
            db.close()
        }
    }

    @Test fun v1_defaults_in_migration_match_seed_for_question_nine() = runBlocking<Unit> {
        helper.createDatabase(dbName, 1).apply {
            question(9, "Will this make my life simpler, or just add one more thing to manage?",
                """["Simpler","One more thing to manage"]""", """["One more thing to manage"]""", group = "USEFULNESS_QUALITY")
            close()
        }
        helper.runMigrationsAndValidate(dbName, 2, true, Migrations.MIGRATION_1_2).close()
        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext<Context>(), PauseDatabase::class.java, dbName)
            .addMigrations(Migrations.MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            val seed = SeedData.questions.first { it.id == 9L }
            assertEquals(seed, db.questionDao().get(9)!!)
        } finally {
            db.close()
        }
    }
}
