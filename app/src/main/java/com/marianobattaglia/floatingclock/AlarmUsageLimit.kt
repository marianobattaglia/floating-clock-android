package com.marianobattaglia.floatingclock

internal const val FREE_ALARM_COOLDOWN_MILLIS = 24L * 60L * 60L * 1000L

internal fun remainingFreeAlarmCooldownMillis(lastSuccessfulActivationMillis: Long, nowMillis: Long): Long {
    if (lastSuccessfulActivationMillis <= 0L) return 0L
    if (lastSuccessfulActivationMillis >= nowMillis) return FREE_ALARM_COOLDOWN_MILLIS
    val elapsed = nowMillis - lastSuccessfulActivationMillis
    if (elapsed < 0L) return FREE_ALARM_COOLDOWN_MILLIS
    return (FREE_ALARM_COOLDOWN_MILLIS - elapsed).coerceAtLeast(0L)
}

internal fun freeAlarmCooldownDurationParts(remainingMillis: Long): Pair<Int, Int> {
    val totalMinutes = ((remainingMillis.coerceAtLeast(1L) + 59_999L) / 60_000L).toInt()
    return totalMinutes / 60 to totalMinutes % 60
}
