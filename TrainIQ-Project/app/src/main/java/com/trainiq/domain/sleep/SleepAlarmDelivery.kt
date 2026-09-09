package com.trainiq.domain.sleep

/** OS side effects are separate from durable routine state. */
interface SleepAlarmDelivery {
    fun schedule(state: SleepRoutine)
    fun showReminder(escalated: Boolean)
    fun showCountdown(state: SleepRoutine)
    fun cancelNotification()
}
