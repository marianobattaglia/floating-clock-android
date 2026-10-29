package com.marianobattaglia.floatingclock

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Paint
import android.text.format.DateFormat
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.*
import com.marianobattaglia.floatingclock.ui.theme.DashboardColors
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.min

internal enum class DashboardSetting(val titleRes: Int, val subtitleRes: Int, val tint: Color) {
    Floating(R.string.clock_show_floating, R.string.clock_floating_subtitle, Color(0xFF69A5FF)),
    Position(R.string.clock_position, R.string.clock_position_subtitle, Color(0xFFBE9BFF)),
    Format(R.string.clock_format, R.string.clock_format_subtitle, Color(0xFFFFB56B)),
    Appearance(R.string.clock_appearance, R.string.clock_appearance_subtitle, Color(0xFF64D7BD)),
    Alarm(R.string.alarm_title, R.string.alarm_subtitle, Color(0xFFFF8E9D))
}

private enum class DashboardShortcut(val labelRes: Int, val icon: String) {
    Home(R.string.nav_home, "home"),
    Pro(R.string.nav_pro, "star"),
    Debug(R.string.nav_debug, "debug")
}

internal val LocalPreviewSelection = compositionLocalOf<(DashboardSetting) -> Unit> { {} }
internal val LocalSelectedPreview = compositionLocalOf<DashboardSetting?> { null }
internal val LocalFirstSettingFocus = compositionLocalOf<FocusRequester?> { null }
internal val LocalOpenPro = compositionLocalOf<() -> Unit> { {} }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ClockDashboard(
    floatingEnabled: Boolean,
    clock: @Composable (Boolean, (String) -> Unit) -> Unit,
    debugEnabled: Boolean = false,
    proContent: @Composable () -> Unit = {},
    debugContent: @Composable () -> Unit = {},
    settings: @Composable ColumnScope.() -> Unit
) {
    var selected by remember { mutableStateOf<DashboardSetting?>(null) }
    var clockTime by remember { mutableStateOf("") }
    var activeShortcut by rememberSaveable { mutableStateOf(DashboardShortcut.Home) }
    val firstSetting = remember { FocusRequester() }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val visibleSelection = selected.takeIf {
        floatingEnabled || it == DashboardSetting.Floating || it == DashboardSetting.Alarm
    }
    BackHandler(enabled = activeShortcut != DashboardShortcut.Home) {
        activeShortcut = DashboardShortcut.Home
    }
    LaunchedEffect(debugEnabled) {
        if (!debugEnabled && activeShortcut == DashboardShortcut.Debug) {
            activeShortcut = DashboardShortcut.Home
        }
    }
    LaunchedEffect(floatingEnabled) {
        if (!floatingEnabled && selected != DashboardSetting.Floating && selected != DashboardSetting.Alarm) {
            selected = null
        }
    }
    Row(
        modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(DashboardColors.Background, Color(0xFF1B2737)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().width(92.dp)
                .background(DashboardColors.Rail).padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.height(8.dp))
            DashboardShortcut.entries.filter { debugEnabled || it != DashboardShortcut.Debug }.forEach { shortcut ->
                val activate = {
                    activeShortcut = shortcut
                    scope.launch {
                        if (shortcut == DashboardShortcut.Home) {
                            scroll.animateScrollTo(0)
                            firstSetting.requestFocus()
                        }
                    }
                    Unit
                }
                Surface(
                    onClick = activate,
                    modifier = Modifier.width(78.dp).dashboardTap(activate),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (activeShortcut == shortcut) DashboardColors.Surface else Color.Transparent,
                        contentColor = if (activeShortcut == shortcut) DashboardColors.Accent else DashboardColors.Muted,
                        focusedContainerColor = DashboardColors.Focused,
                        focusedContentColor = DashboardColors.Text
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                    border = dashboardFocusBorder()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DashboardIcon(shortcut.icon, Modifier.size(20.dp))
                        Text(stringResource(shortcut.labelRes), fontSize = 10.sp)
                    }
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.dashboard_tagline), color = DashboardColors.Muted, fontSize = 12.sp)
                }
                clock(!floatingEnabled) { clockTime = it }
            }
            when (activeShortcut) {
                DashboardShortcut.Home -> Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    CompositionLocalProvider(
                        LocalPreviewSelection provides { selected = it },
                        LocalSelectedPreview provides visibleSelection,
                        LocalFirstSettingFocus provides firstSetting,
                        LocalOpenPro provides { activeShortcut = DashboardShortcut.Pro }
                    ) {
                        Column(
                            modifier = Modifier.weight(0.59f).fillMaxHeight()
                                .verticalScroll(scroll).padding(3.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                            content = settings
                        )
                    }
                    ClockPreviewPanel(
                        selected = visibleSelection,
                        clockTime = clockTime,
                        floatingEnabled = floatingEnabled,
                        modifier = Modifier.weight(0.41f).fillMaxHeight()
                    )
                }
                DashboardShortcut.Pro -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.TopStart
                ) { proContent() }
                DashboardShortcut.Debug -> if (debugEnabled) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.TopStart
                    ) { debugContent() }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun dashboardFocusBorder(selected: Boolean = false) = ClickableSurfaceDefaults.border(
    border = Border(BorderStroke(1.dp, if (selected) DashboardColors.Accent.copy(alpha = 0.55f) else Color.Transparent)),
    focusedBorder = Border(BorderStroke(2.dp, DashboardColors.Accent))
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun DashboardSettingsRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier,
    setting: DashboardSetting?,
    trailing: @Composable RowScope.() -> Unit
) {
    val selectPreview = LocalPreviewSelection.current
    val selected = setting != null && LocalSelectedPreview.current == setting
    val firstFocus = LocalFirstSettingFocus.current
    var focused by remember { mutableStateOf(false) }
    val activate = {
        setting?.let(selectPreview)
        onClick()
    }
    Surface(
        onClick = activate,
        modifier = modifier.fillMaxWidth().dashboardTap(activate)
            .onFocusChanged {
                focused = it.hasFocus
                if (it.hasFocus) setting?.let(selectPreview)
            }
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) DashboardColors.Accent else if (selected) DashboardColors.Accent.copy(alpha = 0.55f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            ).then(
            if (setting == DashboardSetting.Floating && firstFocus != null) Modifier.focusRequester(firstFocus)
            else Modifier
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DashboardColors.Surface,
            contentColor = DashboardColors.Text,
            focusedContainerColor = DashboardColors.Focused,
            focusedContentColor = DashboardColors.Text
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = dashboardFocusBorder(selected)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (setting != null) {
                Box(
                    modifier = Modifier.size(32.dp).background(setting.tint.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    DashboardIcon(setting.name, Modifier.size(19.dp), setting.tint)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (setting != null) Text(stringResource(setting.subtitleRes), color = DashboardColors.Muted, fontSize = 10.sp, lineHeight = 13.sp)
            }
            ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp)) {
                Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
            }
        }
    }
}

