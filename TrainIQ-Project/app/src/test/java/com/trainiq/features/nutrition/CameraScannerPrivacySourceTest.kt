package com.trainiq.features.nutrition

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraScannerPrivacySourceTest {
    @Test
    fun cameraCaptureFailureLogDoesNotWriteThrowablePayload() {
        val source = Files.readString(Paths.get("src/main/java/com/trainiq/features/nutrition/CameraScannerScreen.kt"))

        assertFalse(source.contains("Log.e(\"TrainIQ\", \"Camera capture failed\", exception)"))
        assertFalse(source.contains("android.util.Log.e(\"TrainIQ\", \"Camera capture failed\", exception)"))
        assertTrue(source.contains("if (!BuildConfig.DEBUG) return"))
        assertTrue(source.contains("Log.w(ScannerLogTag, \"Camera capture failed"))
    }

    @Test
    fun scannerTempImagesAreDeletedAfterAnalysisPaths() {
        val source = Files.readString(Paths.get("src/main/java/com/trainiq/features/nutrition/CameraScannerScreen.kt"))
        val analyzeBody = source.substringAfter("fun analyze(path: String, scannerMode: ScannerMode)").substringBefore("fun resetToPreview")

        assertTrue(analyzeBody.contains("deleteScannerTemporaryImage(path)"))
        assertTrue(analyzeBody.contains("finally"))
    }

    @Test
    fun deleteScannerTemporaryImageOnlyDeletesKnownScannerCacheFiles() {
        val tempDir = Files.createTempDirectory("trainiq-scanner-cleanup").toFile()
        val scannerFile = tempDir.resolve("scanner-import-123.jpg").apply { writeText("image") }
        val mealFile = tempDir.resolve("meal-fullscreen-123.jpg").apply { writeText("image") }
        val otherFile = tempDir.resolve("profile-photo.jpg").apply { writeText("image") }

        assertTrue(deleteScannerTemporaryImage(scannerFile.absolutePath))
        assertTrue(deleteScannerTemporaryImage(mealFile.absolutePath))
        assertFalse(deleteScannerTemporaryImage(otherFile.absolutePath))
        assertFalse(scannerFile.exists())
        assertFalse(mealFile.exists())
        assertTrue(otherFile.exists())
    }
}
