package com.marianobattaglia.floatingclock

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.marianobattaglia.floatingclock.ui.theme.DashboardColors
import com.marianobattaglia.floatingclock.ui.theme.FloatingClockTheme
import java.util.Calendar
import java.util.Date
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import java.text.SimpleDateFormat
import kotlinx.coroutines.delay
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.app.ActivityManager
import android.text.format.DateFormat
import androidx.compose.material3.Surface as PhoneSurface
import androidx.compose.material3.Switch as PhoneMaterialSwitch

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        synchronizeNotificationChannelNames(this)
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isAndroidTv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        setContent {
            FloatingClockTheme {
                if (isAndroidTv) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RectangleShape
                    ) {
                        MainScreen()
                    }
                } else {
                    FloatingClockPhoneTheme {
                        PhoneSurface(modifier = Modifier.fillMaxSize()) {
                            MainScreen(isPhone = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(isPhone: Boolean = false) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val inspectionMode = LocalInspectionMode.current
    val proManager = remember(context) { ProEntitlementManager(context) }
    val proState = proManager.state
    var showReviewerAccessDialog by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(proManager, lifecycleOwner, inspectionMode) {
        if (!inspectionMode) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) proManager.refresh()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            proManager.start()
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                proManager.close()
            }
        } else {
            onDispose { proManager.close() }
        }
    }
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var floatingEnabled by remember {
        mutableStateOf(prefs.getBoolean(PREF_FLOATING_ENABLED, false))
    }
    val setFloatingEnabled = remember(prefs) {
        { enabled: Boolean ->
            floatingEnabled = enabled
            prefs.edit().putBoolean(PREF_FLOATING_ENABLED, enabled).apply()
        }
    }

    val settings: @Composable ColumnScope.() -> Unit = {
        FloatingClockSwitch(
            isChecked = floatingEnabled,
            setFloatingEnabled = setFloatingEnabled,
            isPhone = isPhone
        )
        if (floatingEnabled) {
            FloatingClockPositionRow(isPhone = isPhone)
            FloatingClockFormatRow(isPhone = isPhone)
            FloatingClockAppearanceRow(
                hasPro = proState.effectiveStatus == ActualProStatus.PRO,
                isPhone = isPhone
            )
        }
        AlarmConfigRow(
            isPro = proState.effectiveStatus == ActualProStatus.PRO,
            isPhone = isPhone
        )
    }
    val clock: @Composable (Boolean, (String) -> Unit) -> Unit = { visible, onTimeChanged ->
        Clock(onTimeChanged = onTimeChanged, visible = visible, isPhone = isPhone)
    }
    if (isPhone) {
        PhoneDashboard(
            floatingEnabled = floatingEnabled,
            clock = clock,
            debugEnabled = BuildConfig.DEBUG,
            proContent = {
                PhoneProAccessContent(
                    state = proState,
                    onPurchase = proManager::launchProPurchase,
                    onOpenReviewAccess = { showReviewerAccessDialog = true }
                )
            },
            debugContent = {
                PhoneProDebugContent(proState, proManager::setDebugOverride)
            },
            settings = settings
        )
    } else {
        ClockDashboard(
            floatingEnabled = floatingEnabled,
            clock = clock,
            debugEnabled = BuildConfig.DEBUG,
            proContent = {
                ProAccessContent(
                    state = proState,
                    onPurchase = proManager::launchProPurchase,
                    onOpenReviewAccess = { showReviewerAccessDialog = true }
                )
            },
            debugContent = {
                ProDebugContent(proState, proManager::setDebugOverride)
            },
            settings = settings
        )
    }

    if (showReviewerAccessDialog) {
        if (isPhone) {
            PhoneReviewerAccessDialog(
                onDismissRequest = { showReviewerAccessDialog = false },
                onSubmit = proManager::redeemReviewAccessCode
            )
        } else {
            ReviewerAccessDialog(
                onDismissRequest = { showReviewerAccessDialog = false },
                onSubmit = proManager::redeemReviewAccessCode
            )
        }
    }

    LaunchedEffect(Unit) {
        val actuallyRunning = isServiceRunning(context, FloatingClockService::class.java)
        if (floatingEnabled != actuallyRunning) {
            floatingEnabled = actuallyRunning
            prefs.edit().putBoolean(PREF_FLOATING_ENABLED, actuallyRunning).apply()
        }
    }
}


