package com.example.floatingclock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class AlarmSoundService : Service() {
    private var ringtone: Ringtone? = null
    private val handler = Handler(Looper.getMainLooper())
    private var toneGenerator: ToneGenerator? = null
    private val toneHandler = Handler(Looper.getMainLooper())
    private var tonePlaying = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarm()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setContentTitle("Alarma")
            .setContentText("Sonando alarma.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ALARM_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(ALARM_NOTIFICATION_ID, notification)
        }

        startAlarm()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    private fun startAlarm() {
        if (ringtone?.isPlaying == true) return
        sendAlarmState(true)
        val alarmUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
        val rt = if (alarmUri != null) RingtoneManager.getRingtone(this, alarmUri) else null
        if (rt != null) {
            rt.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            rt.play()
            ringtone = rt
        } else {
            startToneFallback()
        }
        handler.postDelayed({ stopAlarm() }, AUTO_STOP_MS)
    }

    private fun stopAlarm() {
        handler.removeCallbacksAndMessages(null)
        ringtone?.stop()
        ringtone = null
        stopToneFallback()
        sendAlarmState(false)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Alarmas",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START = "com.example.floatingclock.ALARM_START"
        const val ACTION_STOP = "com.example.floatingclock.ALARM_STOP"
        const val ACTION_ALARM_STATE = "com.example.floatingclock.ALARM_STATE"
        const val EXTRA_ALARM_ACTIVE = "alarm_active"
        private const val ALARM_CHANNEL_ID = "AlarmChannel"
        private const val ALARM_NOTIFICATION_ID = 2
        private const val AUTO_STOP_MS = 60_000L
    }

    private fun sendAlarmState(active: Boolean) {
        val intent = Intent(ACTION_ALARM_STATE)
            .setPackage(packageName)
            .putExtra(EXTRA_ALARM_ACTIVE, active)
        sendBroadcast(intent)
    }

    private fun startToneFallback() {
        if (tonePlaying) return
        tonePlaying = true
        if (toneGenerator == null) {
            toneGenerator = ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
        }
        toneHandler.post(toneRunnable)
    }

    private fun stopToneFallback() {
        tonePlaying = false
        toneHandler.removeCallbacksAndMessages(null)
        toneGenerator?.release()
        toneGenerator = null
    }

    private val toneRunnable = object : Runnable {
        override fun run() {
            if (!tonePlaying) return
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
            toneHandler.postDelayed(this, 1000)
        }
    }
}
