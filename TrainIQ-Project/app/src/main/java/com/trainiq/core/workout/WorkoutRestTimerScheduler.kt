package com.trainiq.core.workout

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.trainiq.domain.workout.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidWorkoutRestTimerClock @Inject constructor(@ApplicationContext context: Context) : WorkoutRestTimerClock {
    private val bootId = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
        .takeIf { it >= 0 }?.toString().orEmpty()
    override fun now() = WorkoutRestTimerNow(System.currentTimeMillis(), SystemClock.elapsedRealtime(), bootId)
}

@Singleton
class WorkoutRestTimerScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notifications: WorkoutRestTimerNotifications,
) : WorkoutRestTimerDelivery {
    private val alarms get() = context.getSystemService(AlarmManager::class.java)
    fun exactAllowed() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()

    @SuppressLint("ScheduleExactAlarm")
    override fun schedule(timer: ScheduledWorkoutRestTimer) {
        val intent = pending(timer.identity)
        try {
            if (exactAllowed()) alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, timer.elapsedDeadline, intent)
            else alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, timer.elapsedDeadline, intent)
        } catch (_: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, timer.elapsedDeadline, intent)
        }
    }

    override fun cancel(identity: WorkoutRestTimerIdentity) {
        val intent = pending(identity)
        alarms.cancel(intent)
        intent.cancel()
        notifications.cancel(identity)
    }

    override suspend fun show(timer: ScheduledWorkoutRestTimer, silent: Boolean) = notifications.show(timer, silent)

    private fun pending(identity: WorkoutRestTimerIdentity) = PendingIntent.getBroadcast(context, RestNotificationId,
        Intent(context, WorkoutRestTimerReceiver::class.java).setAction(RestTimerAction)
            .setData(Uri.parse("trainiq-rest://${identity.sessionId}/${identity.generation}"))
            .putExtra(RestSessionExtra, identity.sessionId).putExtra(RestGenerationExtra, identity.generation),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

internal const val RestTimerAction = "com.trainiq.WORKOUT_REST_TIMER"
const val RestSessionExtra = "workout_rest_session"
const val RestGenerationExtra = "workout_rest_generation"