@Composable
fun Clock(
    modifier: Modifier = Modifier,
    onTimeChanged: (String) -> Unit = {},
    visible: Boolean = true,
    isPhone: Boolean = false
) {
    var time by remember { mutableStateOf("") }
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var showSeconds by remember {
        mutableStateOf(prefs.getBoolean(PREF_CLOCK_SHOW_SECONDS, false))
    }
    var use24h by remember {
        mutableStateOf(
            prefs.getBoolean(PREF_CLOCK_USE_24H, DateFormat.is24HourFormat(context))
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                showSeconds = prefs.getBoolean(PREF_CLOCK_SHOW_SECONDS, false)
                use24h = prefs.getBoolean(
                    PREF_CLOCK_USE_24H,
                    DateFormat.is24HourFormat(context)
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_CLOCK_SECONDS_CHANGED -> {
                        showSeconds = intent.getBooleanExtra(
                            EXTRA_CLOCK_SHOW_SECONDS,
                            false
                        )
                    }
                    ACTION_CLOCK_FORMAT_CHANGED -> {
                        use24h = intent.getBooleanExtra(
                            EXTRA_CLOCK_USE_24H,
                            DateFormat.is24HourFormat(context)
                        )
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_CLOCK_SECONDS_CHANGED)
            addAction(ACTION_CLOCK_FORMAT_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    val locale = configurationLocale(LocalConfiguration.current)
    LaunchedEffect(showSeconds, use24h, locale) {
        while (true) {
            val pattern = clockPattern(use24h, showSeconds)
            val formatter = SimpleDateFormat(pattern, locale)
            time = formatter.format(Date())
            delay(1000)
        }
    }

    val formattedTime = time
    SideEffect { onTimeChanged(formattedTime) }

    if (visible) {
        Box(
            modifier = modifier.padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = time,
                fontSize = if (isPhone) 26.sp else 36.sp,
                color = DashboardColors.Text,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FloatingClockSwitch(
    isChecked: Boolean,
    setFloatingEnabled: (Boolean) -> Unit,
    isPhone: Boolean = false
) {
    val context = LocalContext.current
    var pendingEnable by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted || !pendingEnable) {
            pendingEnable = false
            return@rememberLauncherForActivityResult
        }

        pendingEnable = false
        if (!canDrawOverlaysCompat(context)) {
            Toast.makeText(
                context,
                context.getString(R.string.permission_overlay_denied),
                Toast.LENGTH_LONG
            ).show()
            return@rememberLauncherForActivityResult
        }

        setFloatingEnabled(true)
        val intent = Intent(context, FloatingClockService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    val selectPreview = LocalPreviewSelection.current
    val onToggle: (Boolean) -> Unit = onToggle@{ checked ->
        selectPreview(DashboardSetting.Floating)
        if (checked) {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                pendingEnable = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                Toast.makeText(
                    context,
                    context.getString(R.string.permission_notification_required),
                    Toast.LENGTH_LONG
                ).show()
                return@onToggle
            }

            if (!canDrawOverlaysCompat(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // No settings screen available; keep the switch off.
                }
                Toast.makeText(
                    context,
                    context.getString(R.string.permission_overlay_denied),
                    Toast.LENGTH_LONG
                ).show()
                return@onToggle
            }
        }

        setFloatingEnabled(checked)
        val intent = Intent(context, FloatingClockService::class.java)
        if (checked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            context.stopService(intent)
        }
    }

    SettingsRow(
        title = stringResource(R.string.clock_show_floating),
        setting = DashboardSetting.Floating,
        isPhone = isPhone,
        onClick = { onToggle(!isChecked) }
    ) {
        if (isPhone) {
            PhoneMaterialSwitch(checked = isChecked, onCheckedChange = onToggle)
        } else {
            Switch(checked = isChecked, onCheckedChange = onToggle)
        }
    }
}

private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    val services = manager.getRunningServices(Int.MAX_VALUE)
    return services.any { it.service.className == serviceClass.name }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    setting: DashboardSetting? = null,
    isPhone: Boolean = false,
    trailing: @Composable RowScope.() -> Unit
) {
    if (isPhone) {
        PhoneSettingsRow(
            title = title,
            subtitle = setting?.let { stringResource(it.subtitleRes) },
            onClick = onClick,
            modifier = modifier,
            trailing = trailing
        )
    } else {
        DashboardSettingsRow(title, onClick, modifier, setting, trailing)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FloatingClockPositionRow(isPhone: Boolean = false) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var showDialog by remember { mutableStateOf(false) }
    var positionId by remember {
        mutableStateOf(prefs.getString(PREF_CLOCK_POSITION, FloatingClockPosition.TOP_LEFT.id))
    }
    var dialogPosition by remember {
        mutableStateOf(FloatingClockPosition.fromId(positionId))
    }

    LaunchedEffect(showDialog) {
        if (!showDialog) return@LaunchedEffect
        dialogPosition = FloatingClockPosition.fromId(positionId)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                positionId = prefs.getString(
                    PREF_CLOCK_POSITION,
                    FloatingClockPosition.TOP_LEFT.id
                )
                dialogPosition = FloatingClockPosition.fromId(positionId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val currentPosition = FloatingClockPosition.fromId(positionId)
    SettingsRow(
        title = stringResource(R.string.clock_position),
        setting = DashboardSetting.Position,
        isPhone = isPhone,
        onClick = { showDialog = true }
    ) {
        Text(text = stringResource(currentPosition.labelRes))
    }

    if (showDialog) {
        SettingsDialog(onDismissRequest = { showDialog = false }, isPhone = isPhone) {
            Text(text = stringResource(R.string.clock_select_position), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            val applySelection: (FloatingClockPosition) -> Unit = { position ->
                dialogPosition = position
                positionId = position.id
                prefs.edit().putString(PREF_CLOCK_POSITION, positionId).apply()
                notifyClockPositionChanged(context, position)
            }
            Box(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 24.dp)
                    .size(240.dp, 160.dp)
                    .border(1.dp, DashboardColors.Accent, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PositionButton(
                            selected = dialogPosition == FloatingClockPosition.TOP_LEFT,
                            label = stringResource(FloatingClockPosition.TOP_LEFT.labelRes),
                            isPhone = isPhone,
                            onClick = { applySelection(FloatingClockPosition.TOP_LEFT) }
                        )
                        PositionButton(
                            selected = dialogPosition == FloatingClockPosition.TOP_RIGHT,
                            label = stringResource(FloatingClockPosition.TOP_RIGHT.labelRes),
                            isPhone = isPhone,
                            onClick = { applySelection(FloatingClockPosition.TOP_RIGHT) }
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PositionButton(
                            selected = dialogPosition == FloatingClockPosition.BOTTOM_LEFT,
                            label = stringResource(FloatingClockPosition.BOTTOM_LEFT.labelRes),
                            isPhone = isPhone,
                            onClick = { applySelection(FloatingClockPosition.BOTTOM_LEFT) }
                        )
                        PositionButton(
                            selected = dialogPosition == FloatingClockPosition.BOTTOM_RIGHT,
                            label = stringResource(FloatingClockPosition.BOTTOM_RIGHT.labelRes),
                            isPhone = isPhone,
                            onClick = { applySelection(FloatingClockPosition.BOTTOM_RIGHT) }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FormFactorButton(isPhone = isPhone, onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FloatingClockFormatRow(isPhone: Boolean = false) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var showDialog by remember { mutableStateOf(false) }
    var showSeconds by remember { mutableStateOf(false) }
    var use24h by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val system24h = DateFormat.is24HourFormat(context)
        if (!prefs.contains(PREF_CLOCK_USE_24H)) {
            prefs.edit().putBoolean(PREF_CLOCK_USE_24H, system24h).apply()
        }
        if (!prefs.contains(PREF_CLOCK_SHOW_SECONDS)) {
            prefs.edit().putBoolean(PREF_CLOCK_SHOW_SECONDS, false).apply()
        }
        use24h = prefs.getBoolean(PREF_CLOCK_USE_24H, system24h)
        showSeconds = prefs.getBoolean(PREF_CLOCK_SHOW_SECONDS, false)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val system24h = DateFormat.is24HourFormat(context)
                use24h = prefs.getBoolean(PREF_CLOCK_USE_24H, system24h)
                showSeconds = prefs.getBoolean(PREF_CLOCK_SHOW_SECONDS, false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SettingsRow(
        title = stringResource(R.string.clock_format),
        setting = DashboardSetting.Format,
        isPhone = isPhone,
        onClick = { showDialog = true }
    ) {
        val label = if (use24h) stringResource(R.string.clock_format_24_hour) else stringResource(R.string.clock_format_12_hour)
        Text(text = label)
    }

    if (showDialog) {
        SettingsDialog(onDismissRequest = { showDialog = false }, isPhone = isPhone) {
            Text(text = stringResource(R.string.clock_format), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            SettingsRow(
                title = stringResource(R.string.clock_show_seconds),
                isPhone = isPhone,
                onClick = {
                    val newValue = !showSeconds
                    showSeconds = newValue
                    prefs.edit().putBoolean(PREF_CLOCK_SHOW_SECONDS, newValue).apply()
                    notifyClockSecondsChanged(context, newValue)
                }
            ) {
                if (isPhone) {
                    PhoneMaterialSwitch(
                        checked = showSeconds,
                        onCheckedChange = { checked ->
                            showSeconds = checked
                            prefs.edit().putBoolean(PREF_CLOCK_SHOW_SECONDS, checked).apply()
                            notifyClockSecondsChanged(context, checked)
                        }
                    )
                } else {
                    Switch(
                        checked = showSeconds,
                        onCheckedChange = { checked ->
                            showSeconds = checked
                            prefs.edit().putBoolean(PREF_CLOCK_SHOW_SECONDS, checked).apply()
                            notifyClockSecondsChanged(context, checked)
                        }
                    )
                }
            }
            SettingsRow(
                title = stringResource(R.string.clock_use_24_hour),
                isPhone = isPhone,
                onClick = {
                    val newValue = !use24h
                    use24h = newValue
                    prefs.edit().putBoolean(PREF_CLOCK_USE_24H, newValue).apply()
                    notifyClockFormatChanged(context, newValue)
                }
            ) {
                if (isPhone) {
                    PhoneMaterialSwitch(
                        checked = use24h,
                        onCheckedChange = { checked ->
                            use24h = checked
                            prefs.edit().putBoolean(PREF_CLOCK_USE_24H, checked).apply()
                            notifyClockFormatChanged(context, checked)
                        }
                    )
                } else {
                    Switch(
                        checked = use24h,
                        onCheckedChange = { checked ->
                            use24h = checked
                            prefs.edit().putBoolean(PREF_CLOCK_USE_24H, checked).apply()
                            notifyClockFormatChanged(context, checked)
                        }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FormFactorButton(isPhone = isPhone, onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PositionButton(
    selected: Boolean,
    label: String,
    isPhone: Boolean = false,
    onClick: () -> Unit
) {
    FormFactorButton(onClick = onClick, isPhone = isPhone, modifier = Modifier.semantics {
        contentDescription = label
        this.selected = selected
    }) {
        Text(if (isPhone) label else stringResource(if (selected) R.string.symbol_selected else R.string.symbol_unselected))
    }
}

private const val PENDING_ALARM_ACTION_NONE = 0
private const val PENDING_ALARM_ACTION_SHOW_DIALOG = 1
private const val PENDING_ALARM_ACTION_ACTIVATE = 2

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AlarmConfigRow(isPro: Boolean, isPhone: Boolean = false) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var alarmEnabled by remember {
        mutableStateOf(prefs.getBoolean(PREF_ALARM_ENABLED, false))
    }
    var alarmHour by remember {
        mutableStateOf(prefs.getInt(PREF_ALARM_HOUR, -1))
    }
    var alarmMinute by remember {
        mutableStateOf(prefs.getInt(PREF_ALARM_MINUTE, -1))
    }
    var lastFreeAlarmSuccessMs by remember {
        mutableStateOf(prefs.getLong(PREF_FREE_ALARM_LAST_SUCCESS_MS, 0L))
    }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var dialogHour by rememberSaveable { mutableStateOf(0) }
    var dialogMinute by rememberSaveable { mutableStateOf(0) }
    var alarmActive by remember { mutableStateOf(false) }
    var exactAccessMissing by rememberSaveable { mutableStateOf(false) }
    var pendingPermissionAction by rememberSaveable {
        mutableStateOf(PENDING_ALARM_ACTION_NONE)
    }
    var pendingAlarmHour by rememberSaveable { mutableStateOf(-1) }
    var pendingAlarmMinute by rememberSaveable { mutableStateOf(-1) }

    LaunchedEffect(lastFreeAlarmSuccessMs) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(30_000)
        }
    }
    val alarmLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(alarmLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) nowMs = System.currentTimeMillis()
        }
        alarmLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { alarmLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val remainingFreeCooldownMs = remainingFreeAlarmCooldownMillis(
        lastFreeAlarmSuccessMs,
        nowMs
    )

    fun cooldownDuration(): String {
        val (hours, minutes) = freeAlarmCooldownDurationParts(remainingFreeCooldownMs)
        return context.getString(R.string.alarm_cooldown_remaining, hours, minutes)
    }

    fun showCooldownMessage() {
        Toast.makeText(
            context,
            context.getString(R.string.alarm_cooldown_message, cooldownDuration()),
            Toast.LENGTH_LONG
        ).show()
    }

    fun canActivateAlarm(): Boolean = isPro || alarmEnabled || remainingFreeCooldownMs <= 0L

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != AlarmSoundService.ACTION_ALARM_STATE) return
                alarmActive = intent.getBooleanExtra(
                    AlarmSoundService.EXTRA_ALARM_ACTIVE,
                    false
                )
            }
        }
        val filter = IntentFilter(AlarmSoundService.ACTION_ALARM_STATE)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    fun openConfigurationDialog() {
        val calendar = Calendar.getInstance()
        dialogHour = if (alarmHour >= 0) alarmHour else calendar.get(Calendar.HOUR_OF_DAY)
        dialogMinute = if (alarmMinute >= 0) alarmMinute else calendar.get(Calendar.MINUTE)
        showDialog = true
    }

    fun scheduleSelectedAlarm(hour: Int, minute: Int, closeDialog: Boolean) {
        if (!canActivateAlarm()) {
            showCooldownMessage()
            return
        }
        val wasAlreadyEnabled = alarmEnabled
        val result = AlarmScheduler.scheduleAlarm(context, hour, minute)
        when (result) {
            AlarmScheduleResult.EXACT,
            AlarmScheduleResult.APPROXIMATE -> {
                alarmHour = hour
                alarmMinute = minute
                alarmEnabled = true
                exactAccessMissing = result == AlarmScheduleResult.APPROXIMATE
                prefs.edit()
                    .putBoolean(PREF_ALARM_ENABLED, true)
                    .putInt(PREF_ALARM_HOUR, hour)
                    .putInt(PREF_ALARM_MINUTE, minute)
                    .also { editor ->
                        if (!isPro && !wasAlreadyEnabled) {
                            val activatedAt = System.currentTimeMillis()
                            lastFreeAlarmSuccessMs = activatedAt
                            nowMs = activatedAt
                            editor.putLong(PREF_FREE_ALARM_LAST_SUCCESS_MS, activatedAt)
                        }
                    }
                    .apply()
                if (closeDialog) showDialog = false

                if (result == AlarmScheduleResult.APPROXIMATE) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.alarm_scheduling_approximate),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            AlarmScheduleResult.FAILED -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.alarm_scheduling_unavailable),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun continuePendingPermissionAction() {
        val action = pendingPermissionAction
        pendingPermissionAction = PENDING_ALARM_ACTION_NONE
        exactAccessMissing = AlarmScheduler.needsExactAlarmPermission(context)

        when (action) {
            PENDING_ALARM_ACTION_SHOW_DIALOG -> openConfigurationDialog()
            PENDING_ALARM_ACTION_ACTIVATE -> scheduleSelectedAlarm(
                pendingAlarmHour,
                pendingAlarmMinute,
                closeDialog = false
            )
        }
    }

    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        continuePendingPermissionAction()
    }

    fun requestExactAlarmAccess(action: Int, hour: Int = -1, minute: Int = -1) {
        pendingPermissionAction = action
        pendingAlarmHour = hour
        pendingAlarmMinute = minute

        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(Uri.parse("package:${context.packageName}"))
        try {
            exactAlarmSettingsLauncher.launch(intent)
        } catch (_: Exception) {
            continuePendingPermissionAction()
        }
    }

    fun openAlarmConfiguration() {
        if (AlarmScheduler.needsExactAlarmPermission(context)) {
            requestExactAlarmAccess(PENDING_ALARM_ACTION_SHOW_DIALOG)
        } else {
            exactAccessMissing = false
            openConfigurationDialog()
        }
    }

    val selectPreview = LocalPreviewSelection.current
    val onAlarmToggle: (Boolean) -> Unit = onAlarmToggle@{ checked ->
        selectPreview(DashboardSetting.Alarm)
        if (checked && !canActivateAlarm()) {
            showCooldownMessage()
            return@onAlarmToggle
        }
        if (checked && (alarmHour < 0 || alarmMinute < 0)) {
            openAlarmConfiguration()
            return@onAlarmToggle
        }
        if (checked) {
            if (AlarmScheduler.needsExactAlarmPermission(context)) {
                requestExactAlarmAccess(
                    PENDING_ALARM_ACTION_ACTIVATE,
                    alarmHour,
                    alarmMinute
                )
                return@onAlarmToggle
            }
            exactAccessMissing = false
            scheduleSelectedAlarm(alarmHour, alarmMinute, closeDialog = false)
        } else {
            AlarmScheduler.cancelAlarm(context)
            alarmEnabled = false
            prefs.edit().putBoolean(PREF_ALARM_ENABLED, false).apply()
        }
    }
    SettingsRow(
        title = stringResource(R.string.alarm_title),
        setting = DashboardSetting.Alarm,
        isPhone = isPhone,
        onClick = {
            if (alarmActive) {
                AlarmScheduler.cancelAlarm(context)
                alarmEnabled = false
                prefs.edit()
                    .putBoolean(PREF_ALARM_ENABLED, false)
                    .apply()
            } else {
                openAlarmConfiguration()
            }
        }
    ) {
        if (isPhone) {
            PhoneMaterialSwitch(checked = alarmEnabled, onCheckedChange = onAlarmToggle)
        } else {
            Switch(checked = alarmEnabled, onCheckedChange = onAlarmToggle)
        }
    }

    if (showDialog) {
        SettingsDialog(onDismissRequest = { showDialog = false }, isPhone = isPhone) {
            Text(text = stringResource(R.string.alarm_configure), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            if (exactAccessMissing) {
                Text(
                    text = stringResource(R.string.alarm_exact_access_missing),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (!isPro && !alarmEnabled && remainingFreeCooldownMs > 0L) {
                Text(
                    text = stringResource(
                        R.string.alarm_cooldown_message,
                        cooldownDuration()
                    ),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Row(
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val increaseHour = stringResource(R.string.alarm_increase_hour)
                    FormFactorButton(isPhone = isPhone, onClick = { dialogHour = (dialogHour + 1) % 24 },
                        modifier = Modifier.semantics { contentDescription = increaseHour }) {
                        Text(stringResource(R.string.symbol_increase))
                    }
                    Text(
                        text = String.format(configurationLocale(LocalConfiguration.current), "%02d", dialogHour),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val decreaseHour = stringResource(R.string.alarm_decrease_hour)
                    FormFactorButton(isPhone = isPhone, onClick = { dialogHour = (dialogHour + 23) % 24 },
                        modifier = Modifier.semantics { contentDescription = decreaseHour }) {
                        Text(stringResource(R.string.symbol_decrease))
                    }
                }
                Text(
                    text = stringResource(R.string.time_separator),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val increaseMinute = stringResource(R.string.alarm_increase_minute)
                    FormFactorButton(isPhone = isPhone, onClick = { dialogMinute = (dialogMinute + 1) % 60 },
                        modifier = Modifier.semantics { contentDescription = increaseMinute }) {
                        Text(stringResource(R.string.symbol_increase))
                    }
                    Text(
                        text = String.format(configurationLocale(LocalConfiguration.current), "%02d", dialogMinute),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val decreaseMinute = stringResource(R.string.alarm_decrease_minute)
                    FormFactorButton(isPhone = isPhone, onClick = { dialogMinute = (dialogMinute + 59) % 60 },
                        modifier = Modifier.semantics { contentDescription = decreaseMinute }) {
                        Text(stringResource(R.string.symbol_decrease))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FormFactorButton(isPhone = isPhone, onClick = {
                    scheduleSelectedAlarm(dialogHour, dialogMinute, closeDialog = true)
                }, enabled = canActivateAlarm()) {
                    Text(stringResource(R.string.action_activate))
                }
                FormFactorButton(isPhone = isPhone, onClick = {
                    AlarmScheduler.cancelAlarm(context)
                    alarmEnabled = false
                    prefs.edit()
                        .putBoolean(PREF_ALARM_ENABLED, false)
                        .apply()
                    showDialog = false
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }
}

private fun notifyClockPositionChanged(
    context: Context,
    position: FloatingClockPosition
) {
    val intent = Intent(ACTION_CLOCK_POSITION_CHANGED)
        .setPackage(context.packageName)
        .putExtra(EXTRA_CLOCK_POSITION, position.id)
    context.sendBroadcast(intent)
}

private fun notifyClockSecondsChanged(
    context: Context,
    showSeconds: Boolean
) {
    val intent = Intent(ACTION_CLOCK_SECONDS_CHANGED)
        .setPackage(context.packageName)
        .putExtra(EXTRA_CLOCK_SHOW_SECONDS, showSeconds)
    context.sendBroadcast(intent)
}

private fun notifyClockFormatChanged(
    context: Context,
    use24h: Boolean
) {
    val intent = Intent(ACTION_CLOCK_FORMAT_CHANGED)
        .setPackage(context.packageName)
        .putExtra(EXTRA_CLOCK_USE_24H, use24h)
    context.sendBroadcast(intent)
}

private fun canDrawOverlaysCompat(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FloatingClockTheme {
        MainScreen()
    }
}
