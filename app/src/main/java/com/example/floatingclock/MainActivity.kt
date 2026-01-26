package com.example.floatingclock

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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.example.floatingclock.ui.theme.FloatingClockTheme
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import kotlinx.coroutines.delay
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.app.ActivityManager
import android.text.format.DateFormat

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FloatingClockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
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

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Clock()
        FloatingClockSwitch(
            isChecked = floatingEnabled,
            setFloatingEnabled = setFloatingEnabled
        )
        if (floatingEnabled) {
            FloatingClockPositionRow()
            FloatingClockFormatRow()
        }
        AlarmConfigRow()
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
fun Clock(modifier: Modifier = Modifier) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(showSeconds, use24h) {
        while (true) {
            val pattern = when {
                use24h && showSeconds -> "HH:mm:ss"
                use24h -> "HH:mm"
                showSeconds -> "hh:mm:ss a"
                else -> "hh:mm a"
            }
            val formatter = SimpleDateFormat(pattern, Locale.getDefault())
            time = formatter.format(Date())
            delay(1000)
        }
    }

    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = time,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FloatingClockSwitch(
    isChecked: Boolean,
    setFloatingEnabled: (Boolean) -> Unit
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
        if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(
                context,
                "Overlay permission not granted.",
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

    val onToggle: (Boolean) -> Unit = onToggle@{ checked ->
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
                    "Notification permission is required to start the floating clock.",
                    Toast.LENGTH_LONG
                ).show()
                return@onToggle
            }

            if (AlarmScheduler.needsExactAlarmPermission(context)) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // Ignore if settings screen is not available.
                }
            }

            if (!Settings.canDrawOverlays(context)) {
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
                    "Overlay permission not granted.",
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
        title = "Show Floating Clock",
        onClick = { onToggle(!isChecked) }
    ) {
        Switch(
            checked = isChecked,
            onCheckedChange = onToggle
        )
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
fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    trailing: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = trailing
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FloatingClockPositionRow() {
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
        title = "Clock position",
        onClick = { showDialog = true }
    ) {
        Text(text = currentPosition.label)
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(modifier = Modifier.padding(24.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Select clock position")
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
                            .border(2.dp, Color.White),
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
                                    onClick = { applySelection(FloatingClockPosition.TOP_LEFT) }
                                )
                                PositionButton(
                                    selected = dialogPosition == FloatingClockPosition.TOP_RIGHT,
                                    onClick = { applySelection(FloatingClockPosition.TOP_RIGHT) }
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                PositionButton(
                                    selected = dialogPosition == FloatingClockPosition.BOTTOM_LEFT,
                                    onClick = { applySelection(FloatingClockPosition.BOTTOM_LEFT) }
                                )
                                PositionButton(
                                    selected = dialogPosition == FloatingClockPosition.BOTTOM_RIGHT,
                                    onClick = { applySelection(FloatingClockPosition.BOTTOM_RIGHT) }
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(onClick = { showDialog = false }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FloatingClockFormatRow() {
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
        title = "Clock format",
        onClick = { showDialog = true }
    ) {
        val label = if (use24h) "24-hour" else "12-hour"
        Text(text = label)
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(modifier = Modifier.padding(24.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Clock format")
                    SettingsRow(
                        title = "Show seconds",
                        onClick = {
                            val newValue = !showSeconds
                            showSeconds = newValue
                            prefs.edit().putBoolean(PREF_CLOCK_SHOW_SECONDS, newValue).apply()
                            notifyClockSecondsChanged(context, newValue)
                        }
                    ) {
                        Switch(
                            checked = showSeconds,
                            onCheckedChange = { checked ->
                                showSeconds = checked
                                prefs.edit().putBoolean(PREF_CLOCK_SHOW_SECONDS, checked).apply()
                                notifyClockSecondsChanged(context, checked)
                            }
                        )
                    }
                    SettingsRow(
                        title = "Use 24-hour format",
                        onClick = {
                            val newValue = !use24h
                            use24h = newValue
                            prefs.edit().putBoolean(PREF_CLOCK_USE_24H, newValue).apply()
                            notifyClockFormatChanged(context, newValue)
                        }
                    ) {
                        Switch(
                            checked = use24h,
                            onCheckedChange = { checked ->
                                use24h = checked
                                prefs.edit().putBoolean(PREF_CLOCK_USE_24H, checked).apply()
                                notifyClockFormatChanged(context, checked)
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(onClick = { showDialog = false }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PositionButton(selected: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(if (selected) "X" else " ")
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AlarmConfigRow() {
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
    var showDialog by remember { mutableStateOf(false) }
    var dialogHour by remember { mutableStateOf(0) }
    var dialogMinute by remember { mutableStateOf(0) }
    var alarmActive by remember { mutableStateOf(false) }

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(showDialog) {
        if (!showDialog) return@LaunchedEffect
        val calendar = Calendar.getInstance()
        dialogHour = if (alarmHour >= 0) alarmHour else calendar.get(Calendar.HOUR_OF_DAY)
        dialogMinute = if (alarmMinute >= 0) alarmMinute else calendar.get(Calendar.MINUTE)
    }

    SettingsRow(
        title = "Alarm",
        onClick = {
            if (alarmActive) {
                AlarmScheduler.cancelAlarm(context)
                alarmEnabled = false
                prefs.edit()
                    .putBoolean(PREF_ALARM_ENABLED, false)
                    .apply()
            } else {
                showDialog = true
            }
        }
    ) {
        Switch(
            checked = alarmEnabled,
            onCheckedChange = { checked ->
                if (checked && (alarmHour < 0 || alarmMinute < 0)) {
                    showDialog = true
                    return@Switch
                }
                if (checked) {
                    if (AlarmScheduler.needsExactAlarmPermission(context)) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Ignore if settings screen is not available.
                        }
                    }
                    val success = AlarmScheduler.scheduleAlarm(
                        context,
                        alarmHour,
                        alarmMinute
                    )
                    if (!success) {
                        Toast.makeText(
                            context,
                            "Alarm scheduling is not available on this device.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Switch
                    }
                } else {
                    AlarmScheduler.cancelAlarm(context)
                }
                alarmEnabled = checked
                prefs.edit().putBoolean(PREF_ALARM_ENABLED, alarmEnabled).apply()
            }
        )
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(modifier = Modifier.padding(24.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Configure alarm")
                    Row(
                        modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(onClick = { dialogHour = (dialogHour + 1) % 24 }) {
                                Text("+")
                            }
                            Text(
                                text = String.format("%02d", dialogHour),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Button(onClick = { dialogHour = (dialogHour + 23) % 24 }) {
                                Text("-")
                            }
                        }
                        Text(
                            text = ":",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(onClick = { dialogMinute = (dialogMinute + 1) % 60 }) {
                                Text("+")
                            }
                            Text(
                                text = String.format("%02d", dialogMinute),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Button(onClick = { dialogMinute = (dialogMinute + 59) % 60 }) {
                                Text("-")
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(onClick = {
                            if (AlarmScheduler.needsExactAlarmPermission(context)) {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    // Ignore if settings screen is not available.
                                }
                            }
                            val success = AlarmScheduler.scheduleAlarm(
                                context,
                                dialogHour,
                                dialogMinute
                            )
                            if (!success) {
                                Toast.makeText(
                                    context,
                                    "Alarm scheduling is not available on this device.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@Button
                            }
                            alarmHour = dialogHour
                            alarmMinute = dialogMinute
                            alarmEnabled = true
                            prefs.edit()
                                .putBoolean(PREF_ALARM_ENABLED, true)
                                .putInt(PREF_ALARM_HOUR, alarmHour)
                                .putInt(PREF_ALARM_MINUTE, alarmMinute)
                                .apply()
                            showDialog = false
                        }) {
                            Text("ACTIVATE")
                        }
                        Button(onClick = {
                            AlarmScheduler.cancelAlarm(context)
                            alarmEnabled = false
                            prefs.edit()
                                .putBoolean(PREF_ALARM_ENABLED, false)
                                .apply()
                            showDialog = false
                        }) {
                            Text("CANCEL")
                        }
                    }
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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FloatingClockTheme {
        MainScreen()
    }
}
