package com.marianobattaglia.floatingclock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.graphics.Point
import android.util.TypedValue
import android.view.View
import android.view.WindowInsets
import android.widget.TextView
import android.text.Layout
import kotlin.math.ceil
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.TextClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

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
    private val appearanceReceiver = AppearanceReceiver()
    private lateinit var displayManager: DisplayManager
    private var destroyed = false
    private var foregroundStarted = false
    private var use24Hour = true
    private var showClockSeconds = false
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            applyAppearance()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1, notification)
        }

        foregroundStarted = true
        synchronizeNotificationChannelNames(this)
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

        floatingClockView = OverlayTextClock(this).apply {
            textLocale = configurationLocale(resources.configuration)
            format24Hour = "HH:mm:ss"
            format12Hour = "hh:mm:ss a"
            textSize = 24f
            setTextColor(Color.WHITE)
            normalBgColor = 0x66000000
            setBackgroundColor(normalBgColor)
            setSingleLine()
        }
        applyPositionFromPrefs()
        displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
        windowManager.addView(floatingClockView, layoutParams)
        val filter = IntentFilter(AlarmSoundService.ACTION_ALARM_STATE)
        ContextCompat.registerReceiver(
            this,
            alarmStateReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val positionFilter = IntentFilter(ACTION_CLOCK_POSITION_CHANGED)
        ContextCompat.registerReceiver(
            this,
            positionReceiver,
            positionFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val secondsFilter = IntentFilter(ACTION_CLOCK_SECONDS_CHANGED)
        ContextCompat.registerReceiver(
            this,
            secondsReceiver,
            secondsFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val formatFilter = IntentFilter(ACTION_CLOCK_FORMAT_CHANGED)
        ContextCompat.registerReceiver(
            this,
            formatReceiver,
            formatFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val appearanceFilter = IntentFilter(ACTION_CLOCK_APPEARANCE_CHANGED)
        ContextCompat.registerReceiver(
            this, appearanceReceiver, appearanceFilter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        destroyed = true
        foregroundStarted = false
        displayManager.unregisterDisplayListener(displayListener)
        super.onDestroy()
        stopBlink()
        unregisterReceiver(alarmStateReceiver)
        unregisterReceiver(positionReceiver)
        unregisterReceiver(secondsReceiver)
        unregisterReceiver(formatReceiver)
        unregisterReceiver(appearanceReceiver)
        if (floatingClockView.isAttachedToWindow) windowManager.removeView(floatingClockView)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (destroyed) return
        synchronizeNotificationChannelNames(this)
        if (foregroundStarted) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1, buildNotification())
        }
        if (::floatingClockView.isInitialized) {
            floatingClockView.textLocale = configurationLocale(resources.configuration)
            // Recreate the calendar and force TextClock to refresh without replacing its window.
            floatingClockView.timeZone = null
            applyFormat(use24Hour, showClockSeconds)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CLOCK_CHANNEL_ID,
                getString(R.string.clock_notification_channel),
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

    private inner class AppearanceReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == ACTION_CLOCK_APPEARANCE_CHANGED) applyAppearance()
        }
    }

    private fun applyPositionFromPrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val positionId = prefs.getString(
            PREF_CLOCK_POSITION,
            FloatingClockPosition.TOP_LEFT.id
        )
        layoutParams.gravity = FloatingClockPosition.fromId(positionId).gravity
        applyFormat(
            prefs.getBoolean(PREF_CLOCK_USE_24H, true),
            prefs.getBoolean(PREF_CLOCK_SHOW_SECONDS, false)
        )
    }

    private fun applyPosition(position: FloatingClockPosition) {
        layoutParams.gravity = position.gravity
        applyAppearance()
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
        use24Hour = use24h
        showClockSeconds = showSeconds
        val pattern24 = clockPattern(true, showSeconds)
        val pattern12 = clockPattern(false, showSeconds)
        if (use24h) {
            floatingClockView.format24Hour = pattern24
            floatingClockView.format12Hour = pattern24
        } else {
            floatingClockView.format24Hour = pattern12
            floatingClockView.format12Hour = pattern12
        }
        applyAppearance()
    }

    @Suppress("DEPRECATION")
    private fun availableWindowSize(): ClockPixelSize {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            return ClockPixelSize(
                (metrics.bounds.width() - insets.left - insets.right).coerceAtLeast(1),
                (metrics.bounds.height() - insets.top - insets.bottom).coerceAtLeast(1)
            )
        }
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        return ClockPixelSize(size.x.coerceAtLeast(1), size.y.coerceAtLeast(1))
    }

    private fun longestTimeText(): String = widestClockText(
        configurationLocale(resources.configuration), use24Hour, showClockSeconds,
        floatingClockView.paint::measureText
    )

    private fun buildNotification(): Notification = NotificationCompat.Builder(this, CLOCK_CHANNEL_ID)
        .setContentTitle(getString(R.string.clock_notification_title))
        .setContentText(getString(R.string.clock_notification_body))
        .setSmallIcon(R.mipmap.ic_launcher)
        .setOnlyAlertOnce(true)
        .build()

    private fun applyAppearance() {
        if (destroyed || !::floatingClockView.isInitialized) return
        val appearance = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).readClockAppearance()
        floatingClockView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f * appearance.scalePercent / 100f)
        val probe = TextView(this).apply {
            setSingleLine()
            typeface = floatingClockView.typeface
            includeFontPadding = floatingClockView.includeFontPadding
            text = longestTimeText()
        }
        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val geometry = fitClockGeometry(appearance, resources.displayMetrics.density, availableWindowSize()) { candidate ->
            probe.setTextSize(TypedValue.COMPLEX_UNIT_SP, candidate.textSizeSp)
            probe.setPadding(
                candidate.horizontalPaddingPx, candidate.verticalPaddingPx,
                candidate.horizontalPaddingPx, candidate.verticalPaddingPx
            )
            probe.measure(unspecified, unspecified)
            ClockPixelSize(probe.measuredWidth, probe.measuredHeight)
        }
        floatingClockView.setTextSize(TypedValue.COMPLEX_UNIT_SP, geometry.textSizeSp)
        floatingClockView.setPadding(
            geometry.horizontalPaddingPx, geometry.verticalPaddingPx,
            geometry.horizontalPaddingPx, geometry.verticalPaddingPx
        )
        layoutParams.x = geometry.offsetX
        layoutParams.y = geometry.offsetY
        if (floatingClockView.isAttachedToWindow) {
            windowManager.updateViewLayout(floatingClockView, layoutParams)
        }
    }

    private class OverlayTextClock(context: Context) : TextClock(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            // Wrap-content roots are first offered the platform's preferred dialog
            // width. A horizontally scrolling TextView accepts that width silently.
            // Report truncation so WindowManager retries with the available width.
            val desiredWidth = ceil(Layout.getDesiredWidth(text, paint).toDouble()).toInt() +
                compoundPaddingLeft + compoundPaddingRight
            if (desiredWidth > measuredWidth) {
                setMeasuredDimension(measuredWidthAndState or MEASURED_STATE_TOO_SMALL, measuredHeightAndState)
            }
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
