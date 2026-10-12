package com.astute.pause.testutil

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.astute.pause.data.local.PauseDatabase

object TestDb {
    fun create(): PauseDatabase =
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), PauseDatabase::class.java)
            .addCallback(PauseDatabase.SEED_CALLBACK)
            .allowMainThreadQueries()
            .build()
}
