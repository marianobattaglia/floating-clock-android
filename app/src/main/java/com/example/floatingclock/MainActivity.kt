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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.example.floatingclock.ui.theme.FloatingClockTheme
import java.util.Calendar
import kotlinx.coroutines.delay
import android.content.BroadcastReceiver
import android.content.IntentFilter

private const val PREFS_NAME = "floating_clock_prefs"
private const val PREF_FLOATING_ENABLED = "floating_clock_enabled"
private const val PREF_ALARM_ENABLED = "alarm_enabled"
private const val PREF_ALARM_HOUR = "alarm_hour"
private const val PREF_ALARM_MINUTE = "alarm_minute"

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
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Clock()
        FloatingClockSwitch()
        AlarmConfigRow()
    }
}


@Composable
fun Clock(modifier: Modifier = Modifier) {
    var time by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)
            time = String.format("%02d:%02d:%02d", hour, minute, second)
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
fun FloatingClockSwitch() {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var isChecked by remember {
        mutableStateOf(prefs.getBoolean(PREF_FLOATING_ENABLED, false))
    }
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
                "Permiso de superposicion no concedido.",
                Toast.LENGTH_LONG
            ).show()
            return@rememberLauncherForActivityResult
        }

        isChecked = true
        prefs.edit().putBoolean(PREF_FLOATING_ENABLED, true).apply()
        val intent = Intent(context, FloatingClockService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Show Floating Clock")
        Switch(
            checked = isChecked,
            onCheckedChange = {
                if (it) {
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
                            "Necesito permiso de notificaciones para iniciar el reloj flotante.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Switch
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
                            "Permiso de superposicion no concedido.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Switch
                    }
                }

                isChecked = it
                prefs.edit().putBoolean(PREF_FLOATING_ENABLED, isChecked).apply()
                val intent = Intent(context, FloatingClockService::class.java)
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } else {
                    context.stopService(intent)
                }
            }
        )
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable {
                if (alarmActive) {
                    AlarmScheduler.cancelAlarm(context)
                    alarmEnabled = false
                    prefs.edit()
                        .putBoolean(PREF_ALARM_ENABLED, false)
                        .apply()
                } else {
                    showDialog = true
                }
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Alarm")
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
                            "No se puede programar la alarma en este dispositivo.",
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
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = "Configurar alarma")
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
                                    "No se puede programar la alarma en este dispositivo.",
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
                            Text("ACTIVAR")
                        }
                        Button(onClick = {
                            AlarmScheduler.cancelAlarm(context)
                            alarmEnabled = false
                            prefs.edit()
                                .putBoolean(PREF_ALARM_ENABLED, false)
                                .apply()
                            showDialog = false
                        }) {
                            Text("CANCELAR")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FloatingClockTheme {
        MainScreen()
    }
}
