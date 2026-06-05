package com.trainiq.core.security

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRulesPrivacySourceTest {
    @Test
    fun android12DataExtractionRulesExcludeRoomDatabaseFromCloudAndTransfer() {
        val source = Files.readString(Paths.get("src/main/res/xml/data_extraction_rules.xml"))

        assertEquals(2, Regex("""<exclude domain="database" path="trainiq\.db" />""").findAll(source).count())
        assertTrue(source.contains("<cloud-backup disableIfNoEncryptionCapabilities=\"true\">"))
        assertTrue(source.contains("<device-transfer>"))
    }

    @Test
    fun legacyBackupRulesExcludeRoomDatabase() {
        val source = Files.readString(Paths.get("src/main/res/xml/backup_rules.xml"))

        assertTrue(source.contains("<exclude domain=\"database\" path=\"trainiq.db\" />"))
    }
}
