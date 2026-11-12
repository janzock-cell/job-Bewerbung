package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE cover_letters ADD COLUMN recipientCompany TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE cover_letters ADD COLUMN recipientPerson TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE cover_letters ADD COLUMN recipientStreet TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE cover_letters ADD COLUMN recipientCityZip TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE cover_letters ADD COLUMN status TEXT NOT NULL DEFAULT 'Entwurf'")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `vault_documents` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `sourceType` TEXT NOT NULL,
                    `extractedText` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "bewerbungs_helfer_db"
            )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
            INSTANCE = instance
            instance
        }
    }
}
