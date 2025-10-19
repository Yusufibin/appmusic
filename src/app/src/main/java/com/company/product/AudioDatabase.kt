package com.company.product

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Preset::class, LogEntry::class], version = 1)
abstract class AudioDatabase : RoomDatabase() {
    abstract fun audioDao(): AudioDao
}