package com.example.floatingclock

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FloatingClockAppearanceRow() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var appearance by remember { mutableStateOf(prefs.readClockAppearance()) }
    var showDialog by remember { mutableStateOf(false) }
    var restoreFocus by remember { mutableStateOf(false) }
    val rowFocus = remember { FocusRequester() }
    val initialFocus = remember { FocusRequester() }

    DisposableEffect(lifecycleOwner, prefs) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) appearance = prefs.readClockAppearance()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(showDialog) {
        if (!showDialog && restoreFocus) {
            withFrameNanos { }
            rowFocus.requestFocus()
            restoreFocus = false
        }
    }
    val save: (ClockAppearance) -> Unit = { updated ->
        if (updated != appearance) {
            prefs.edit()
                .putInt(PREF_CLOCK_EDGE_DISTANCE_DP, updated.edgeDistanceDp)
                .putInt(PREF_CLOCK_SCALE_PERCENT, updated.scalePercent)
                .putInt(PREF_CLOCK_PADDING_PERCENT, updated.paddingPercent)
                .apply()
            appearance = updated
            notifyClockAppearanceChanged(context)
        }
    }
    SettingsRow(
        title = "Clock appearance",
        modifier = Modifier.focusRequester(rowFocus),
        onClick = {
            appearance = prefs.readClockAppearance()
            restoreFocus = true
            showDialog = true
        }
    ) {
        Text("${appearance.edgeDistanceDp} dp / ${appearance.scalePercent}% / ${appearance.paddingPercent}%")
    }
    if (showDialog) {
        val close = { showDialog = false }
        SettingsDialog(onDismissRequest = close) {
            Text("Clock appearance", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            AppearanceStepper(
                label = "Edge distance",
                value = "${appearance.edgeDistanceDp} dp",
                decreaseModifier = Modifier.focusRequester(initialFocus),
                onAdjust = { increase ->
                    save(appearance.copy(edgeDistanceDp = ClockAppearanceLimits.edgeDistance.adjust(
                        appearance.edgeDistanceDp, increase
                    )))
                }
            )
            AppearanceStepper(
                label = "Overall size",
                value = "${appearance.scalePercent}%",
                onAdjust = { increase ->
                    save(appearance.copy(scalePercent = ClockAppearanceLimits.overallSize.adjust(
                        appearance.scalePercent, increase
                    )))
                }
            )
            AppearanceStepper(
                label = "Internal spacing",
                value = "${appearance.paddingPercent}%",
                onAdjust = { increase ->
                    save(appearance.copy(paddingPercent = ClockAppearanceLimits.internalSpacing.adjust(
                        appearance.paddingPercent, increase
                    )))
                }
            )
            Text("100% keeps the original size or spacing. Changes are saved immediately.")
            Button(onClick = close) { Text("OK") }
            LaunchedEffect(Unit) {
                withFrameNanos { }
                initialFocus.requestFocus()
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppearanceStepper(
    label: String,
    value: String,
    decreaseModifier: Modifier = Modifier,
    onAdjust: (Boolean) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Keep both buttons focusable even at a bound; the range makes it a no-op.
            Button(
                modifier = decreaseModifier.semantics { contentDescription = "Decrease $label" },
                onClick = { onAdjust(false) }
            ) { Text("−") }
            Text(value, modifier = Modifier.width(80.dp))
            Button(
                modifier = Modifier.semantics { contentDescription = "Increase $label" },
                onClick = { onAdjust(true) }
            ) { Text("+") }
        }
    }
}

private fun notifyClockAppearanceChanged(context: Context) {
    context.sendBroadcast(Intent(ACTION_CLOCK_APPEARANCE_CHANGED).setPackage(context.packageName))
}
