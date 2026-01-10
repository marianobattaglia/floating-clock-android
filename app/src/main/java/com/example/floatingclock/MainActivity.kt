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
import androidx.core.content.ContextCompat
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.example.floatingclock.ui.theme.FloatingClockTheme
import java.util.Calendar
import kotlinx.coroutines.delay

private const val PREFS_NAME = "floating_clock_prefs"
private const val PREF_FLOATING_ENABLED = "floating_clock_enabled"

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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FloatingClockTheme {
        MainScreen()
    }
}
