package com.trainiq.core.reminders

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderNotificationPrivacySourceTest {
    @Test
    fun reminderNotificationsUsePrivateVisibilityAndRedactedPublicVersion() {
        val source = Files.readString(
            Paths.get("src/main/java/com/trainiq/core/reminders/TrainIqReminderNotifications.kt"),
        )

        assertTrue(source.contains(".setVisibility(NotificationCompat.VISIBILITY_PRIVATE)"))
        assertTrue(source.contains(".setPublicVersion(publicReminderNotification(content))"))
        assertTrue(source.contains("Open TrainIQ voor je reminder."))
    }
}
