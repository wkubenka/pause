package com.astute.pause.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.work.WorkManager
import com.astute.pause.data.local.PauseDatabase
import com.astute.pause.data.settings.DataStoreNotificationSettings
import com.astute.pause.data.settings.NotificationSettings
import com.astute.pause.domain.ClockTicker
import com.astute.pause.domain.NowTicker
import com.astute.pause.reminders.AndroidNotifier
import com.astute.pause.reminders.Notifier
import com.astute.pause.reminders.ReminderScheduler
import com.astute.pause.reminders.WorkManagerReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): PauseDatabase =
        Room.databaseBuilder(context, PauseDatabase::class.java, PauseDatabase.NAME)
            .addCallback(PauseDatabase.SEED_CALLBACK)
            .build()

    @Provides
    fun clock(): Clock = Clock.systemDefaultZone()

    @Provides @Singleton
    fun workManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    @Provides @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun reminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler
    @Binds abstract fun notifier(impl: AndroidNotifier): Notifier
    @Binds abstract fun notificationSettings(impl: DataStoreNotificationSettings): NotificationSettings
    @Binds abstract fun nowTicker(impl: ClockTicker): NowTicker
}
