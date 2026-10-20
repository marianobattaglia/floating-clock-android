package com.example.floatingclock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.res.Configuration
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class NotificationLocalizationTest {
    @Test fun renameExistingChannelsPreservesTheirSettingsAndDoesNotCreateUnusedOnes() {
        if (Build.VERSION.SDK_INT < 26) return
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(NotificationManager::class.java)
        val oldClock = manager.getNotificationChannel(CLOCK_CHANNEL_ID)
        val oldAlarm = manager.getNotificationChannel(ALARM_CHANNEL_ID)
        val french = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale.FRENCH)
        })
        try {
            synchronizeNotificationChannelNames(french)
            if (oldClock == null) assertNull(manager.getNotificationChannel(CLOCK_CHANNEL_ID))
            if (oldAlarm == null) assertNull(manager.getNotificationChannel(ALARM_CHANNEL_ID))
            if (oldClock == null) {
                val clock = NotificationChannel(CLOCK_CHANNEL_ID, "Old clock label", NotificationManager.IMPORTANCE_LOW)
                clock.setSound(null, null)
                manager.createNotificationChannel(clock)
            }
            val before = manager.getNotificationChannel(CLOCK_CHANNEL_ID)
            synchronizeNotificationChannelNames(french)
            val after = manager.getNotificationChannel(CLOCK_CHANNEL_ID)
            assertEquals("Service de l’horloge flottante", after.name.toString())
            assertEquals(before.importance, after.importance)
            assertEquals(before.sound, after.sound)
            assertEquals(before.vibrationPattern?.toList(), after.vibrationPattern?.toList())
            assertEquals(before.id, after.id)
            if (oldAlarm == null) assertNull(manager.getNotificationChannel(ALARM_CHANNEL_ID))
        } finally {
            if (oldClock == null) manager.deleteNotificationChannel(CLOCK_CHANNEL_ID)
            else manager.createNotificationChannel(oldClock)
            if (oldAlarm != null) manager.createNotificationChannel(oldAlarm)
        }
    }
}