// This TV Material version handles D-pad and accessibility clicks on Surface. Forward
// pointer taps to the same callback without adding another focusable/clickable node.
// Child switches consume their own gestures, so a tap does not toggle the row twice.
@Composable
private fun Modifier.dashboardTap(onClick: () -> Unit): Modifier {
    val action by rememberUpdatedState(onClick)
    return pointerInput(Unit) {
        detectTapGestures(onTap = { action() })
    }
}

/** Shared presentation only; each caller retains its existing dialog controls and callbacks. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SettingsDialog(
    onDismissRequest: () -> Unit,
    isPhone: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        CompositionLocalProvider(
            LocalPreviewSelection provides {},
            LocalSelectedPreview provides null,
            LocalFirstSettingFocus provides null
        ) {
            if (isPhone) {
                androidx.compose.material3.Surface(
                    modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()
                        .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.88f),
                    shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
                    tonalElevation = 6.dp
                ) {
                    ProvideTextStyle(
                        MaterialTheme.typography.bodyMedium.copy(color = DashboardColors.Text)
                    ) {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            content = content
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()
                        .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.88f),
                    shape = RoundedCornerShape(20.dp),
                    border = Border(BorderStroke(1.dp, DashboardColors.Border), shape = RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        content = content
                    )
                }
            }
        }
    }
}

/** A read-only snapshot; no settings writes, broadcasts or service calls originate here. */
@Composable
private fun previewPreferences(): Map<String, *> {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var values by remember(prefs) { mutableStateOf(prefs.all.toMap()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(prefs, lifecycle) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { updated, _ -> values = updated.all.toMap() }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) values = prefs.all.toMap()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        lifecycle.addObserver(observer)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            lifecycle.removeObserver(observer)
        }
    }
    return values
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ClockPreviewPanel(selected: DashboardSetting?, clockTime: String, floatingEnabled: Boolean, modifier: Modifier) {
    val values = previewPreferences()
    val context = LocalContext.current
    val position = FloatingClockPosition.fromId(values[PREF_CLOCK_POSITION] as? String)
    val appearance = ClockAppearance.from(values)
    val use24h = values[PREF_CLOCK_USE_24H] as? Boolean ?: DateFormat.is24HourFormat(context)
    val seconds = values[PREF_CLOCK_SHOW_SECONDS] as? Boolean ?: false
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(DashboardColors.Panel)
            .verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardIcon("preview", Modifier.size(19.dp), DashboardColors.Accent)
            Text(stringResource(R.string.preview_title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        if (selected == null) {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f)
                    .background(DashboardColors.Background, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardIcon("Floating", Modifier.size(36.dp), DashboardColors.Muted)
                    Text(stringResource(R.string.preview_select_setting), color = DashboardColors.Muted, fontSize = 13.sp)
                }
            }
            Text(stringResource(R.string.preview_empty_hint), color = DashboardColors.Muted, fontSize = 12.sp)
        } else {
            Text(stringResource(selected.titleRes), fontSize = 13.sp, color = selected.tint)
            if (selected == DashboardSetting.Alarm) {
                val hour = values[PREF_ALARM_HOUR] as? Int ?: -1
                val minute = values[PREF_ALARM_MINUTE] as? Int ?: -1
                val alarmTime = if (hour >= 0 && minute >= 0) String.format(configurationLocale(LocalConfiguration.current), "%02d:%02d", hour, minute) else stringResource(R.string.time_unset)
                Box(
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .background(DashboardColors.Background, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(alarmTime, fontSize = 36.sp, fontWeight = FontWeight.Light)
                }
                PreviewValue(stringResource(R.string.alarm_title), if (values[PREF_ALARM_ENABLED] == true) stringResource(R.string.status_on) else stringResource(R.string.status_off))
            } else {
                MiniatureClockScene(position, appearance, clockTime.ifEmpty { stringResource(R.string.time_unset) }, floatingEnabled)
                when (selected) {
                    DashboardSetting.Floating -> PreviewValue(stringResource(R.string.preview_floating_clock), if (floatingEnabled) stringResource(R.string.status_on) else stringResource(R.string.status_off))
                    DashboardSetting.Position -> PreviewValue(stringResource(R.string.preview_position), stringResource(position.labelRes))
                    DashboardSetting.Format -> {
                        PreviewValue(stringResource(R.string.preview_format), if (use24h) stringResource(R.string.clock_format_24_hour) else stringResource(R.string.clock_format_12_hour))
                        PreviewValue(stringResource(R.string.preview_seconds), if (seconds) stringResource(R.string.status_shown) else stringResource(R.string.status_hidden))
                    }
                    DashboardSetting.Appearance -> {
                        PreviewValue(stringResource(R.string.appearance_edge_distance), stringResource(R.string.value_distance_dp, appearance.edgeDistanceDp))
                        PreviewValue(stringResource(R.string.appearance_overall_size), stringResource(R.string.value_percent, appearance.scalePercent))
                        PreviewValue(stringResource(R.string.appearance_internal_spacing), stringResource(R.string.value_percent, appearance.paddingPercent))
                    }
                    else -> Unit
                }
            }
            Text(stringResource(R.string.preview_saved_hint), color = DashboardColors.Muted, fontSize = 10.sp)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PreviewValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), color = DashboardColors.Muted, fontSize = 11.sp)
        Text(value, modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun MiniatureClockScene(
    position: FloatingClockPosition,
    appearance: ClockAppearance,
    time: String,
    showClock: Boolean
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val displayWidth = configuration.screenWidthDp * density.density
    val displayHeight = configuration.screenHeightDp * density.density
    val fontScale = density.fontScale
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val background = ImageBitmap.imageResource(R.drawable.preview_landscape)
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(displayWidth / displayHeight)
            .clip(RoundedCornerShape(10.dp)).background(Color(0xFF252F3F))
    ) {
        // Fill the existing frame with a centered crop, preserving the photograph's proportions.
        val sourceWidth = min(background.width.toFloat(), background.height * size.width / size.height).toInt().coerceAtLeast(1)
        val sourceHeight = min(background.height.toFloat(), background.width * size.height / size.width).toInt().coerceAtLeast(1)
        drawImage(
            background,
            srcOffset = IntOffset((background.width - sourceWidth) / 2, (background.height - sourceHeight) / 2),
            srcSize = IntSize(sourceWidth, sourceHeight),
            dstSize = IntSize(size.width.toInt(), size.height.toInt())
        )
        if (!showClock) return@Canvas
        val geometry = fitClockGeometry(
            appearance, density.density, ClockPixelSize(displayWidth.toInt(), displayHeight.toInt())
        ) { candidate ->
            paint.textSize = candidate.textSizeSp * density.density * fontScale
            ClockPixelSize(
                ceil(paint.measureText(time)).toInt() + candidate.horizontalPaddingPx * 2,
                ceil(paint.fontMetrics.descent - paint.fontMetrics.ascent).toInt() + candidate.verticalPaddingPx * 2
            )
        }
        val scale = min(size.width / displayWidth, size.height / displayHeight)
        paint.textSize = geometry.textSizeSp * density.density * fontScale * scale
        val paddingX = geometry.horizontalPaddingPx * scale
        val paddingY = geometry.verticalPaddingPx * scale
        val width = paint.measureText(time) + paddingX * 2
        val height = paint.fontMetrics.descent - paint.fontMetrics.ascent + paddingY * 2
        val right = position == FloatingClockPosition.TOP_RIGHT || position == FloatingClockPosition.BOTTOM_RIGHT
        val bottom = position == FloatingClockPosition.BOTTOM_LEFT || position == FloatingClockPosition.BOTTOM_RIGHT
        val x = if (right) size.width - width - geometry.offsetX * scale else geometry.offsetX * scale
        val y = if (bottom) size.height - height - geometry.offsetY * scale else geometry.offsetY * scale
        drawRoundRect(Color.Black.copy(alpha = 0.75f), Offset(x, y), Size(width, height), CornerRadius(4f))
        paint.color = android.graphics.Color.WHITE
        drawContext.canvas.nativeCanvas.drawText(time, x + paddingX, y + paddingY - paint.fontMetrics.ascent, paint)
    }
}

/** Small line icons drawn locally, so no icon library or downloaded assets are needed. */
@Composable
internal fun DashboardIcon(kind: String, modifier: Modifier = Modifier, tint: Color = DashboardColors.Accent) {
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        val stroke = Stroke(width = 1.8f * unit)
        fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
        fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(tint, point(x, y), point(x2, y2), stroke.width)
        when (kind) {
            "home" -> {
                val roof = Path().apply { moveTo(3 * unit, 11 * unit); lineTo(12 * unit, 3 * unit); lineTo(21 * unit, 11 * unit) }
                drawPath(roof, tint, style = stroke)
                line(6f, 10f, 6f, 21f); line(6f, 21f, 18f, 21f); line(18f, 21f, 18f, 10f)
                line(10f, 21f, 10f, 14f); line(10f, 14f, 14f, 14f); line(14f, 14f, 14f, 21f)
            }
            "settings", "Appearance" -> {
                drawCircle(tint, 7 * unit, point(12f, 12f), style = stroke)
                drawCircle(tint, 2.5f * unit, point(12f, 12f), style = stroke)
                line(12f, 1f, 12f, 5f); line(12f, 19f, 12f, 23f)
                line(1f, 12f, 5f, 12f); line(19f, 12f, 23f, 12f)
                line(4f, 4f, 7f, 7f); line(17f, 17f, 20f, 20f)
                line(4f, 20f, 7f, 17f); line(17f, 7f, 20f, 4f)
            }
            "Position" -> {
                line(12f, 2f, 12f, 22f); line(2f, 12f, 22f, 12f)
                line(12f, 2f, 8f, 6f); line(12f, 2f, 16f, 6f)
                line(12f, 22f, 8f, 18f); line(12f, 22f, 16f, 18f)
                line(2f, 12f, 6f, 8f); line(2f, 12f, 6f, 16f)
                line(22f, 12f, 18f, 8f); line(22f, 12f, 18f, 16f)
            }
            "preview" -> {
                drawRoundRect(tint, point(2f, 3f), Size(20 * unit, 14 * unit), CornerRadius(2 * unit), style = stroke)
                line(12f, 17f, 12f, 21f); line(7f, 21f, 17f, 21f)
            }
            "star" -> {
                val star = Path().apply {
                    moveTo(12 * unit, 2 * unit)
                    lineTo(14.8f * unit, 8.3f * unit)
                    lineTo(21.5f * unit, 9 * unit)
                    lineTo(16.5f * unit, 13.5f * unit)
                    lineTo(18 * unit, 20.5f * unit)
                    lineTo(12 * unit, 16.8f * unit)
                    lineTo(6 * unit, 20.5f * unit)
                    lineTo(7.5f * unit, 13.5f * unit)
                    lineTo(2.5f * unit, 9 * unit)
                    lineTo(9.2f * unit, 8.3f * unit)
                    close()
                }
                drawPath(star, tint)
            }
            "debug" -> {
                drawCircle(tint, 6 * unit, point(12f, 12f), style = stroke)
                line(12f, 1f, 12f, 5f)
                line(8f, 4f, 5f, 2f); line(16f, 4f, 19f, 2f)
                line(6f, 9f, 2f, 7f); line(18f, 9f, 22f, 7f)
                line(6f, 15f, 2f, 17f); line(18f, 15f, 22f, 17f)
                line(9f, 12f, 15f, 12f)
            }
            else -> {
                drawCircle(tint, 8.5f * unit, point(12f, 12f), style = stroke)
                line(12f, 6f, 12f, 12f); line(12f, 12f, 16f, 14f)
                if (kind == "Alarm") { line(3f, 4f, 6f, 2f); line(18f, 2f, 21f, 4f) }
            }
        }
    }
}
