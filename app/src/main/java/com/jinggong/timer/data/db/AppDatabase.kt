package com.jinggong.timer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SessionEntity::class, AudioEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun audioDao(): AudioDao

    companion object {
        const val NAME = "jinggong.db"
    }
}
