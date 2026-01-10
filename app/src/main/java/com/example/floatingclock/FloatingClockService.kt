package com.example.floatingclock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextClock
import androidx.core.app.NotificationCompat

class FloatingClockService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingClockView: TextClock
    private lateinit var layoutParams: WindowManager.LayoutParams
    private val blinkHandler = Handler(Looper.getMainLooper())
    private var blinkOn = false
    private var normalBgColor = 0
    private var isBlinking = false
    private val alarmStateReceiver = AlarmStateReceiver()
    private val positionReceiver = PositionReceiver()
    private val secondsReceiver = SecondsReceiver()
    private val formatReceiver = FormatReceiver()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "FloatingClockChannel")
            .setContentTitle("Floating Clock")
            .setContentText("Floating clock is running.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1, notification)
        }

        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        applyPositionFromPrefs()

        floatingClockView = TextClock(this).apply {
            format24Hour = "HH:mm:ss"
            format12Hour = "hh:mm:ss a"
            textSize = 24f
            setTextColor(Color.WHITE)
            normalBgColor = 0x66000000
            setBackgroundColor(normalBgColor)
            setPadding(24, 16, 24, 16)
        }
        windowManager.addView(floatingClockView, layoutParams)
        val filter = IntentFilter(AlarmSoundService.ACTION_ALARM_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(alarmStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(alarmStateReceiver, filter)
        }

        val positionFilter = IntentFilter(ACTION_CLOCK_POSITION_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(positionReceiver, positionFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(positionReceiver, positionFilter)
        }

        val secondsFilter = IntentFilter(ACTION_CLOCK_SECONDS_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(secondsReceiver, secondsFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(secondsReceiver, secondsFilter)
        }

        val formatFilter = IntentFilter(ACTION_CLOCK_FORMAT_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(formatReceiver, formatFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(formatReceiver, formatFilter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBlink()
        unregisterReceiver(alarmStateReceiver)
        unregisterReceiver(positionReceiver)
        unregisterReceiver(secondsReceiver)
        unregisterReceiver(formatReceiver)
        windowManager.removeView(floatingClockView)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "FloatingClockChannel",
                "Floating Clock Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private inner class AlarmStateReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action != AlarmSoundService.ACTION_ALARM_STATE) return
            val active = intent.getBooleanExtra(AlarmSoundService.EXTRA_ALARM_ACTIVE, false)
            if (active) startBlink() else stopBlink()
        }
    }

    private inner class PositionReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action != ACTION_CLOCK_POSITION_CHANGED) return
            val id = intent.getStringExtra(EXTRA_CLOCK_POSITION)
            applyPosition(FloatingClockPosition.fromId(id))
        }
    }

    private inner class SecondsReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action != ACTION_CLOCK_SECONDS_CHANGED) return
            val showSeconds = intent.getBooleanExtra(EXTRA_CLOCK_SHOW_SECONDS, false)
            applySeconds(showSeconds)
        }
    }

    private inner class FormatReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action != ACTION_CLOCK_FORMAT_CHANGED) return
            val use24h = intent.getBooleanExtra(EXTRA_CLOCK_USE_24H, false)
            applyFormat(use24h)
        }
    }

    private fun applyPositionFromPrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val positionId = prefs.getString(
            PREF_CLOCK_POSITION,
            FloatingClockPosition.TOP_LEFT.id
        )
        applyPosition(FloatingClockPosition.fromId(positionId))
        val showSeconds = prefs.getBoolean(PREF_CLOCK_SHOW_SECONDS, false)
        applySeconds(showSeconds)
        val use24h = prefs.getBoolean(PREF_CLOCK_USE_24H, true)
        applyFormat(use24h)
    }

    private fun applyPosition(position: FloatingClockPosition) {
        layoutParams.gravity = position.gravity
        val offset = (resources.displayMetrics.density * 16).toInt()
        layoutParams.x = offset
        layoutParams.y = offset
        if (::floatingClockView.isInitialized) {
            windowManager.updateViewLayout(floatingClockView, layoutParams)
        }
    }

    private fun applySeconds(showSeconds: Boolean) {
        if (!::floatingClockView.isInitialized) return
        val use24h = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(PREF_CLOCK_USE_24H, true)
        applyFormat(use24h, showSeconds)
    }

    private fun applyFormat(use24h: Boolean) {
        val showSeconds = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(PREF_CLOCK_SHOW_SECONDS, false)
        applyFormat(use24h, showSeconds)
    }

    private fun applyFormat(use24h: Boolean, showSeconds: Boolean) {
        if (!::floatingClockView.isInitialized) return
        val pattern24 = if (showSeconds) "HH:mm:ss" else "HH:mm"
        val pattern12 = if (showSeconds) "hh:mm:ss a" else "hh:mm a"
        if (use24h) {
            floatingClockView.format24Hour = pattern24
            floatingClockView.format12Hour = pattern24
        } else {
            floatingClockView.format24Hour = pattern12
            floatingClockView.format12Hour = pattern12
        }
    }

    private fun startBlink() {
        if (isBlinking) return
        isBlinking = true
        blinkOn = false
        blinkHandler.post(blinkRunnable)
    }

    private fun stopBlink() {
        isBlinking = false
        blinkHandler.removeCallbacksAndMessages(null)
        floatingClockView.setBackgroundColor(normalBgColor)
    }

    private val blinkRunnable = object : Runnable {
        override fun run() {
            if (!isBlinking) return
            blinkOn = !blinkOn
            val color = if (blinkOn) Color.RED else normalBgColor
            floatingClockView.setBackgroundColor(color)
            blinkHandler.postDelayed(this, 500)
        }
    }
}
