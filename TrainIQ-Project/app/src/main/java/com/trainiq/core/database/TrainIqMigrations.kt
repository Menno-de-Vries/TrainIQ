package com.trainiq.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object TrainIqMigrations {
    val Migration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE user_profile ADD COLUMN age INTEGER NOT NULL DEFAULT 30")
            db.execSQL("ALTER TABLE user_profile ADD COLUMN sex TEXT NOT NULL DEFAULT 'MALE'")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN caloriesBurned INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE meals ADD COLUMN mealType TEXT NOT NULL DEFAULT 'LUNCH'")
            db.execSQL("ALTER TABLE meals ADD COLUMN name TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE meals ADD COLUMN notes TEXT")
        }
    }
}
