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
    private val blinkHandler = Handler(Looper.getMainLooper())
    private var blinkOn = false
    private var normalBgColor = 0
    private var isBlinking = false
    private val alarmStateReceiver = AlarmStateReceiver()

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

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

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
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBlink()
        unregisterReceiver(alarmStateReceiver)
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
