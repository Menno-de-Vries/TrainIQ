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

    val Migration3To4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE workout_sets ADD COLUMN set_type TEXT NOT NULL DEFAULT 'WORKING'")
            db.execSQL("ALTER TABLE workout_exercises ADD COLUMN set_type TEXT NOT NULL DEFAULT 'WORKING'")
            db.execSQL("ALTER TABLE workout_exercises ADD COLUMN superset_group_id INTEGER")
            db.execSQL("ALTER TABLE workout_exercises ADD COLUMN order_index INTEGER NOT NULL DEFAULT 0")
        }
    }

    val Migration4To5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE workout_exercises ADD COLUMN target_weight_kg REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE workout_exercises ADD COLUMN target_rpe REAL NOT NULL DEFAULT 0.0")
        }
    }

    val Migration5To6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS routine_sets (
                    id INTEGER NOT NULL,
                    workoutExerciseId INTEGER NOT NULL,
                    order_index INTEGER NOT NULL DEFAULT 0,
                    set_type TEXT NOT NULL DEFAULT 'NORMAL',
                    target_reps INTEGER NOT NULL DEFAULT 0,
                    target_weight_kg REAL NOT NULL DEFAULT 0.0,
                    rest_seconds INTEGER NOT NULL DEFAULT 0,
                    target_rpe REAL NOT NULL DEFAULT 0.0,
                    target_rir INTEGER,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
        }
    }

    val Migration6To7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN routine_id INTEGER")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN workout_day_id INTEGER")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN started_at INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN ended_at INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE workout_sets ADD COLUMN rest_seconds INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE workout_sets ADD COLUMN order_index INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE workout_sets ADD COLUMN completed INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE workout_sets ADD COLUMN logged_at INTEGER NOT NULL DEFAULT 0")
        }
    }

    val Migration7To8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN completed INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE workout_sets ADD COLUMN performed_exercise_id INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE workout_sets ADD COLUMN completed_at INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS performed_exercises (
                    id INTEGER NOT NULL,
                    session_id INTEGER NOT NULL,
                    exercise_id INTEGER NOT NULL,
                    source_workout_exercise_id INTEGER,
                    order_index INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_sets_workoutExerciseId_order_index ON routine_sets(workoutExerciseId, order_index)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_routine_id ON workout_sessions(routine_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_workout_day_id ON workout_sessions(workout_day_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_status_date ON workout_sessions(status, date)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_performed_exercises_session_id_order_index ON performed_exercises(session_id, order_index)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_performed_exercises_exercise_id ON performed_exercises(exercise_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_performed_exercises_source_workout_exercise_id ON performed_exercises(source_workout_exercise_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sets_sessionId_order_index ON workout_sets(sessionId, order_index)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sets_exerciseId ON workout_sets(exerciseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sets_performed_exercise_id_order_index ON workout_sets(performed_exercise_id, order_index)")
        }
    }
}
