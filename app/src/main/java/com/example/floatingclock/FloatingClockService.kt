package com.example.floatingclock

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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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

        floatingClockView = OverlayTextClock(this).apply {
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

        val appearanceFilter = IntentFilter(ACTION_CLOCK_APPEARANCE_CHANGED)
        ContextCompat.registerReceiver(
            this, appearanceReceiver, appearanceFilter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        destroyed = true
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
        applyAppearance()
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
        val pattern24 = if (showSeconds) "HH:mm:ss" else "HH:mm"
        val pattern12 = if (showSeconds) "hh:mm:ss a" else "hh:mm a"
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

    // Choose the widest valid field values, including localized AM/PM, rather than
    // fitting only the current tick (which could clip at the next minute/hour).
    private fun longestTimeText(): String {
        val locale = Locale.getDefault()
        val calendar = Calendar.getInstance().apply { set(Calendar.MILLISECOND, 0) }
        fun widest(pattern: String, field: Int, values: IntRange): String {
            val formatter = SimpleDateFormat(pattern, locale)
            return values.map { value ->
                calendar.set(field, value)
                formatter.format(calendar.time)
            }.maxBy { floatingClockView.paint.measureText(it) }
        }
        val hours = widest(if (use24Hour) "HH" else "hh", Calendar.HOUR_OF_DAY, 0..23)
        val minutes = widest("mm", Calendar.MINUTE, 0..59)
        val seconds = if (showClockSeconds) ":" + widest("ss", Calendar.SECOND, 0..59) else ""
        val period = if (use24Hour) "" else " " + widest("a", Calendar.HOUR_OF_DAY, 0..23)
        return "$hours:$minutes$seconds$period"
    }

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
