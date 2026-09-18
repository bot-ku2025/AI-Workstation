package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectEntity::class,
        ProjectPromptEntity::class,
        SharedCheckpointEntity::class,
        WorkHistoryEntity::class,
        SettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class WorkstationDatabase : RoomDatabase() {
    abstract fun workstationDao(): WorkstationDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'DARK'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN accentColor TEXT NOT NULL DEFAULT 'CYAN'")
            }
        }

        @Volatile
        private var INSTANCE: WorkstationDatabase? = null

        fun getDatabase(context: Context): WorkstationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkstationDatabase::class.java,
                    "ai_workstation.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
