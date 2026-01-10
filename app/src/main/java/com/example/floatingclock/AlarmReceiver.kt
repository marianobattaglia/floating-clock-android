package com.example.floatingclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

const val ACTION_ALARM_TRIGGER = "com.example.floatingclock.ALARM_TRIGGER"

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_ALARM_TRIGGER) return
        val serviceIntent = Intent(context, AlarmSoundService::class.java)
            .setAction(AlarmSoundService.ACTION_START)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}

