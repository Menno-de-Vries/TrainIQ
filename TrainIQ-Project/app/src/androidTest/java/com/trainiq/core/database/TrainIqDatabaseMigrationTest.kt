package com.trainiq.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrainIqDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TrainIqDatabase::class.java,
    )

    @Test
    fun migration9To10PreservesExistingDataAndCreatesNewTables() {
        helper.createDatabase(TEST_DB, 9).apply {
            seedVersion9Data()
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            10,
            true,
            TrainIqMigrations.Migration9To10,
        )

        migrated.assertVersion9DataSurvived()
        migrated.assertVersion10TablesAreUsable()
        migrated.close()
    }

    @Test
    fun migration10To11PreservesMirrorDataAndCreatesImportMetadataTable() {
        helper.createDatabase(TEST_DB, 10).apply {
            seedVersion10MirrorData()
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            11,
            true,
            TrainIqMigrations.Migration10To11,
        )

        migrated.assertVersion10MirrorDataSurvived()
        migrated.assertVersion11MetadataTableIsUsable()
        migrated.close()
    }

    @Test
    fun migration11To12AddsForeignKeysAndPreservesRelationalData() {
        helper.createDatabase(TEST_DB, 11).apply {
            seedVersion11RelationalData()
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            12,
            true,
            TrainIqMigrations.Migration11To12,
        )

        migrated.assertVersion11RelationalDataSurvived()
        migrated.assertForeignKey("workout_days", "workout_routines")
        migrated.assertForeignKey("workout_exercises", "workout_days")
        migrated.assertForeignKey("workout_exercises", "exercises")
        migrated.assertForeignKey("routine_sets", "workout_exercises")
        migrated.assertForeignKey("performed_exercises", "workout_sessions")
        migrated.assertForeignKey("workout_sets", "workout_sessions")
        migrated.assertForeignKey("recipe_ingredients", "recipes")
        migrated.assertForeignKey("meal_items", "meals")
        migrated.assertForeignKey("active_workout_sets", "active_workout_sessions")
        migrated.assertForeignKey("workout_log_event_sets", "workout_log_events")
        migrated.assertRejectsForeignKeyViolation()
        migrated.close()
    }

    @Test
    fun migration11To12RemovesDirtyLegacyOrphansBeforeEnforcingForeignKeys() {
        helper.createDatabase(TEST_DB, 11).apply {
            seedVersion11RelationalData()
            seedVersion11OrphanRows()
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            12,
            true,
            TrainIqMigrations.Migration11To12,
        )

        migrated.assertVersion11RelationalDataSurvived()
        migrated.assertNoForeignKeyViolations()
        migrated.assertVersion11OrphansRemoved()
        migrated.close()
    }

    @Test
    fun migration12To13AddsServingCountAndPreservesMealItems() {
        helper.createDatabase(TEST_DB, 12).apply {
            seedVersion11RelationalData()
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            13,
            true,
            TrainIqMigrations.Migration12To13,
        )

        migrated.assertMealItemServingCountDefaulted()
        migrated.close()
    }

    @Test
    fun migration14To15AddsDefaultServingGramsToFoodItems() {
        helper.createDatabase(TEST_DB, 14).apply {
            seedVersion11RelationalData()
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            15,
            true,
            TrainIqMigrations.Migration14To15,
        )

        migrated.assertFoodDefaultServingGramsDefaulted()
        migrated.close()
    }

    @Test
    fun migration15To16CreatesEmptySavedGoalAdviceTableAndPreservesProfile() {
        helper.createDatabase(TEST_DB, 15).apply {
            seedVersion11RelationalData()
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            16,
            true,
            TrainIqMigrations.Migration15To16,
        )

        migrated.query("SELECT name FROM user_profile WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Migration Athlete", cursor.getString(0))
        }
        migrated.execSQL(
            """
            INSERT INTO saved_goal_advice (
                id, profile_fingerprint, saved_at, bmr, maintenance_calories, activity_multiplier,
                calorie_target, protein_target, carbs_target, fat_target, training_focus, summary,
                calorie_advice, macro_advice, activity_explanation, attention_points_json, advice,
                data_quality, source, raw_response
            ) VALUES (1, 'profile-v1', 1725000000000, 1820, 2750, 1.55, 2450, 180, 260, 75,
                'Progressieve overload', 'Sterke basis.', '', '', '', '[]', 'Train vier keer per week.',
                '', 'LOCAL_CALCULATION', NULL)
            """.trimIndent(),
        )
        migrated.query("SELECT summary FROM saved_goal_advice WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Sterke basis.", cursor.getString(0))
        }
        migrated.close()
    }

    @Test
    fun migration13To14RemovesDraftExerciseForeignKeysAndPreservesDraftState() {
        helper.createDatabase(TEST_DB, 13).apply {
            seedVersion11RelationalData()
            seedActiveWorkoutDraftState()
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            14,
            true,
            TrainIqMigrations.Migration13To14,
        )

        migrated.assertActiveWorkoutDraftStateSurvived()
        migrated.assertNoForeignKey("active_workout_drafts", "exercises")
        migrated.assertNoForeignKey("active_workout_collapsed_exercises", "exercises")
        migrated.close()
    }

    @Test
    fun migration12To15PreservesNutritionAndActiveWorkoutState() {
        helper.createDatabase(TEST_DB, 12).apply {
            seedVersion11RelationalData()
            seedActiveWorkoutDraftState()
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            15,
            true,
            TrainIqMigrations.Migration12To13,
            TrainIqMigrations.Migration13To14,
            TrainIqMigrations.Migration14To15,
        )

        migrated.assertMealItemServingCountDefaulted()
        migrated.assertFoodDefaultServingGramsDefaulted()
        migrated.assertActiveWorkoutDraftStateSurvived()
        migrated.assertNoForeignKeyViolations()
        migrated.close()
    }

    @Test
    fun olderMigrationChainFromVersions2Through8PreservesLegacyDataAndReachesCurrentSchema() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        (2..8).forEach { version ->
            val dbName = "$TEST_DB-chain-v$version"
            context.deleteDatabase(dbName)
            try {
                createLegacyDatabase(context, dbName, version)

                val migrated = Room.databaseBuilder(
                    context,
                    TrainIqDatabase::class.java,
                    dbName,
                )
                    .addMigrations(*TrainIqMigrations.All)
                    .build()

                val db = migrated.openHelper.writableDatabase
                db.assertLegacyChainDataSurvived(version)
                db.assertCurrentSchemaUsableAfterLegacyChain(version)
                migrated.close()
            } finally {
                context.deleteDatabase(dbName)
            }
        }
    }

    @Test
    fun migration3To11AddsRepsInReserveWhenLegacyMigratedDatabaseMissedIt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "$TEST_DB-chain-missing-reps-in-reserve"
        context.deleteDatabase(dbName)
        try {
            createLegacyDatabase(context, dbName, 3)

            val migrated = Room.databaseBuilder(
                context,
                TrainIqDatabase::class.java,
                dbName,
            )
                .addMigrations(*TrainIqMigrations.All)
                .build()

            val db = migrated.openHelper.writableDatabase
            db.query("SELECT repsInReserve FROM workout_sets WHERE id = 80").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(0))
            }
            migrated.close()
        } finally {
            context.deleteDatabase(dbName)
        }
    }

    private fun SupportSQLiteDatabase.seedVersion9Data() {
        execSQL(
            """
            INSERT INTO user_profile (
                id, name, age, sex, height, weight, bodyFat, activityLevel, goal,
                calorieTarget, proteinTarget, carbsTarget, fatTarget, trainingFocus
            ) VALUES (
                1, 'Migration Athlete', 34, 'MALE', 181.0, 82.5, 14.0, 'ACTIVE',
                'STRENGTH', 2800, 180, 300, 80, 'Hypertrophy'
            )
            """.trimIndent(),
        )
        execSQL("INSERT INTO workout_routines (id, name, description, active) VALUES (10, 'Upper Lower', 'Four days', 1)")
        execSQL("INSERT INTO workout_days (id, routineId, name, orderIndex) VALUES (20, 10, 'Upper A', 0)")
        execSQL("INSERT INTO exercises (id, name, muscleGroup, equipment) VALUES (30, 'Bench Press', 'Chest', 'Barbell')")
        execSQL(
            """
            INSERT INTO workout_exercises (
                id, dayId, exerciseId, targetSets, repRange, restSeconds,
                target_weight_kg, target_rpe, set_type, superset_group_id, order_index
            ) VALUES (40, 20, 30, 4, '6-8', 180, 100.0, 8.0, 'WORKING', NULL, 0)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO routine_sets (
                id, workoutExerciseId, order_index, set_type, target_reps,
                target_weight_kg, rest_seconds, target_rpe, target_rir
            ) VALUES (50, 40, 0, 'NORMAL', 8, 100.0, 180, 8.0, 2)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO workout_sessions (
                id, date, duration, caloriesBurned, routine_id, workout_day_id,
                started_at, ended_at, status, completed, debrief_summary,
                debrief_progression_feedback, debrief_recommendation,
                debrief_next_session_focus, debrief_recovery_score,
                debrief_intensity_signal, debrief_wins, debrief_risks,
                debrief_next_load_target, debrief_recovery_advice, debrief_source
            ) VALUES (
                60, 1714557600000, 3600000, 420, 10, 20, 1714557600000,
                1714561200000, 'COMPLETED', 1, 'Solid session', '', '', '',
                82, 'MAINTAIN', '', '', '', '', 'LOCAL_FALLBACK'
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO performed_exercises (
                id, session_id, exercise_id, source_workout_exercise_id, order_index
            ) VALUES (70, 60, 30, 40, 0)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO workout_sets (
                id, sessionId, exerciseId, weight, reps, rpe, repsInReserve,
                performed_exercise_id, set_type, rest_seconds, order_index,
                completed, logged_at, completed_at
            ) VALUES (80, 60, 30, 100.0, 8, 8.0, 2, 70, 'WORKING', 180, 0, 1, 1714557700000, 1714557710000)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO meals (id, date, mealType, name, notes, calories, protein, carbs, fat)
            VALUES (90, 1714557600000, 'LUNCH', 'Chicken rice', 'pre-workout', 640, 50, 70, 18)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO body_measurements (id, date, weight, bodyFat, muscleMass)
            VALUES (100, 1714557600000, 82.5, 14.0, 66.0)
            """.trimIndent(),
        )
    }

    private fun SupportSQLiteDatabase.assertVersion9DataSurvived() {
        query("SELECT name, trainingFocus FROM user_profile WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Migration Athlete", cursor.getString(0))
            assertEquals("Hypertrophy", cursor.getString(1))
        }
        query("SELECT COUNT(*) FROM workout_sets WHERE sessionId = 60 AND weight = 100.0").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        query("SELECT COUNT(*) FROM meals WHERE id = 90 AND calories = 640").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        query("SELECT COUNT(*) FROM body_measurements WHERE id = 100 AND weight = 82.5").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }

    private fun SupportSQLiteDatabase.assertVersion10TablesAreUsable() {
        execSQL(
            """
            INSERT INTO food_items (
                id, name, barcode, calories_per_100g, protein_per_100g,
                carbs_per_100g, fat_per_100g, source_type, created_at, updated_at
            ) VALUES (200, 'Greek yogurt', NULL, 59.0, 10.0, 3.6, 0.4, 'MANUAL', 1714557600000, 1714557600000)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO recipes (id, name, notes, total_cooked_grams, created_at, updated_at)
            VALUES (210, 'Protein bowl', 'simple', 500.0, 1714557600000, 1714557600000)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO recipe_ingredients (id, recipe_id, food_item_id, grams_used, order_index)
            VALUES (220, 210, 200, 250.0, 0)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO meal_items (
                id, meal_id, item_type, reference_id, name, grams_used,
                calories, protein, carbs, fat, notes, order_index
            ) VALUES (230, 90, 'FOOD', 200, 'Greek yogurt', 250.0, 147.5, 25.0, 9.0, 1.0, NULL, 0)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO active_workout_sessions (
                sessionId, dayId, routineId, startedAt, updatedAt, restTimerEndsAt, restTimerTotalSeconds
            ) VALUES (300, 20, 10, 1714557600000, 1714557700000, NULL, 0)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO active_workout_sets (
                session_id, id, exercise_id, performed_exercise_id, source_workout_exercise_id,
                weight, reps, rpe, reps_in_reserve, set_type, rest_seconds, order_index,
                completed, logged_at
            ) VALUES (300, 1, 30, 70, 40, 100.0, 8, 8.0, 2, 'NORMAL', 180, 0, 1, 1714557700000)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO workout_log_events (
                id, day_id, session_id, type, sync_status, created_at, undo_expires_at, target_event_id
            ) VALUES (400, 20, 300, 'ADD_SET', 'PENDING', 1714557700000, NULL, NULL)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO workout_log_event_sets (
                event_id, snapshot_role, snapshot_index, id, exercise_id, performed_exercise_id,
                source_workout_exercise_id, weight, reps, rpe, reps_in_reserve, set_type,
                rest_seconds, order_index, completed, logged_at
            ) VALUES (400, 'CURRENT', 0, 1, 30, 70, 40, 100.0, 8, 8.0, 2, 'NORMAL', 180, 0, 1, 1714557700000)
            """.trimIndent(),
        )

        query("SELECT COUNT(*) FROM food_items").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        query("SELECT COUNT(*) FROM recipe_ingredients WHERE recipe_id = 210").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        query("SELECT COUNT(*) FROM workout_log_event_sets WHERE event_id = 400").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }

    private fun SupportSQLiteDatabase.seedVersion10MirrorData() {
        execSQL(
            """
            INSERT INTO food_items (
                id, name, barcode, calories_per_100g, protein_per_100g,
                carbs_per_100g, fat_per_100g, source_type, created_at, updated_at
            ) VALUES (200, 'Greek yogurt', NULL, 59.0, 10.0, 3.6, 0.4, 'MANUAL', 1714557600000, 1714557600000)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO recipes (id, name, notes, total_cooked_grams, created_at, updated_at)
            VALUES (210, 'Protein bowl', 'simple', 500.0, 1714557600000, 1714557600000)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO recipe_ingredients (id, recipe_id, food_item_id, grams_used, order_index)
            VALUES (220, 210, 200, 250.0, 0)
            """.trimIndent(),
        )
    }

    private fun SupportSQLiteDatabase.seedVersion11RelationalData() {
        seedVersion9Data()
        assertVersion10TablesAreUsable()
        assertVersion11MetadataTableIsUsable()
    }

    private fun SupportSQLiteDatabase.seedVersion11OrphanRows() {
        execSQL("INSERT INTO workout_days (id, routineId, name, orderIndex) VALUES (920, 404, 'Orphan day', 0)")
        execSQL(
            """
            INSERT INTO workout_exercises (
                id, dayId, exerciseId, targetSets, repRange, restSeconds,
                target_weight_kg, target_rpe, set_type, superset_group_id, order_index
            ) VALUES (940, 404, 30, 3, '8-10', 90, 0.0, 0.0, 'WORKING', NULL, 0)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO routine_sets (
                id, workoutExerciseId, order_index, set_type, target_reps,
                target_weight_kg, rest_seconds, target_rpe, target_rir
            ) VALUES (950, 404, 0, 'NORMAL', 8, 0.0, 90, 0.0, NULL)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO performed_exercises (
                id, session_id, exercise_id, source_workout_exercise_id, order_index
            ) VALUES (970, 404, 30, NULL, 0)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO workout_sets (
                id, sessionId, exerciseId, weight, reps, rpe, repsInReserve,
                performed_exercise_id, set_type, rest_seconds, order_index,
                completed, logged_at, completed_at
            ) VALUES (980, 404, 30, 100.0, 8, 8.0, 2, 0, 'WORKING', 180, 0, 1, 1714557700000, 1714557710000)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO meal_items (
                id, meal_id, item_type, reference_id, name, grams_used,
                calories, protein, carbs, fat, notes, order_index
            ) VALUES (990, 404, 'FOOD', 200, 'Orphan meal item', 100.0, 100.0, 10.0, 10.0, 1.0, NULL, 0)
            """.trimIndent(),
        )
    }

    private fun SupportSQLiteDatabase.seedActiveWorkoutDraftState() {
        execSQL(
            """
            INSERT INTO active_workout_drafts (
                session_id, exercise_id, weight, reps, rpe, set_type
            ) VALUES (300, 30, '102.5', '6', '8.5', 'WORKING')
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO active_workout_collapsed_exercises (
                session_id, exercise_id
            ) VALUES (300, 30)
            """.trimIndent(),
        )
    }

    private fun SupportSQLiteDatabase.assertVersion11RelationalDataSurvived() {
        query("SELECT COUNT(*) FROM workout_days WHERE id = 20 AND routineId = 10").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        query("SELECT COUNT(*) FROM workout_exercises WHERE id = 40 AND dayId = 20 AND exerciseId = 30").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        query("SELECT COUNT(*) FROM workout_log_event_sets WHERE event_id = 400").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }

    private fun SupportSQLiteDatabase.assertForeignKey(tableName: String, parentTableName: String) {
        query("PRAGMA foreign_key_list(`$tableName`)").use { cursor ->
            val parentIndex = cursor.getColumnIndex("table")
            val parents = generateSequence { if (cursor.moveToNext()) cursor.getString(parentIndex) else null }
                .toSet()
            assertTrue("$tableName should reference $parentTableName", parentTableName in parents)
        }
    }

    private fun SupportSQLiteDatabase.assertNoForeignKey(tableName: String, parentTableName: String) {
        query("PRAGMA foreign_key_list(`$tableName`)").use { cursor ->
            val parentIndex = cursor.getColumnIndex("table")
            val parents = generateSequence { if (cursor.moveToNext()) cursor.getString(parentIndex) else null }
                .toSet()
            assertTrue("$tableName should not reference $parentTableName", parentTableName !in parents)
        }
    }

    private fun SupportSQLiteDatabase.assertRejectsForeignKeyViolation() {
        execSQL("PRAGMA foreign_keys=ON")
        var rejected = false
        try {
            execSQL("INSERT INTO workout_days (id, routineId, name, orderIndex) VALUES (999, 404, 'Broken', 0)")
        } catch (_: android.database.SQLException) {
            rejected = true
        }
        assertTrue("Foreign key violations should be rejected after v12 migration", rejected)
    }

    private fun SupportSQLiteDatabase.assertNoForeignKeyViolations() {
        query("PRAGMA foreign_key_check").use { cursor ->
            assertEquals("No foreign-key violations should remain after v12 migration", 0, cursor.count)
        }
    }

    private fun SupportSQLiteDatabase.assertMealItemServingCountDefaulted() {
        query("SELECT serving_count FROM meal_items WHERE id = 230").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }

    private fun SupportSQLiteDatabase.assertFoodDefaultServingGramsDefaulted() {
        query("SELECT default_serving_grams FROM food_items WHERE id = 200").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(100.0, cursor.getDouble(0), 0.0)
        }
    }

    private fun SupportSQLiteDatabase.assertActiveWorkoutDraftStateSurvived() {
        query("SELECT weight, reps, rpe, set_type FROM active_workout_drafts WHERE session_id = 300 AND exercise_id = 30").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("102.5", cursor.getString(0))
            assertEquals("6", cursor.getString(1))
            assertEquals("8.5", cursor.getString(2))
            assertEquals("WORKING", cursor.getString(3))
        }
        query("SELECT COUNT(*) FROM active_workout_collapsed_exercises WHERE session_id = 300 AND exercise_id = 30").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }

    private fun SupportSQLiteDatabase.assertVersion11OrphansRemoved() {
        listOf(
            "SELECT COUNT(*) FROM workout_days WHERE id = 920",
            "SELECT COUNT(*) FROM workout_exercises WHERE id = 940",
            "SELECT COUNT(*) FROM routine_sets WHERE id = 950",
            "SELECT COUNT(*) FROM performed_exercises WHERE id = 970",
            "SELECT COUNT(*) FROM workout_sets WHERE id = 980",
            "SELECT COUNT(*) FROM meal_items WHERE id = 990",
        ).forEach { sql ->
            query(sql).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Dirty legacy orphan should be removed: $sql", 0, cursor.getInt(0))
            }
        }
    }

    private fun SupportSQLiteDatabase.assertVersion10MirrorDataSurvived() {
        query("SELECT COUNT(*) FROM food_items WHERE id = 200 AND name = 'Greek yogurt'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        query("SELECT COUNT(*) FROM recipe_ingredients WHERE id = 220 AND recipe_id = 210").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }

    private fun SupportSQLiteDatabase.assertVersion11MetadataTableIsUsable() {
        execSQL(
            """
            INSERT INTO room_mirror_import_runs (
                generation_id, source_fingerprint, started_at, finished_at, status, schema_version,
                expected_row_count, imported_row_count, stale_row_count, mismatch_count,
                json_authoritative, room_authoritative, error_type
            ) VALUES (
                'dryrun-test-generation', 'fingerprint', 1714557600000, 1714557601000,
                'SUCCESS', 12, 3, 3, 0, 0, 1, 0, NULL
            )
            """.trimIndent(),
        )
        query("SELECT status, json_authoritative, room_authoritative FROM room_mirror_import_runs WHERE generation_id = 'dryrun-test-generation'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("SUCCESS", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals(0, cursor.getInt(2))
        }
    }

    private fun createLegacyDatabase(context: Context, dbName: String, version: Int) {
        val databaseFile = context.getDatabasePath(dbName)
        databaseFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { db ->
            db.beginTransaction()
            try {
                db.createVersion2Schema()
                db.applyLegacySchemaStepsTo(version)
                db.seedLegacyChainData(version)
                db.version = version
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    private fun SQLiteDatabase.createVersion2Schema() {
        execSQL(
            """
            CREATE TABLE user_profile (
                id INTEGER NOT NULL,
                name TEXT NOT NULL,
                height REAL NOT NULL,
                weight REAL NOT NULL,
                bodyFat REAL NOT NULL,
                activityLevel TEXT NOT NULL,
                goal TEXT NOT NULL,
                calorieTarget INTEGER NOT NULL,
                proteinTarget INTEGER NOT NULL,
                carbsTarget INTEGER NOT NULL,
                fatTarget INTEGER NOT NULL,
                trainingFocus TEXT NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        execSQL("CREATE TABLE workout_routines (id INTEGER NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, active INTEGER NOT NULL, PRIMARY KEY(id))")
        execSQL("CREATE TABLE workout_days (id INTEGER NOT NULL, routineId INTEGER NOT NULL, name TEXT NOT NULL, orderIndex INTEGER NOT NULL, PRIMARY KEY(id))")
        execSQL("CREATE TABLE exercises (id INTEGER NOT NULL, name TEXT NOT NULL, muscleGroup TEXT NOT NULL, equipment TEXT NOT NULL, PRIMARY KEY(id))")
        execSQL(
            """
            CREATE TABLE workout_exercises (
                id INTEGER NOT NULL,
                dayId INTEGER NOT NULL,
                exerciseId INTEGER NOT NULL,
                targetSets INTEGER NOT NULL,
                repRange TEXT NOT NULL,
                restSeconds INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        execSQL("CREATE TABLE workout_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, duration INTEGER NOT NULL)")
        execSQL(
            """
            CREATE TABLE workout_sets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sessionId INTEGER NOT NULL,
                exerciseId INTEGER NOT NULL,
                weight REAL NOT NULL,
                reps INTEGER NOT NULL,
                rpe REAL NOT NULL
            )
            """.trimIndent(),
        )
        execSQL(
            """
            CREATE TABLE meals (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date INTEGER NOT NULL,
                calories INTEGER NOT NULL,
                protein INTEGER NOT NULL,
                carbs INTEGER NOT NULL,
                fat INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        execSQL(
            """
            CREATE TABLE body_measurements (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date INTEGER NOT NULL,
                weight REAL NOT NULL,
                bodyFat REAL NOT NULL,
                muscleMass REAL NOT NULL
            )
            """.trimIndent(),
        )
    }

    private fun SQLiteDatabase.applyLegacySchemaStepsTo(version: Int) {
        if (version >= 3) {
            execSQL("ALTER TABLE user_profile ADD COLUMN age INTEGER NOT NULL DEFAULT 30")
            execSQL("ALTER TABLE user_profile ADD COLUMN sex TEXT NOT NULL DEFAULT 'MALE'")
            execSQL("ALTER TABLE workout_sessions ADD COLUMN caloriesBurned INTEGER NOT NULL DEFAULT 0")
            execSQL("ALTER TABLE meals ADD COLUMN mealType TEXT NOT NULL DEFAULT 'LUNCH'")
            execSQL("ALTER TABLE meals ADD COLUMN name TEXT NOT NULL DEFAULT ''")
            execSQL("ALTER TABLE meals ADD COLUMN notes TEXT")
        }
        if (version >= 4) {
            execSQL("ALTER TABLE workout_sets ADD COLUMN repsInReserve INTEGER")
            execSQL("ALTER TABLE workout_sets ADD COLUMN set_type TEXT NOT NULL DEFAULT 'WORKING'")
            execSQL("ALTER TABLE workout_exercises ADD COLUMN set_type TEXT NOT NULL DEFAULT 'WORKING'")
            execSQL("ALTER TABLE workout_exercises ADD COLUMN superset_group_id INTEGER")
            execSQL("ALTER TABLE workout_exercises ADD COLUMN order_index INTEGER NOT NULL DEFAULT 0")
        }
        if (version >= 5) {
            execSQL("ALTER TABLE workout_exercises ADD COLUMN target_weight_kg REAL NOT NULL DEFAULT 0.0")
            execSQL("ALTER TABLE workout_exercises ADD COLUMN target_rpe REAL NOT NULL DEFAULT 0.0")
        }
        if (version >= 6) {
            execSQL(
                """
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
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
        }
        if (version >= 7) {
            execSQL("ALTER TABLE workout_sessions ADD COLUMN routine_id INTEGER")
            execSQL("ALTER TABLE workout_sessions ADD COLUMN workout_day_id INTEGER")
            execSQL("ALTER TABLE workout_sessions ADD COLUMN started_at INTEGER NOT NULL DEFAULT 0")
            execSQL("ALTER TABLE workout_sessions ADD COLUMN ended_at INTEGER NOT NULL DEFAULT 0")
            execSQL("ALTER TABLE workout_sets ADD COLUMN rest_seconds INTEGER NOT NULL DEFAULT 0")
            execSQL("ALTER TABLE workout_sets ADD COLUMN order_index INTEGER NOT NULL DEFAULT 0")
            execSQL("ALTER TABLE workout_sets ADD COLUMN completed INTEGER NOT NULL DEFAULT 1")
            execSQL("ALTER TABLE workout_sets ADD COLUMN logged_at INTEGER NOT NULL DEFAULT 0")
        }
        if (version >= 8) {
            execSQL("ALTER TABLE workout_sessions ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'")
            execSQL("ALTER TABLE workout_sessions ADD COLUMN completed INTEGER NOT NULL DEFAULT 1")
            execSQL("ALTER TABLE workout_sets ADD COLUMN performed_exercise_id INTEGER NOT NULL DEFAULT 0")
            execSQL("ALTER TABLE workout_sets ADD COLUMN completed_at INTEGER NOT NULL DEFAULT 0")
            execSQL(
                """
                CREATE TABLE performed_exercises (
                    id INTEGER NOT NULL,
                    session_id INTEGER NOT NULL,
                    exercise_id INTEGER NOT NULL,
                    source_workout_exercise_id INTEGER,
                    order_index INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            execSQL("CREATE INDEX index_routine_sets_workoutExerciseId_order_index ON routine_sets(workoutExerciseId, order_index)")
            execSQL("CREATE INDEX index_workout_sessions_routine_id ON workout_sessions(routine_id)")
            execSQL("CREATE INDEX index_workout_sessions_workout_day_id ON workout_sessions(workout_day_id)")
            execSQL("CREATE INDEX index_workout_sessions_status_date ON workout_sessions(status, date)")
            execSQL("CREATE INDEX index_performed_exercises_session_id_order_index ON performed_exercises(session_id, order_index)")
            execSQL("CREATE INDEX index_performed_exercises_exercise_id ON performed_exercises(exercise_id)")
            execSQL("CREATE INDEX index_performed_exercises_source_workout_exercise_id ON performed_exercises(source_workout_exercise_id)")
            execSQL("CREATE INDEX index_workout_sets_sessionId_order_index ON workout_sets(sessionId, order_index)")
            execSQL("CREATE INDEX index_workout_sets_exerciseId ON workout_sets(exerciseId)")
            execSQL("CREATE INDEX index_workout_sets_performed_exercise_id_order_index ON workout_sets(performed_exercise_id, order_index)")
        }
    }

    private fun SQLiteDatabase.seedLegacyChainData(version: Int) {
        val profileColumns = if (version >= 3) {
            "id, name, age, sex, height, weight, bodyFat, activityLevel, goal, calorieTarget, proteinTarget, carbsTarget, fatTarget, trainingFocus"
        } else {
            "id, name, height, weight, bodyFat, activityLevel, goal, calorieTarget, proteinTarget, carbsTarget, fatTarget, trainingFocus"
        }
        val profileValues = if (version >= 3) {
            "1, 'Legacy Athlete', 35, 'MALE', 181.0, 82.5, 14.0, 'ACTIVE', 'STRENGTH', 2800, 180, 300, 80, 'Strength'"
        } else {
            "1, 'Legacy Athlete', 181.0, 82.5, 14.0, 'ACTIVE', 'STRENGTH', 2800, 180, 300, 80, 'Strength'"
        }
        execSQL("INSERT INTO user_profile ($profileColumns) VALUES ($profileValues)")
        execSQL("INSERT INTO workout_routines (id, name, description, active) VALUES (10, 'Legacy Routine', 'old split', 1)")
        execSQL("INSERT INTO workout_days (id, routineId, name, orderIndex) VALUES (20, 10, 'Legacy Day', 0)")
        execSQL("INSERT INTO exercises (id, name, muscleGroup, equipment) VALUES (30, 'Legacy Squat', 'Legs', 'Barbell')")
        execSQL("INSERT INTO workout_exercises (id, dayId, exerciseId, targetSets, repRange, restSeconds) VALUES (40, 20, 30, 5, '5', 180)")
        if (version >= 6) {
            execSQL(
                """
                INSERT INTO routine_sets (
                    id, workoutExerciseId, order_index, set_type, target_reps,
                    target_weight_kg, rest_seconds, target_rpe, target_rir
                ) VALUES (50, 40, 0, 'NORMAL', 5, 0.0, 180, 0.0, NULL)
                """.trimIndent(),
            )
        }
        execSQL("INSERT INTO workout_sessions (id, date, duration) VALUES (60, 1714557600000, 3600000)")
        if (version >= 8) {
            execSQL("INSERT INTO performed_exercises (id, session_id, exercise_id, source_workout_exercise_id, order_index) VALUES (70, 60, 30, 40, 0)")
        }
        execSQL("INSERT INTO workout_sets (id, sessionId, exerciseId, weight, reps, rpe) VALUES (80, 60, 30, 120.0, 5, 8.0)")
        execSQL("INSERT INTO meals (id, date, calories, protein, carbs, fat) VALUES (90, 1714557600000, 700, 55, 80, 20)")
        execSQL("INSERT INTO body_measurements (id, date, weight, bodyFat, muscleMass) VALUES (100, 1714557600000, 82.5, 14.0, 66.0)")
    }

    private fun SupportSQLiteDatabase.assertLegacyChainDataSurvived(startVersion: Int) {
        query("SELECT name, age, sex, trainingFocus FROM user_profile WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Legacy Athlete", cursor.getString(0))
            assertEquals(if (startVersion >= 3) 35 else 30, cursor.getInt(1))
            assertEquals("MALE", cursor.getString(2))
            assertEquals("Strength", cursor.getString(3))
        }
        query("SELECT target_weight_kg, target_rpe, set_type FROM workout_exercises WHERE id = 40").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0.0, cursor.getDouble(0), 0.0)
            assertEquals(0.0, cursor.getDouble(1), 0.0)
            assertEquals("WORKING", cursor.getString(2))
        }
        query("SELECT status, completed, debrief_source FROM workout_sessions WHERE id = 60").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("COMPLETED", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals("LOCAL_FALLBACK", cursor.getString(2))
        }
        query("SELECT performed_exercise_id, completed, completed_at FROM workout_sets WHERE id = 80").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getLong(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals(0, cursor.getLong(2))
        }
        query("SELECT COUNT(*) FROM meals WHERE id = 90 AND mealType = 'LUNCH' AND calories = 700").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }

    private fun SupportSQLiteDatabase.assertCurrentSchemaUsableAfterLegacyChain(startVersion: Int) {
        execSQL(
            """
            INSERT INTO food_items (
                id, name, barcode, calories_per_100g, protein_per_100g,
                carbs_per_100g, fat_per_100g, source_type, created_at, updated_at
            ) VALUES (${200 + startVersion}, 'Legacy yogurt $startVersion', NULL, 59.0, 10.0, 3.6, 0.4, 'MANUAL', 1714557600000, 1714557600000)
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO room_mirror_import_runs (
                generation_id, source_fingerprint, started_at, finished_at, status, schema_version,
                expected_row_count, imported_row_count, stale_row_count, mismatch_count,
                json_authoritative, room_authoritative, error_type
            ) VALUES (
                'legacy-chain-$startVersion', 'fingerprint-$startVersion', 1714557600000, 1714557601000,
                'SUCCESS', 12, 1, 1, 0, 0, 1, 0, NULL
            )
            """.trimIndent(),
        )
        query("SELECT COUNT(*) FROM food_items WHERE id = ${200 + startVersion}").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        query("SELECT room_authoritative FROM room_mirror_import_runs WHERE generation_id = 'legacy-chain-$startVersion'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    private companion object {
        const val TEST_DB = "trainiq-migration-test"
    }
}
