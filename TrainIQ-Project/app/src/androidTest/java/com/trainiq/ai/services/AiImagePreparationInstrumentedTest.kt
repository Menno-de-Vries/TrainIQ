package com.trainiq.ai.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiImagePreparationInstrumentedTest {
    @Test
    fun photoAboveSixMiB_isCompressedRatherThanRejected() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sourceFile = File(context.cacheDir, "large-ai-source-${System.nanoTime()}.png")
        val source = Bitmap.createBitmap(1_600, 1_600, Bitmap.Config.ARGB_8888)
        try {
            val random = java.util.Random(42)
            val pixels = IntArray(1_600 * 1_600) { random.nextInt() or 0xff000000.toInt() }
            source.setPixels(pixels, 0, 1_600, 0, 0, 1_600, 1_600)
            FileOutputStream(sourceFile).use { assertTrue(source.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            assertTrue(sourceFile.length() > 6L * 1024 * 1024)
            val prepared = prepareMealScanImageBytes(sourceFile)
            assertNotNull(prepared)
            val decoded = BitmapFactory.decodeByteArray(prepared, 0, prepared!!.size)
            try {
                assertTrue(maxOf(decoded.width, decoded.height) <= 1_280)
                assertTrue(prepared.size <= 1_500_000)
            } finally { decoded.recycle() }
        } finally { source.recycle(); sourceFile.delete() }
    }

    @Test
    fun largeDimensionJpeg_isSampledAndPreparedWithinUploadBounds() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sourceFile = File(context.cacheDir, "large-ai-image-${System.nanoTime()}.jpg")
        val source = Bitmap.createBitmap(4_096, 2_048, Bitmap.Config.ARGB_8888)
        try {
            source.eraseColor(0xff6a8caf.toInt())
            FileOutputStream(sourceFile).use { output ->
                assertTrue(source.compress(Bitmap.CompressFormat.JPEG, 95, output))
            }

            val prepared = prepareMealScanImageBytes(sourceFile)
            assertNotNull(prepared)
            val decoded = BitmapFactory.decodeByteArray(prepared, 0, prepared!!.size)
            assertNotNull(decoded)
            try {
                assertTrue(maxOf(decoded.width, decoded.height) <= 1_280)
                assertTrue(prepared.size <= 1_500_000)
            } finally {
                decoded.recycle()
            }
        } finally {
            source.recycle()
            sourceFile.delete()
        }
    }
}
