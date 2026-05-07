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
            db.addColumnIfMissing("workout_sets", "repsInReserve", "INTEGER")
            db.execSQL("ALTER TABLE workout_sets ADD COLUMN set_type TEXT NOT NULL DEFAULT 'WORKING'")
            db.execSQL("ALTER TABLE workout_exercises ADD COLUMN set_type TEXT NOT NULL DEFAULT 'WORKING'")
            db.execSQL("ALTER TABLE workout_exercises ADD COLUMN superset_group_id INTEGER")
            db.execSQL("ALTER TABLE workout_exercises ADD COLUMN order_index INTEGER NOT NULL DEFAULT 0")
        }
    }

    val Migration4To5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.addColumnIfMissing("workout_sets", "repsInReserve", "INTEGER")
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

    val Migration8To9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN debrief_summary TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN debrief_progression_feedback TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN debrief_recommendation TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN debrief_next_session_focus TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN debrief_recovery_score INTEGER NOT NULL DEFAULT 75")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN debrief_intensity_signal TEXT NOT NULL DEFAULT 'MAINTAIN'")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN debrief_wins TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN debrief_risks TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN debrief_next_load_target TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN debrief_recovery_advice TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE workout_sessions ADD COLUMN debrief_source TEXT NOT NULL DEFAULT 'LOCAL_FALLBACK'")
        }
    }

    val Migration9To10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS food_items (
                    id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    barcode TEXT,
                    calories_per_100g REAL NOT NULL,
                    protein_per_100g REAL NOT NULL,
                    carbs_per_100g REAL NOT NULL,
                    fat_per_100g REAL NOT NULL,
                    source_type TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recipes (
                    id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    notes TEXT,
                    total_cooked_grams REAL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recipe_ingredients (
                    id INTEGER NOT NULL,
                    recipe_id INTEGER NOT NULL,
                    food_item_id INTEGER NOT NULL,
                    grams_used REAL NOT NULL,
                    order_index INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_recipe_id_order_index ON recipe_ingredients(recipe_id, order_index)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_food_item_id ON recipe_ingredients(food_item_id)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS meal_items (
                    id INTEGER NOT NULL,
                    meal_id INTEGER NOT NULL,
                    item_type TEXT NOT NULL,
                    reference_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    grams_used REAL NOT NULL,
                    calories REAL NOT NULL,
                    protein REAL NOT NULL,
                    carbs REAL NOT NULL,
                    fat REAL NOT NULL,
                    notes TEXT,
                    order_index INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_items_meal_id_order_index ON meal_items(meal_id, order_index)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_items_reference_id ON meal_items(reference_id)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS active_workout_sessions (
                    sessionId INTEGER NOT NULL,
                    dayId INTEGER NOT NULL,
                    routineId INTEGER,
                    startedAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    restTimerEndsAt INTEGER,
                    restTimerTotalSeconds INTEGER NOT NULL,
                    PRIMARY KEY(sessionId)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS active_workout_drafts (
                    session_id INTEGER NOT NULL,
                    exercise_id INTEGER NOT NULL,
                    weight TEXT NOT NULL,
                    reps TEXT NOT NULL,
                    rpe TEXT NOT NULL,
                    set_type TEXT NOT NULL,
                    PRIMARY KEY(session_id, exercise_id)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_drafts_session_id ON active_workout_drafts(session_id)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS active_workout_collapsed_exercises (
                    session_id INTEGER NOT NULL,
                    exercise_id INTEGER NOT NULL,
                    PRIMARY KEY(session_id, exercise_id)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_collapsed_exercises_session_id ON active_workout_collapsed_exercises(session_id)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS active_workout_sets (
                    session_id INTEGER NOT NULL,
                    id INTEGER NOT NULL,
                    exercise_id INTEGER NOT NULL,
                    performed_exercise_id INTEGER NOT NULL,
                    source_workout_exercise_id INTEGER,
                    weight REAL NOT NULL,
                    reps INTEGER NOT NULL,
                    rpe REAL NOT NULL,
                    reps_in_reserve INTEGER,
                    set_type TEXT NOT NULL,
                    rest_seconds INTEGER NOT NULL,
                    order_index INTEGER NOT NULL,
                    completed INTEGER NOT NULL,
                    logged_at INTEGER NOT NULL,
                    PRIMARY KEY(session_id, id)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_sets_session_id_order_index ON active_workout_sets(session_id, order_index)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_sets_exercise_id ON active_workout_sets(exercise_id)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS workout_log_events (
                    id INTEGER NOT NULL,
                    day_id INTEGER NOT NULL,
                    session_id INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    sync_status TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    undo_expires_at INTEGER,
                    target_event_id INTEGER,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_log_events_day_id_created_at ON workout_log_events(day_id, created_at)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_log_events_session_id_created_at ON workout_log_events(session_id, created_at)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_log_events_sync_status ON workout_log_events(sync_status)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS workout_log_event_sets (
                    event_id INTEGER NOT NULL,
                    snapshot_role TEXT NOT NULL,
                    snapshot_index INTEGER NOT NULL,
                    id INTEGER NOT NULL,
                    exercise_id INTEGER NOT NULL,
                    performed_exercise_id INTEGER NOT NULL,
                    source_workout_exercise_id INTEGER,
                    weight REAL NOT NULL,
                    reps INTEGER NOT NULL,
                    rpe REAL NOT NULL,
                    reps_in_reserve INTEGER,
                    set_type TEXT NOT NULL,
                    rest_seconds INTEGER NOT NULL,
                    order_index INTEGER NOT NULL,
                    completed INTEGER NOT NULL,
                    logged_at INTEGER NOT NULL,
                    PRIMARY KEY(event_id, snapshot_role, snapshot_index)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_log_event_sets_event_id ON workout_log_event_sets(event_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_log_event_sets_exercise_id ON workout_log_event_sets(exercise_id)")
        }
    }

    val Migration10To11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS room_mirror_import_runs (
                    generation_id TEXT NOT NULL,
                    source_fingerprint TEXT NOT NULL,
                    started_at INTEGER NOT NULL,
                    finished_at INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    schema_version INTEGER NOT NULL,
                    expected_row_count INTEGER NOT NULL,
                    imported_row_count INTEGER NOT NULL,
                    stale_row_count INTEGER NOT NULL,
                    mismatch_count INTEGER NOT NULL,
                    json_authoritative INTEGER NOT NULL DEFAULT 1,
                    room_authoritative INTEGER NOT NULL DEFAULT 0,
                    error_type TEXT,
                    PRIMARY KEY(generation_id)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_room_mirror_import_runs_status_finished_at ON room_mirror_import_runs(status, finished_at)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_room_mirror_import_runs_source_fingerprint ON room_mirror_import_runs(source_fingerprint)")
        }
    }

    val All = arrayOf(
        Migration2To3,
        Migration3To4,
        Migration4To5,
        Migration5To6,
        Migration6To7,
        Migration7To8,
        Migration8To9,
        Migration9To10,
        Migration10To11,
    )

    private fun SupportSQLiteDatabase.addColumnIfMissing(
        table: String,
        column: String,
        definition: String,
    ) {
        val exists = query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }
                .any { it == column }
        }
        if (!exists) {
            execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
        }
    }
}
