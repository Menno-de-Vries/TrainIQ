package com.trainiq.features.nutrition

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Synthetic local-emulator evidence survives the connected runner's app cleanup. */
internal fun captureTask4Evidence(name: String, semantics: String) {
    require(name.matches(Regex("[a-z0-9-]+")))
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val automation = instrumentation.uiAutomation
    fun shell(command: String) = automation.executeShellCommand(command).use { descriptor ->
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }
    val output = File(instrumentation.targetContext.getExternalFilesDir(null), "task4-$name.txt")
    output.writeText(semantics)
    shell("cp ${output.absolutePath} /data/local/tmp/trainiq-task4-$name.txt")
    check(shell("cat /data/local/tmp/trainiq-task4-$name.txt").toString(Charsets.UTF_8) == semantics)
    shell("screencap -p /data/local/tmp/trainiq-task4-$name.png")
}
