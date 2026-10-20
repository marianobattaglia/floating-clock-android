package com.example.floatingclock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LocaleFormattingTest {
    @Test fun timePatternsRetainExplicitFormatAndSecondsChoices() {
        assertEquals("HH:mm", clockPattern(true, false))
        assertEquals("HH:mm:ss", clockPattern(true, true))
        assertEquals("hh:mm a", clockPattern(false, false))
        assertEquals("hh:mm:ss a", clockPattern(false, true))
    }

    @Test fun widestProbeIncludesLocalizedPeriodsAndFitsWithoutRewritingPreferences() {
        val requested = ClockAppearance(128, 200, 300)
        for (tag in listOf("en-US", "es-AR", "pt-BR", "pt-PT", "fr-CA", "de-DE")) {
            val locale = Locale.forLanguageTag(tag)
            val probe = widestClockText(locale, false, true) { it.length.toFloat() }
            val formatter = SimpleDateFormat(clockPattern(false, true), locale)
            val calendar = Calendar.getInstance(locale)
            for (hour in 0..23) {
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                assertTrue(tag, probe.length >= formatter.format(calendar.time).length)
            }
            val fitted = fitClockGeometry(requested, 1f, ClockPixelSize(260, 90)) {
                ClockPixelSize((probe.length * it.textSizeSp * 0.6f).toInt() + it.horizontalPaddingPx * 2,
                    it.textSizeSp.toInt() + it.verticalPaddingPx * 2)
            }
            assertTrue(fitted.textSizeSp <= 48f)
            assertEquals(ClockAppearance(128, 200, 300), requested)
        }
    }

    @Test fun persistedPositionIdentifiersAreIndependentOfLocalizedLabels() {
        assertEquals(listOf("top_left", "top_right", "bottom_left", "bottom_right"), FloatingClockPosition.entries.map { it.id })
        FloatingClockPosition.entries.forEach { assertEquals(it, FloatingClockPosition.fromId(it.id)) }
        assertEquals(FloatingClockPosition.TOP_LEFT, FloatingClockPosition.fromId("unknown"))
    }
}
