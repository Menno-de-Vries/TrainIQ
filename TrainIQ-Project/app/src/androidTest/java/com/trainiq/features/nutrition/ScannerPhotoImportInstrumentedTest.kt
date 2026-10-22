package com.trainiq.features.nutrition

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.RandomAccessFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScannerPhotoImportInstrumentedTest {
    @Test fun galleryCopyAcceptsLargerPhotoAndCleansUpOversizedImport() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "gallery-boundary-${System.nanoTime()}.png")
        var imported: String? = null
        try {
            RandomAccessFile(source, "rw").use { it.setLength(7L * 1024 * 1024) }
            imported = copyScannerImageFromUri(context, Uri.fromFile(source))
            assertNotNull(imported)
            assertEquals(source.length(), File(requireNotNull(imported)).length())
            File(requireNotNull(imported)).delete()
            imported = null
            val before = context.cacheDir.listFiles().orEmpty().filter { it.name.startsWith("scanner-import-") }.map { it.name }.toSet()
            RandomAccessFile(source, "rw").use { it.setLength(32L * 1024 * 1024 + 1) }
            assertNull(copyScannerImageFromUri(context, Uri.fromFile(source)))
            assertEquals(before, context.cacheDir.listFiles().orEmpty().filter { it.name.startsWith("scanner-import-") }.map { it.name }.toSet())
        } finally { imported?.let { File(it).delete() }; source.delete() }
    }
}
