package com.marianobattaglia.floatingclock

import android.content.SharedPreferences
import android.view.Gravity

const val PREFS_NAME = "floating_clock_prefs"
const val PREF_FLOATING_ENABLED = "floating_clock_enabled"
const val PREF_ALARM_ENABLED = "alarm_enabled"
const val PREF_ALARM_HOUR = "alarm_hour"
const val PREF_ALARM_MINUTE = "alarm_minute"
const val PREF_CLOCK_POSITION = "clock_position"
const val PREF_CLOCK_SHOW_SECONDS = "clock_show_seconds"
const val PREF_CLOCK_USE_24H = "clock_use_24h"

const val ACTION_CLOCK_POSITION_CHANGED = "com.marianobattaglia.floatingclock.CLOCK_POSITION_CHANGED"
const val EXTRA_CLOCK_POSITION = "clock_position"
const val ACTION_CLOCK_SECONDS_CHANGED = "com.marianobattaglia.floatingclock.CLOCK_SECONDS_CHANGED"
const val EXTRA_CLOCK_SHOW_SECONDS = "clock_show_seconds"
const val ACTION_CLOCK_FORMAT_CHANGED = "com.marianobattaglia.floatingclock.CLOCK_FORMAT_CHANGED"
const val EXTRA_CLOCK_USE_24H = "clock_use_24h"

enum class FloatingClockPosition(val id: String, val labelRes: Int, val gravity: Int) {
    TOP_LEFT("top_left", R.string.clock_position_top_left, Gravity.TOP or Gravity.START),
    TOP_RIGHT("top_right", R.string.clock_position_top_right, Gravity.TOP or Gravity.END),
    BOTTOM_LEFT("bottom_left", R.string.clock_position_bottom_left, Gravity.BOTTOM or Gravity.START),
    BOTTOM_RIGHT("bottom_right", R.string.clock_position_bottom_right, Gravity.BOTTOM or Gravity.END);

    companion object {
        fun fromId(id: String?): FloatingClockPosition {
            return entries.firstOrNull { it.id == id } ?: TOP_LEFT
        }
    }
}

const val PREF_CLOCK_EDGE_DISTANCE_DP = "clock_edge_distance_dp"
const val PREF_CLOCK_SCALE_PERCENT = "clock_scale_percent"
const val PREF_CLOCK_PADDING_PERCENT = "clock_padding_percent"
const val PREF_PRO_STATUS_CONFIRMED = "pro_status_confirmed"
const val PREF_PRO_PURCHASED = "pro_purchased"
const val PREF_DEBUG_PRO_OVERRIDE = "debug_pro_override"
const val PREF_REVIEW_ACCESS_GRANTED_HASH = "review_access_granted_hash"
const val PREF_FREE_ALARM_LAST_SUCCESS_MS = "free_alarm_last_success_ms"
const val ACTION_CLOCK_APPEARANCE_CHANGED = "com.marianobattaglia.floatingclock.CLOCK_APPEARANCE_CHANGED"

// Read independently: a malformed preference must not discard the other settings.
fun SharedPreferences.readClockAppearance(): ClockAppearance = ClockAppearance.from(all)

object ClockAppearanceLimits {
    val edgeDistance = AppearanceRange(0, 128, 4, 16)
    val overallSize = AppearanceRange(50, 200, 10, 100)
    val internalSpacing = AppearanceRange(0, 300, 10, 100)
}

data class AppearanceRange(val min: Int, val max: Int, val step: Int, val default: Int) {
    fun normalize(value: Any?): Int = (value as? Int ?: default).coerceIn(min, max)

    fun adjust(value: Int, increase: Boolean): Int {
        val bounded = value.coerceIn(min, max)
        return (bounded + if (increase) step else -step).coerceIn(min, max)
    }
}

data class ClockAppearance(
    val edgeDistanceDp: Int = ClockAppearanceLimits.edgeDistance.default,
    val scalePercent: Int = ClockAppearanceLimits.overallSize.default,
    val paddingPercent: Int = ClockAppearanceLimits.internalSpacing.default
) {
    companion object {
        fun from(values: Map<String, *>): ClockAppearance = ClockAppearance(
            ClockAppearanceLimits.edgeDistance.normalize(values[PREF_CLOCK_EDGE_DISTANCE_DP]),
            ClockAppearanceLimits.overallSize.normalize(values[PREF_CLOCK_SCALE_PERCENT]),
            ClockAppearanceLimits.internalSpacing.normalize(values[PREF_CLOCK_PADDING_PERCENT])
        )
    }
}
