package com.workeasy.clockincompanion.di

import android.content.Context
import androidx.room.Room
import com.workeasy.clockincompanion.data.offline.AppDatabase
import com.workeasy.clockincompanion.data.offline.ClockEventDao
import com.workeasy.clockincompanion.data.offline.RoomClockEventStore
import com.workeasy.clockincompanion.data.offline.WorkManagerFlushScheduler
import com.workeasy.clockincompanion.domain.store.ClockEventStore
import com.workeasy.clockincompanion.domain.store.OfflineFlushScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OfflineModule {

    @Binds
    @Singleton
    abstract fun bindClockEventStore(impl: RoomClockEventStore): ClockEventStore

    @Binds
    @Singleton
    abstract fun bindFlushScheduler(impl: WorkManagerFlushScheduler): OfflineFlushScheduler

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "clock_in_db").build()

        @Provides
        fun provideClockEventDao(db: AppDatabase): ClockEventDao = db.clockEventDao()
    }
}
