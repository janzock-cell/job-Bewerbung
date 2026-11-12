package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserProfile::class, Experience::class, Skill::class, CoverLetter::class, VaultDocument::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun applicationDao(): ApplicationDao
}
