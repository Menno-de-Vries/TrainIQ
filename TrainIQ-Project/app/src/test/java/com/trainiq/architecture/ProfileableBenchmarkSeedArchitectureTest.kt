package com.trainiq.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileableBenchmarkSeedArchitectureTest {
    @Test
    fun profileableBenchmarkSeedActivityIsProfileableOnlyAndNotInMainManifest() {
        val profileableActivity = File("src/profileable/java/com/trainiq/benchmark/BenchmarkSeedActivity.kt")
        val profileableManifest = File("src/profileable/AndroidManifest.xml")
        val mainManifest = File("src/main/AndroidManifest.xml").readText()
        val macrobenchmarkSource = File("../macrobenchmark/src/main/java/com/trainiq/macrobenchmark/TrainIqStartupBenchmark.java").readText()

        assertTrue("Benchmark seed activity must live only in the profileable source set", profileableActivity.exists())
        assertTrue("Benchmark seed activity must be declared only in the profileable manifest", profileableManifest.exists())

        val activitySource = profileableActivity.readText()
        val manifestSource = profileableManifest.readText()

        assertTrue(activitySource.contains("package com.trainiq.benchmark"))
        assertTrue(activitySource.contains("class BenchmarkSeedActivity"))
        assertTrue(activitySource.contains("clearMirrorTables()"))
        assertTrue(activitySource.contains("startOrResumeActiveWorkoutSession("))
        assertTrue(manifestSource.contains("com.trainiq.benchmark.BenchmarkSeedActivity"))
        assertTrue("Benchmark seed activity must only be exported from the profileable-only manifest for macrobenchmark shell startup", manifestSource.contains("android:exported=\"true\""))
        assertTrue(manifestSource.contains("android:excludeFromRecents=\"true\""))
        assertTrue(manifestSource.contains("android:noHistory=\"true\""))
        assertTrue(macrobenchmarkSource.contains("BenchmarkSeedActivity"))
        assertTrue(macrobenchmarkSource.contains("activeWorkoutLoggingFrames"))
        assertFalse("Benchmark seed activity must not ship from main manifest", mainManifest.contains("BenchmarkSeedActivity"))
    }

    @Test
    fun topLevelFrameBenchmarkSeedsCompletedOnboardingBeforeNavigation() {
        val macrobenchmarkSource = File("../macrobenchmark/src/main/java/com/trainiq/macrobenchmark/TrainIqStartupBenchmark.java").readText()
        val benchmarkBody = macrobenchmarkSource
            .substringAfter("public void topLevelNavigationAndSettingsScrollFrames()")
            .substringBefore("public void activeWorkoutLoggingFrames()")
        val baselineProfileBody = macrobenchmarkSource
            .substringAfter("public void generateBaselineProfileForCriticalJourneys()")
            .substringBefore("public void coldStartupWithRequiredBaselineProfile()")

        assertTrue(benchmarkBody.contains("seedActiveWorkout(scope);"))
        assertTrue(benchmarkBody.indexOf("seedActiveWorkout(scope);") < benchmarkBody.indexOf("startTrainIqMainActivity(scope);"))
        assertTrue(baselineProfileBody.contains("seedActiveWorkout(scope);"))
    }
}
