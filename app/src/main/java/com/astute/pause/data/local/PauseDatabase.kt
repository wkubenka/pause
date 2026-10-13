package com.astute.pause.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.astute.pause.data.local.dao.AnswerDao
import com.astute.pause.data.local.dao.ItemDao
import com.astute.pause.data.local.dao.QuestionDao
import com.astute.pause.data.local.dao.TierDao
import com.astute.pause.data.local.entity.Answer
import com.astute.pause.data.local.entity.Item
import com.astute.pause.data.local.entity.Question
import com.astute.pause.data.local.entity.Tier

@Database(
    entities = [Item::class, Answer::class, Question::class, Tier::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PauseDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun answerDao(): AnswerDao
    abstract fun questionDao(): QuestionDao
    abstract fun tierDao(): TierDao

    companion object {
        const val NAME = "pause.db"

        val SEED_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                SeedData.insertInto(db)
            }
        }
    }
}
