package com.example.floatingclock

import android.content.res.Configuration
import androidx.core.os.ConfigurationCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal fun configurationLocale(configuration: Configuration): Locale =
    ConfigurationCompat.getLocales(configuration)[0] ?: Locale.ENGLISH

internal fun clockPattern(use24Hour: Boolean, showSeconds: Boolean): String = when {
    use24Hour && showSeconds -> "HH:mm:ss"
    use24Hour -> "HH:mm"
    showSeconds -> "hh:mm:ss a"
    else -> "hh:mm a"
}

// Measure all valid field values, rather than just the current tick. The locale must
// be the same as the clock's current configuration, including localized AM/PM text.
internal fun widestClockText(
    locale: Locale,
    use24Hour: Boolean,
    showSeconds: Boolean,
    measureText: (String) -> Float
): String {
    val calendar = Calendar.getInstance(locale).apply { set(Calendar.MILLISECOND, 0) }
    fun widest(pattern: String, field: Int, values: IntRange): String {
        val formatter = SimpleDateFormat(pattern, locale)
        return values.map { value ->
            calendar.set(field, value)
            formatter.format(calendar.time)
        }.maxBy(measureText)
    }
    val hours = widest(if (use24Hour) "HH" else "hh", Calendar.HOUR_OF_DAY, 0..23)
    val minutes = widest("mm", Calendar.MINUTE, 0..59)
    val seconds = if (showSeconds) ":" + widest("ss", Calendar.SECOND, 0..59) else ""
    val period = if (use24Hour) "" else " " + widest("a", Calendar.HOUR_OF_DAY, 0..23)
    return "$hours:$minutes$seconds$period"
}
