package com.marianobattaglia.floatingclock

import kotlin.math.roundToInt

internal data class ClockPixelSize(val width: Int, val height: Int)

internal data class ClockGeometry(
    val textSizeSp: Float,
    val horizontalPaddingPx: Int,
    val verticalPaddingPx: Int,
    val offsetX: Int = 0,
    val offsetY: Int = 0
)

internal fun clockGeometry(appearance: ClockAppearance, scale: Float): ClockGeometry {
    val paddingScale = appearance.paddingPercent / 100f * scale
    return ClockGeometry(
        textSizeSp = 24f * scale,
        horizontalPaddingPx = (24f * paddingScale).roundToInt(),
        verticalPaddingPx = (16f * paddingScale).roundToInt()
    )
}

// The caller measures the longest time using the real font, density and font scale.
// Never mutate requested preferences when the current screen requires a smaller clock.
internal fun fitClockGeometry(
    appearance: ClockAppearance,
    density: Float,
    available: ClockPixelSize,
    measure: (ClockGeometry) -> ClockPixelSize
): ClockGeometry {
    val requestedScale = appearance.scalePercent / 100f
    fun fits(size: ClockPixelSize) = size.width <= available.width && size.height <= available.height

    var geometry = clockGeometry(appearance, requestedScale)
    var size = measure(geometry)
    if (!fits(size)) {
        var low = 0f
        var high = requestedScale
        // Re-measure rounded padding/font metrics, rather than assuming linear pixel sizes.
        repeat(24) {
            val candidate = (low + high) / 2f
            if (fits(measure(clockGeometry(appearance, candidate)))) low = candidate else high = candidate
        }
        geometry = clockGeometry(appearance, low)
        size = measure(geometry)
    }
    val offset = (appearance.edgeDistanceDp * density).roundToInt()
    return geometry.copy(
        offsetX = offset.coerceIn(0, (available.width - size.width).coerceAtLeast(0)),
        offsetY = offset.coerceIn(0, (available.height - size.height).coerceAtLeast(0))
    )
}
