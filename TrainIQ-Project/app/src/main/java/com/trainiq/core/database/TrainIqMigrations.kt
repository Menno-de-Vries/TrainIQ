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

    val Migration11To12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys=OFF")
            db.removeVersion11ForeignKeyOrphans()
            db.recreateTable(
                table = "workout_days",
                columns = "id, routineId, name, orderIndex",
                createSql = """
                    CREATE TABLE workout_days (
                        id INTEGER NOT NULL,
                        routineId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        orderIndex INTEGER NOT NULL,
                        PRIMARY KEY(id),
                        FOREIGN KEY(routineId) REFERENCES workout_routines(id) ON DELETE CASCADE
                    )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_days_routineId ON workout_days(routineId)")

            db.recreateTable(
                table = "workout_exercises",
                columns = """
                    id, dayId, exerciseId, targetSets, repRange, restSeconds,
                    target_weight_kg, target_rpe, set_type, superset_group_id, order_index
                """.trimIndent(),
                createSql = """
                    CREATE TABLE workout_exercises (
                        id INTEGER NOT NULL,
                        dayId INTEGER NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        targetSets INTEGER NOT NULL,
                        repRange TEXT NOT NULL,
                        restSeconds INTEGER NOT NULL,
                        target_weight_kg REAL NOT NULL DEFAULT 0.0,
                        target_rpe REAL NOT NULL DEFAULT 0.0,
                        set_type TEXT NOT NULL DEFAULT 'WORKING',
                        superset_group_id INTEGER,
                        order_index INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(id),
                        FOREIGN KEY(dayId) REFERENCES workout_days(id) ON DELETE CASCADE,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE RESTRICT
                    )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_exercises_dayId ON workout_exercises(dayId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_exercises_exerciseId ON workout_exercises(exerciseId)")

            db.recreateTable(
                table = "routine_sets",
                columns = """
                    id, workoutExerciseId, order_index, set_type, target_reps,
                    target_weight_kg, rest_seconds, target_rpe, target_rir
                """.trimIndent(),
                createSql = """
                    CREATE TABLE routine_sets (
                        id INTEGER NOT NULL,
                        workoutExerciseId INTEGER NOT NULL,
                        order_index INTEGER NOT NULL DEFAULT 0,
                        set_type TEXT NOT NULL DEFAULT 'NORMAL',
                        target_reps INTEGER NOT NULL DEFAULT 0,
                        target_weight_kg REAL NOT NULL DEFAULT 0.0,
                        rest_seconds INTEGER NOT NULL DEFAULT 0,
                        target_rpe REAL NOT NULL DEFAULT 0.0,
                        target_rir INTEGER,
                        PRIMARY KEY(id),
                        FOREIGN KEY(workoutExerciseId) REFERENCES workout_exercises(id) ON DELETE CASCADE
                    )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_sets_workoutExerciseId_order_index ON routine_sets(workoutExerciseId, order_index)")

            db.recreateTable(
                table = "workout_sessions",
                columns = """
                    id, date, duration, caloriesBurned, routine_id, workout_day_id,
                    started_at, ended_at, status, completed, debrief_summary,
                    debrief_progression_feedback, debrief_recommendation,
                    debrief_next_session_focus, debrief_recovery_score,
                    debrief_intensity_signal, debrief_wins, debrief_risks,
                    debrief_next_load_target, debrief_recovery_advice, debrief_source
                """.trimIndent(),
                createSql = """
                    CREATE TABLE workout_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        duration INTEGER NOT NULL,
                        caloriesBurned INTEGER NOT NULL DEFAULT 0,
                        routine_id INTEGER,
                        workout_day_id INTEGER,
                        started_at INTEGER NOT NULL DEFAULT 0,
                        ended_at INTEGER NOT NULL DEFAULT 0,
                        status TEXT NOT NULL DEFAULT 'COMPLETED',
                        completed INTEGER NOT NULL DEFAULT 1,
                        debrief_summary TEXT NOT NULL DEFAULT '',
                        debrief_progression_feedback TEXT NOT NULL DEFAULT '',
                        debrief_recommendation TEXT NOT NULL DEFAULT '',
                        debrief_next_session_focus TEXT NOT NULL DEFAULT '',
                        debrief_recovery_score INTEGER NOT NULL DEFAULT 75,
                        debrief_intensity_signal TEXT NOT NULL DEFAULT 'MAINTAIN',
                        debrief_wins TEXT NOT NULL DEFAULT '',
                        debrief_risks TEXT NOT NULL DEFAULT '',
                        debrief_next_load_target TEXT NOT NULL DEFAULT '',
                        debrief_recovery_advice TEXT NOT NULL DEFAULT '',
                        debrief_source TEXT NOT NULL DEFAULT 'LOCAL_FALLBACK',
                        FOREIGN KEY(routine_id) REFERENCES workout_routines(id) ON DELETE SET NULL,
                        FOREIGN KEY(workout_day_id) REFERENCES workout_days(id) ON DELETE SET NULL
                    )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_routine_id ON workout_sessions(routine_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_workout_day_id ON workout_sessions(workout_day_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_status_date ON workout_sessions(status, date)")

            db.recreateTable(
                table = "performed_exercises",
                columns = "id, session_id, exercise_id, source_workout_exercise_id, order_index",
                createSql = """
                    CREATE TABLE performed_exercises (
                        id INTEGER NOT NULL,
                        session_id INTEGER NOT NULL,
                        exercise_id INTEGER NOT NULL,
                        source_workout_exercise_id INTEGER,
                        order_index INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(id),
                        FOREIGN KEY(session_id) REFERENCES workout_sessions(id) ON DELETE CASCADE,
                        FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON DELETE RESTRICT,
                        FOREIGN KEY(source_workout_exercise_id) REFERENCES workout_exercises(id) ON DELETE SET NULL
                    )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_performed_exercises_session_id_order_index ON performed_exercises(session_id, order_index)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_performed_exercises_exercise_id ON performed_exercises(exercise_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_performed_exercises_source_workout_exercise_id ON performed_exercises(source_workout_exercise_id)")

            db.recreateTable(
                table = "workout_sets",
                columns = """
                    id, sessionId, exerciseId, weight, reps, rpe, repsInReserve,
                    performed_exercise_id, set_type, rest_seconds, order_index,
                    completed, logged_at, completed_at
                """.trimIndent(),
                createSql = """
                    CREATE TABLE workout_sets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        weight REAL NOT NULL,
                        reps INTEGER NOT NULL,
                        rpe REAL NOT NULL,
                        repsInReserve INTEGER,
                        performed_exercise_id INTEGER NOT NULL DEFAULT 0,
                        set_type TEXT NOT NULL DEFAULT 'WORKING',
                        rest_seconds INTEGER NOT NULL DEFAULT 0,
                        order_index INTEGER NOT NULL DEFAULT 0,
                        completed INTEGER NOT NULL DEFAULT 1,
                        logged_at INTEGER NOT NULL DEFAULT 0,
                        completed_at INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON DELETE CASCADE,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE RESTRICT
                    )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sets_sessionId_order_index ON workout_sets(sessionId, order_index)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sets_exerciseId ON workout_sets(exerciseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sets_performed_exercise_id_order_index ON workout_sets(performed_exercise_id, order_index)")

            db.recreateTable(
                table = "recipe_ingredients",
                columns = "id, recipe_id, food_item_id, grams_used, order_index",
                createSql = """
                    CREATE TABLE recipe_ingredients (
                        id INTEGER NOT NULL,
                        recipe_id INTEGER NOT NULL,
                        food_item_id INTEGER NOT NULL,
                        grams_used REAL NOT NULL,
                        order_index INTEGER NOT NULL,
                        PRIMARY KEY(id),
                        FOREIGN KEY(recipe_id) REFERENCES recipes(id) ON DELETE CASCADE,
                        FOREIGN KEY(food_item_id) REFERENCES food_items(id) ON DELETE RESTRICT
                    )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_recipe_id_order_index ON recipe_ingredients(recipe_id, order_index)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_food_item_id ON recipe_ingredients(food_item_id)")

            db.recreateTable(
                table = "meal_items",
                columns = """
                    id, meal_id, item_type, reference_id, name, grams_used,
                    calories, protein, carbs, fat, notes, order_index
                """.trimIndent(),
                createSql = """
                    CREATE TABLE meal_items (
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
                        PRIMARY KEY(id),
                        FOREIGN KEY(meal_id) REFERENCES meals(id) ON DELETE CASCADE
                    )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_items_meal_id_order_index ON meal_items(meal_id, order_index)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_items_reference_id ON meal_items(reference_id)")

            db.recreateTable(
                table = "active_workout_sessions",
                columns = "sessionId, dayId, routineId, startedAt, updatedAt, restTimerEndsAt, restTimerTotalSeconds",
                createSql = """
                    CREATE TABLE active_workout_sessions (
                        sessionId INTEGER NOT NULL,
                        dayId INTEGER NOT NULL,
                        routineId INTEGER,
                        startedAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        restTimerEndsAt INTEGER,
                        restTimerTotalSeconds INTEGER NOT NULL,
                        PRIMARY KEY(sessionId),
                        FOREIGN KEY(dayId) REFERENCES workout_days(id) ON DELETE CASCADE,
                        FOREIGN KEY(routineId) REFERENCES workout_routines(id) ON DELETE SET NULL
                    )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_sessions_dayId ON active_workout_sessions(dayId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_sessions_routineId ON active_workout_sessions(routineId)")

            db.recreateTable(
                table = "active_workout_drafts",
                columns = "session_id, exercise_id, weight, reps, rpe, set_type",
                createSql = """
                    CREATE TABLE active_workout_drafts (
                        session_id INTEGER NOT NULL,
                        exercise_id INTEGER NOT NULL,
                        weight TEXT NOT NULL,
                        reps TEXT NOT NULL,
                        rpe TEXT NOT NULL,
                        set_type TEXT NOT NULL,
                        PRIMARY KEY(session_id, exercise_id),
                        FOREIGN KEY(session_id) REFERENCES active_workout_sessions(sessionId) ON DELETE CASCADE,
                        FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON DELETE RESTRICT
                    )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_drafts_session_id ON active_workout_drafts(session_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_drafts_exercise_id ON active_workout_drafts(exercise_id)")

            db.recreateTable(
                table = "active_workout_collapsed_exercises",
                columns = "session_id, exercise_id",
                createSql = """
                    CREATE TABLE active_workout_collapsed_exercises (
                        session_id INTEGER NOT NULL,
                        exercise_id INTEGER NOT NULL,
                        PRIMARY KEY(session_id, exercise_id),
                        FOREIGN KEY(session_id) REFERENCES active_workout_sessions(sessionId) ON DELETE CASCADE,
                        FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON DELETE RESTRICT
                    )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_collapsed_exercises_session_id ON active_workout_collapsed_exercises(session_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_collapsed_exercises_exercise_id ON active_workout_collapsed_exercises(exercise_id)")

            db.recreateTable(
                table = "active_workout_sets",
                columns = """
                    session_id, id, exercise_id, performed_exercise_id, source_workout_exercise_id,
                    weight, reps, rpe, reps_in_reserve, set_type, rest_seconds, order_index,
                    completed, logged_at
                """.trimIndent(),
                createSql = """
                    CREATE TABLE active_workout_sets (
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
                        PRIMARY KEY(session_id, id),
                        FOREIGN KEY(session_id) REFERENCES active_workout_sessions(sessionId) ON DELETE CASCADE,
                        FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON DELETE RESTRICT
                    )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_sets_session_id_order_index ON active_workout_sets(session_id, order_index)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_sets_exercise_id ON active_workout_sets(exercise_id)")

            db.recreateTable(
                table = "workout_log_events",
                columns = "id, day_id, session_id, type, sync_status, created_at, undo_expires_at, target_event_id",
                createSql = """
                    CREATE TABLE workout_log_events (
                        id INTEGER NOT NULL,
                        day_id INTEGER NOT NULL,
                        session_id INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        sync_status TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        undo_expires_at INTEGER,
                        target_event_id INTEGER,
                        PRIMARY KEY(id),
                        FOREIGN KEY(day_id) REFERENCES workout_days(id) ON DELETE CASCADE
                    )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_log_events_day_id_created_at ON workout_log_events(day_id, created_at)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_log_events_session_id_created_at ON workout_log_events(session_id, created_at)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_log_events_sync_status ON workout_log_events(sync_status)")

            db.recreateTable(
                table = "workout_log_event_sets",
                columns = """
                    event_id, snapshot_role, snapshot_index, id, exercise_id, performed_exercise_id,
                    source_workout_exercise_id, weight, reps, rpe, reps_in_reserve, set_type,
                    rest_seconds, order_index, completed, logged_at
                """.trimIndent(),
                createSql = """
                    CREATE TABLE workout_log_event_sets (
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
                        PRIMARY KEY(event_id, snapshot_role, snapshot_index),
                        FOREIGN KEY(event_id) REFERENCES workout_log_events(id) ON DELETE CASCADE,
                        FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON DELETE RESTRICT
                    )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_log_event_sets_event_id ON workout_log_event_sets(event_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_log_event_sets_exercise_id ON workout_log_event_sets(exercise_id)")
            db.execSQL("PRAGMA foreign_keys=ON")
            db.assertNoForeignKeyViolations()
        }
    }

    val Migration12To13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.addColumnIfMissing("meal_items", "serving_count", "INTEGER NOT NULL DEFAULT 1")
        }
    }

    val Migration13To14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys=OFF")
            db.recreateTable(
                table = "active_workout_drafts",
                columns = """
                    session_id, exercise_id, weight, reps, rpe, set_type
                """.trimIndent(),
                createSql = """
                    CREATE TABLE active_workout_drafts (
                        session_id INTEGER NOT NULL,
                        exercise_id INTEGER NOT NULL,
                        weight TEXT NOT NULL,
                        reps TEXT NOT NULL,
                        rpe TEXT NOT NULL,
                        set_type TEXT NOT NULL,
                        PRIMARY KEY(session_id, exercise_id),
                        FOREIGN KEY(session_id) REFERENCES active_workout_sessions(sessionId) ON DELETE CASCADE
                    )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_drafts_session_id ON active_workout_drafts(session_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_drafts_exercise_id ON active_workout_drafts(exercise_id)")

            db.recreateTable(
                table = "active_workout_collapsed_exercises",
                columns = """
                    session_id, exercise_id
                """.trimIndent(),
                createSql = """
                    CREATE TABLE active_workout_collapsed_exercises (
                        session_id INTEGER NOT NULL,
                        exercise_id INTEGER NOT NULL,
                        PRIMARY KEY(session_id, exercise_id),
                        FOREIGN KEY(session_id) REFERENCES active_workout_sessions(sessionId) ON DELETE CASCADE
                    )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_collapsed_exercises_session_id ON active_workout_collapsed_exercises(session_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_active_workout_collapsed_exercises_exercise_id ON active_workout_collapsed_exercises(exercise_id)")
            db.execSQL("PRAGMA foreign_keys=ON")
            db.assertNoForeignKeyViolations()
        }
    }

    val Migration14To15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.addColumnIfMissing("food_items", "default_serving_grams", "REAL NOT NULL DEFAULT 100.0")
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
        Migration11To12,
        Migration12To13,
        Migration13To14,
        Migration14To15,
    )

    private fun SupportSQLiteDatabase.recreateTable(
        table: String,
        columns: String,
        createSql: String,
    ) {
        val newTable = "${table}_v12"
        execSQL(createSql.replace("CREATE TABLE $table", "CREATE TABLE $newTable"))
        execSQL("INSERT INTO $newTable ($columns) SELECT $columns FROM $table")
        execSQL("DROP TABLE $table")
        execSQL("ALTER TABLE $newTable RENAME TO $table")
    }

    private fun SupportSQLiteDatabase.removeVersion11ForeignKeyOrphans() {
        execSQL("DELETE FROM workout_log_event_sets WHERE event_id NOT IN (SELECT id FROM workout_log_events)")
        execSQL("DELETE FROM workout_log_event_sets WHERE exercise_id NOT IN (SELECT id FROM exercises)")
        execSQL("DELETE FROM workout_log_events WHERE day_id NOT IN (SELECT id FROM workout_days)")

        execSQL("DELETE FROM active_workout_sets WHERE session_id NOT IN (SELECT sessionId FROM active_workout_sessions)")
        execSQL("DELETE FROM active_workout_sets WHERE exercise_id NOT IN (SELECT id FROM exercises)")
        execSQL("DELETE FROM active_workout_drafts WHERE session_id NOT IN (SELECT sessionId FROM active_workout_sessions)")
        execSQL("DELETE FROM active_workout_drafts WHERE exercise_id NOT IN (SELECT id FROM exercises)")
        execSQL("DELETE FROM active_workout_collapsed_exercises WHERE session_id NOT IN (SELECT sessionId FROM active_workout_sessions)")
        execSQL("DELETE FROM active_workout_collapsed_exercises WHERE exercise_id NOT IN (SELECT id FROM exercises)")
        execSQL("DELETE FROM active_workout_sessions WHERE dayId NOT IN (SELECT id FROM workout_days)")
        execSQL("UPDATE active_workout_sessions SET routineId = NULL WHERE routineId IS NOT NULL AND routineId NOT IN (SELECT id FROM workout_routines)")

        execSQL("DELETE FROM meal_items WHERE meal_id NOT IN (SELECT id FROM meals)")
        execSQL("DELETE FROM recipe_ingredients WHERE recipe_id NOT IN (SELECT id FROM recipes)")
        execSQL("DELETE FROM recipe_ingredients WHERE food_item_id NOT IN (SELECT id FROM food_items)")

        execSQL("DELETE FROM workout_sets WHERE sessionId NOT IN (SELECT id FROM workout_sessions)")
        execSQL("DELETE FROM workout_sets WHERE exerciseId NOT IN (SELECT id FROM exercises)")
        execSQL("DELETE FROM performed_exercises WHERE session_id NOT IN (SELECT id FROM workout_sessions)")
        execSQL("DELETE FROM performed_exercises WHERE exercise_id NOT IN (SELECT id FROM exercises)")
        execSQL("UPDATE performed_exercises SET source_workout_exercise_id = NULL WHERE source_workout_exercise_id IS NOT NULL AND source_workout_exercise_id NOT IN (SELECT id FROM workout_exercises)")
        execSQL("UPDATE workout_sessions SET routine_id = NULL WHERE routine_id IS NOT NULL AND routine_id NOT IN (SELECT id FROM workout_routines)")
        execSQL("UPDATE workout_sessions SET workout_day_id = NULL WHERE workout_day_id IS NOT NULL AND workout_day_id NOT IN (SELECT id FROM workout_days)")

        execSQL("DELETE FROM routine_sets WHERE workoutExerciseId NOT IN (SELECT id FROM workout_exercises)")
        execSQL("DELETE FROM workout_exercises WHERE dayId NOT IN (SELECT id FROM workout_days)")
        execSQL("DELETE FROM workout_exercises WHERE exerciseId NOT IN (SELECT id FROM exercises)")
        execSQL("DELETE FROM workout_days WHERE routineId NOT IN (SELECT id FROM workout_routines)")
    }

    private fun SupportSQLiteDatabase.assertNoForeignKeyViolations() {
        query("PRAGMA foreign_key_check").use { cursor ->
            check(!cursor.moveToFirst()) {
                val tableIndex = cursor.getColumnIndex("table")
                val rowIdIndex = cursor.getColumnIndex("rowid")
                val table = cursor.getString(tableIndex)
                val rowId = cursor.getLong(rowIdIndex)
                "Room v12 migration left a foreign-key violation in $table rowid=$rowId"
            }
        }
    }

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
