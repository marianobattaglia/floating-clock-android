package com.marianobattaglia.floatingclock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil

class ClockAppearanceTest {
    @Test
    fun malformedSettingsFallBackIndependentlyWithoutRewritingInput() {
        val stored = mapOf(
            PREF_CLOCK_EDGE_DISTANCE_DP to "24",
            PREF_CLOCK_SCALE_PERCENT to 150,
            PREF_CLOCK_PADDING_PERCENT to false
        )
        assertEquals(ClockAppearance(16, 150, 100), ClockAppearance.from(stored))
        assertEquals("24", stored[PREF_CLOCK_EDGE_DISTANCE_DP])
        assertEquals(ClockAppearance(), ClockAppearance.from(emptyMap<String, Any>()))
        assertEquals(
            ClockAppearance(0, 200, 100),
            ClockAppearance.from(mapOf(
                PREF_CLOCK_EDGE_DISTANCE_DP to Int.MIN_VALUE,
                PREF_CLOCK_SCALE_PERCENT to Int.MAX_VALUE,
                PREF_CLOCK_PADDING_PERCENT to 100L
            ))
        )
    }

    @Test
    fun stepsSaturateAtBothBoundsWithoutWrapping() {
        for (range in listOf(
            ClockAppearanceLimits.edgeDistance,
            ClockAppearanceLimits.overallSize,
            ClockAppearanceLimits.internalSpacing
        )) {
            assertEquals(range.min, range.adjust(Int.MIN_VALUE, false))
            assertEquals(range.max, range.adjust(Int.MAX_VALUE, true))
            assertEquals(range.default + range.step, range.adjust(range.default, true))
            assertEquals(range.default - range.step, range.adjust(range.default, false))
            var value = range.default
            repeat(1000) { value = range.adjust(value, true) }
            assertEquals(range.max, value)
            repeat(1000) { value = range.adjust(value, false) }
            assertEquals(range.min, value)
        }
    }

    @Test
    fun defaultGeometryPreservesLegacyUnitsAndScalesTogether() {
        val default = clockGeometry(ClockAppearance(), 1f)
        assertEquals(24f, default.textSizeSp, 0f)
        assertEquals(24, default.horizontalPaddingPx)
        assertEquals(16, default.verticalPaddingPx)
        val scaled = clockGeometry(ClockAppearance(), 1.5f)
        assertEquals(36f, scaled.textSizeSp, 0f)
        assertEquals(36, scaled.horizontalPaddingPx)
        assertEquals(24, scaled.verticalPaddingPx)
        val spaced = clockGeometry(ClockAppearance(paddingPercent = 200), 1.5f)
        assertEquals(scaled.textSizeSp, spaced.textSizeSp, 0f)
        assertEquals(72, spaced.horizontalPaddingPx)
        assertEquals(48, spaced.verticalPaddingPx)
        val zero = clockGeometry(ClockAppearance(paddingPercent = 0), 0.5f)
        assertEquals(12f, zero.textSizeSp, 0f)
        assertEquals(0, zero.horizontalPaddingPx)
        assertEquals(0, zero.verticalPaddingPx)
    }

    private fun measured(geometry: ClockGeometry, textWidth: Float = 200f): ClockPixelSize {
        val scale = geometry.textSizeSp / 24f
        return ClockPixelSize(
            ceil(textWidth * scale).toInt() + 2 * geometry.horizontalPaddingPx,
            ceil(32f * scale).toInt() + 2 * geometry.verticalPaddingPx
        )
    }

    @Test
    fun edgeDistanceUsesDensityAndClampsEachAxisIndependently() {
        val geometry = fitClockGeometry(ClockAppearance(edgeDistanceDp = 128), 2f, ClockPixelSize(300, 100)) {
            measured(it)
        }
        assertEquals(52, geometry.offsetX)
        assertEquals(36, geometry.offsetY)
        assertEquals(24f, geometry.textSizeSp, 0f)
        val default = fitClockGeometry(ClockAppearance(), 1.5f, ClockPixelSize(1920, 1080)) { measured(it) }
        assertEquals(24, default.offsetX)
        assertEquals(24, default.offsetY)
    }

    @Test
    fun fittingHandlesPixelRoundingAndFormatExpansionWithoutChangingRequestedValues() {
        val requested = ClockAppearance(128, 200, 300)
        for (screen in listOf(ClockPixelSize(80, 30), ClockPixelSize(300, 20), ClockPixelSize(1, 1))) {
            val fitted = fitClockGeometry(requested, 3f, screen) { measured(it, 400f) }
            val size = measured(fitted, 400f)
            assertTrue(size.width + fitted.offsetX <= screen.width)
            assertTrue(size.height + fitted.offsetY <= screen.height)
            assertTrue(fitted.offsetX >= 0 && fitted.offsetY >= 0)
            assertTrue(fitted.textSizeSp < 48f)
        }
        val screen = ClockPixelSize(700, 200)
        val shortFormat = fitClockGeometry(requested, 1f, screen) { measured(it) }
        val longFormat = fitClockGeometry(requested, 1f, screen) { measured(it, 400f) }
        assertTrue(longFormat.textSizeSp < shortFormat.textSizeSp)
        val restored = fitClockGeometry(requested, 1f, ClockPixelSize(4000, 2000)) { measured(it, 400f) }
        assertEquals(48f, restored.textSizeSp, 0f)
        assertEquals(ClockAppearance(128, 200, 300), requested)
    }
}
