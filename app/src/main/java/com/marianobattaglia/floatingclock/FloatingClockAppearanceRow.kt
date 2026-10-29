package com.marianobattaglia.floatingclock

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
import androidx.compose.ui.res.stringResource
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
fun FloatingClockAppearanceRow(hasPro: Boolean, isPhone: Boolean = false) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var appearance by remember { mutableStateOf(prefs.readClockAppearance()) }
    var showDialog by remember { mutableStateOf(false) }
    var restoreFocus by remember { mutableStateOf(false) }
    val rowFocus = remember { FocusRequester() }
    val initialFocus = remember { FocusRequester() }
    val openPro = LocalOpenPro.current

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
        title = stringResource(R.string.clock_appearance),
        setting = DashboardSetting.Appearance,
        isPhone = isPhone,
        modifier = Modifier.focusRequester(rowFocus),
        onClick = {
            if (!hasPro) {
                openPro()
                return@SettingsRow
            }
            appearance = prefs.readClockAppearance()
            restoreFocus = true
            showDialog = true
        }
    ) {
        if (hasPro) {
            Text(stringResource(R.string.appearance_summary, appearance.edgeDistanceDp, appearance.scalePercent, appearance.paddingPercent))
        } else {
            Text(stringResource(R.string.pro_locked_label))
        }
    }
    if (showDialog) {
        val close = { showDialog = false }
        SettingsDialog(onDismissRequest = close, isPhone = isPhone) {
            Text(stringResource(R.string.clock_appearance), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            AppearanceStepper(
                label = stringResource(R.string.appearance_edge_distance),
                decreaseLabel = stringResource(R.string.appearance_decrease_edge),
                increaseLabel = stringResource(R.string.appearance_increase_edge),
                value = stringResource(R.string.value_distance_dp, appearance.edgeDistanceDp),
                isPhone = isPhone,
                decreaseModifier = Modifier.focusRequester(initialFocus),
                onAdjust = { increase ->
                    save(appearance.copy(edgeDistanceDp = ClockAppearanceLimits.edgeDistance.adjust(
                        appearance.edgeDistanceDp, increase
                    )))
                }
            )
            AppearanceStepper(
                label = stringResource(R.string.appearance_overall_size),
                decreaseLabel = stringResource(R.string.appearance_decrease_size),
                increaseLabel = stringResource(R.string.appearance_increase_size),
                value = stringResource(R.string.value_percent, appearance.scalePercent),
                isPhone = isPhone,
                onAdjust = { increase ->
                    save(appearance.copy(scalePercent = ClockAppearanceLimits.overallSize.adjust(
                        appearance.scalePercent, increase
                    )))
                }
            )
            AppearanceStepper(
                label = stringResource(R.string.appearance_internal_spacing),
                decreaseLabel = stringResource(R.string.appearance_decrease_spacing),
                increaseLabel = stringResource(R.string.appearance_increase_spacing),
                value = stringResource(R.string.value_percent, appearance.paddingPercent),
                isPhone = isPhone,
                onAdjust = { increase ->
                    save(appearance.copy(paddingPercent = ClockAppearanceLimits.internalSpacing.adjust(
                        appearance.paddingPercent, increase
                    )))
                }
            )
            Text(stringResource(R.string.appearance_help))
            FormFactorButton(isPhone = isPhone, onClick = close) { Text(stringResource(R.string.action_ok)) }
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
    decreaseLabel: String,
    increaseLabel: String,
    isPhone: Boolean,
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
            FormFactorButton(
                isPhone = isPhone,
                modifier = decreaseModifier.semantics { contentDescription = decreaseLabel },
                onClick = { onAdjust(false) }
            ) { Text(stringResource(R.string.symbol_decrease)) }
            Text(value, modifier = Modifier.width(80.dp))
            FormFactorButton(
                isPhone = isPhone,
                modifier = Modifier.semantics { contentDescription = increaseLabel },
                onClick = { onAdjust(true) }
            ) { Text(stringResource(R.string.symbol_increase)) }
        }
    }
}

private fun notifyClockAppearanceChanged(context: Context) {
    context.sendBroadcast(Intent(ACTION_CLOCK_APPEARANCE_CHANGED).setPackage(context.packageName))
}
