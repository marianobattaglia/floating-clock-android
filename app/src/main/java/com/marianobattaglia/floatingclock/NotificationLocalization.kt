package com.marianobattaglia.floatingclock

import android.app.NotificationManager
import android.content.Context
import android.os.Build

internal const val CLOCK_CHANNEL_ID = "FloatingClockChannel"
internal const val ALARM_CHANNEL_ID = "AlarmChannel"

/** Rename existing channels without creating unused ones or altering user settings. */
internal fun synchronizeNotificationChannelNames(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java)
    for ((id, nameRes) in listOf(
        CLOCK_CHANNEL_ID to R.string.clock_notification_channel,
        ALARM_CHANNEL_ID to R.string.alarm_notification_channel
    )) {
        val channel = manager.getNotificationChannel(id) ?: continue
        val name = context.getString(nameRes)
        if (channel.name.toString() != name) {
            channel.name = name
            manager.createNotificationChannel(channel)
        }
    }
}
