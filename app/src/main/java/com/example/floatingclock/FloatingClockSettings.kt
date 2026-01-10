package com.example.floatingclock

import android.view.Gravity

const val PREFS_NAME = "floating_clock_prefs"
const val PREF_FLOATING_ENABLED = "floating_clock_enabled"
const val PREF_ALARM_ENABLED = "alarm_enabled"
const val PREF_ALARM_HOUR = "alarm_hour"
const val PREF_ALARM_MINUTE = "alarm_minute"
const val PREF_CLOCK_POSITION = "clock_position"

const val ACTION_CLOCK_POSITION_CHANGED = "com.example.floatingclock.CLOCK_POSITION_CHANGED"
const val EXTRA_CLOCK_POSITION = "clock_position"

enum class FloatingClockPosition(val id: String, val label: String, val gravity: Int) {
    TOP_LEFT("top_left", "Top left", Gravity.TOP or Gravity.START),
    TOP_RIGHT("top_right", "Top right", Gravity.TOP or Gravity.END),
    BOTTOM_LEFT("bottom_left", "Bottom left", Gravity.BOTTOM or Gravity.START),
    BOTTOM_RIGHT("bottom_right", "Bottom right", Gravity.BOTTOM or Gravity.END);

    companion object {
        fun fromId(id: String?): FloatingClockPosition {
            return entries.firstOrNull { it.id == id } ?: TOP_LEFT
        }
    }
}
