package com.trainiq.features.nutrition

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class BarcodeRecognitionInstrumentedTest {
    @Test fun bundledRecognizerReadsRetailEan13WithoutNetwork() {
        // EAN-13 3017620422003, encoded explicitly as a deterministic camera-image fixture.
        val modules = "101" + "0001101" + "0011001" + "0010001" + "0000101" + "0011011" + "0001101" +
            "01010" + "1011100" + "1101100" + "1101100" + "1110010" + "1110010" + "1000010" + "101"
        val bitmap = Bitmap.createBitmap(1000, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply { color = Color.BLACK }
        modules.forEachIndexed { index, bit ->
            if (bit == '1') canvas.drawRect(120f + index * 8, 100f, 128f + index * 8, 500f, paint)
        }
        BarcodeScanning.getClient().use { scanner ->
            val results = Tasks.await(scanner.process(InputImage.fromBitmap(bitmap, 0)), 15, TimeUnit.SECONDS)
            assertEquals(listOf("3017620422003"), results.map { it.rawValue })
        }
        bitmap.recycle()
    }
}
