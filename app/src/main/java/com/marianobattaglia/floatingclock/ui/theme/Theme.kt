package com.marianobattaglia.floatingclock.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FloatingClockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = DashboardColors.Accent,
            onPrimary = DashboardColors.Rail,
            secondary = DashboardColors.Accent,
            background = DashboardColors.Background,
            onBackground = DashboardColors.Text,
            surface = DashboardColors.Surface,
            onSurface = DashboardColors.Text,
            surfaceVariant = DashboardColors.Panel,
            onSurfaceVariant = DashboardColors.Muted,
            border = DashboardColors.Border
        ),
        typography = Typography,
        content = content
    )
}
