package com.workeasy.clockincompanion.data.offline

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ClockEventEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clockEventDao(): ClockEventDao
}
