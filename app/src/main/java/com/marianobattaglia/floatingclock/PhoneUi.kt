package com.marianobattaglia.floatingclock

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Button as TvButton
import com.marianobattaglia.floatingclock.ui.theme.DashboardColors
import androidx.compose.material3.darkColorScheme

private enum class PhoneDestination(val labelRes: Int, val icon: String) {
    Home(R.string.nav_home, "home"),
    Pro(R.string.nav_pro, "star"),
    Debug(R.string.nav_debug, "debug")
}

@Composable
internal fun FloatingClockPhoneTheme(content: @Composable () -> Unit) {
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
            outline = DashboardColors.Border
        ),
        content = content
    )
}

@Composable
internal fun PhoneDashboard(
    floatingEnabled: Boolean,
    clock: @Composable (Boolean, (String) -> Unit) -> Unit,
    debugEnabled: Boolean,
    proContent: @Composable () -> Unit,
    debugContent: @Composable () -> Unit,
    settings: @Composable ColumnScope.() -> Unit
) {
    var destination by rememberSaveable { mutableStateOf(PhoneDestination.Home) }
    var clockTime by remember { mutableStateOf("") }
    val destinations = PhoneDestination.entries.filter {
        debugEnabled || it != PhoneDestination.Debug
    }

    BackHandler(enabled = destination != PhoneDestination.Home) {
        destination = PhoneDestination.Home
    }
    LaunchedEffect(debugEnabled) {
        if (!debugEnabled && destination == PhoneDestination.Debug) {
            destination = PhoneDestination.Home
        }
    }

    Scaffold(
        containerColor = DashboardColors.Background,
        bottomBar = {
            NavigationBar(containerColor = DashboardColors.Rail) {
                destinations.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = {
                            DashboardIcon(
                                item.icon,
                                Modifier.size(22.dp),
                                if (destination == item) DashboardColors.Accent else DashboardColors.Muted
                            )
                        },
                        label = { Text(stringResource(item.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DashboardColors.Accent,
                            selectedTextColor = DashboardColors.Accent,
                            unselectedIconColor = DashboardColors.Muted,
                            unselectedTextColor = DashboardColors.Muted,
                            indicatorColor = DashboardColors.Focused
                        )
                    )
                }
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding).imePadding()
        ) {
            when (destination) {
                PhoneDestination.Home -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(R.string.dashboard_tagline),
                                color = DashboardColors.Muted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        clock(!floatingEnabled) { clockTime = it }
                    }
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompositionLocalProvider(LocalOpenPro provides {
                            destination = PhoneDestination.Pro
                        }) {
                            settings()
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                PhoneDestination.Pro -> Box(
                    modifier = Modifier.fillMaxSize().widthIn(max = 640.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) { proContent() }
                PhoneDestination.Debug -> if (debugEnabled) {
                    Box(
                        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) { debugContent() }
                }
            }
        }
    }
}

@Composable
internal fun PhoneSettingsRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardColors.Surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = DashboardColors.Muted)
                }
            }
            androidx.tv.material3.ProvideTextStyle(
                androidx.tv.material3.MaterialTheme.typography.bodyMedium.copy(color = DashboardColors.Text)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun FormFactorButton(
    isPhone: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    if (isPhone) {
        Button(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled) {
            androidx.tv.material3.ProvideTextStyle(
                androidx.tv.material3.MaterialTheme.typography.labelLarge.copy(color = DashboardColors.Rail)
            ) {
                content()
            }
        }
    } else {
        TvButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
    }
}

@Composable
internal fun PhoneProAccessContent(
    state: ProEntitlementUiState,
    onPurchase: (Activity) -> Unit,
    onOpenReviewAccess: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.pro_title), style = MaterialTheme.typography.headlineMedium)
        Surface(shape = RoundedCornerShape(14.dp), color = DashboardColors.Surface) {
            Text(
                stringResource(
                    R.string.pro_effective_status,
                    stringResource(statusLabel(state.effectiveStatus))
                ),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(stringResource(R.string.pro_benefits_title), style = MaterialTheme.typography.titleMedium)
        Text("• ${stringResource(R.string.pro_benefit_appearance)}")
        Text("• ${stringResource(R.string.pro_benefit_alarm)}")
        Text(stringResource(R.string.pro_purchase_explanation), color = DashboardColors.Muted)
        state.productPrice?.let { Text(stringResource(R.string.pro_lifetime_price, it)) }
        Button(
            onClick = { activity?.let(onPurchase) },
            enabled = activity != null && state.effectiveStatus != ActualProStatus.PRO,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) { Text(stringResource(R.string.pro_purchase_action)) }
        billingFeedback(state.feedback)?.let { message ->
            Text(stringResource(message), color = DashboardColors.Muted)
        }
        Text(stringResource(R.string.pro_restore_note), color = DashboardColors.Muted)
        if (state.reviewAccessGranted) {
            Text(stringResource(R.string.review_access_active), color = DashboardColors.Accent)
        } else if (state.reviewAccessConfigured) {
            Button(onClick = onOpenReviewAccess, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.review_access_action))
            }
        }
    }
}

@Composable
internal fun PhoneProDebugContent(
    state: ProEntitlementUiState,
    onOverrideSelected: (DebugProOverride) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.debug_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.debug_actual_status, stringResource(statusLabel(state.actualStatus))),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            stringResource(R.string.pro_effective_status, stringResource(statusLabel(state.effectiveStatus))),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(stringResource(R.string.debug_override_title), fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PhoneOverrideButton(DebugProOverride.FREE, state.debugOverride, R.string.debug_override_free, onOverrideSelected)
            PhoneOverrideButton(DebugProOverride.PRO, state.debugOverride, R.string.debug_override_pro, onOverrideSelected)
            PhoneOverrideButton(DebugProOverride.USE_PLAY, state.debugOverride, R.string.debug_override_play, onOverrideSelected)
        }
        Text(stringResource(R.string.debug_override_note), color = DashboardColors.Muted)
    }
}

@Composable
private fun PhoneOverrideButton(
    value: DebugProOverride,
    selected: DebugProOverride,
    labelRes: Int,
    onSelected: (DebugProOverride) -> Unit
) {
    Button(
        onClick = { onSelected(value) },
        enabled = value != selected,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        contentPadding = ButtonDefaults.ContentPadding
    ) { Text(stringResource(labelRes), fontSize = 12.sp) }
}

@Composable
internal fun PhoneReviewerAccessDialog(
    onDismissRequest: () -> Unit,
    onSubmit: (String) -> ReviewAccessResult
) {
    var code by rememberSaveable { mutableStateOf("") }
    var invalidCode by rememberSaveable { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.review_access_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.review_access_instructions), color = DashboardColors.Muted)
                OutlinedTextField(
                    value = code,
                    onValueChange = { updated ->
                        code = updated.filter(Char::isLetterOrDigit).uppercase().take(REVIEW_ACCESS_CODE_LENGTH)
                        invalidCode = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.review_access_code_placeholder)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii
                    )
                )
                if (invalidCode) Text(stringResource(R.string.review_access_invalid), color = MaterialTheme.colorScheme.error)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            when (onSubmit(code)) {
                                ReviewAccessResult.GRANTED -> onDismissRequest()
                                ReviewAccessResult.INVALID,
                                ReviewAccessResult.UNAVAILABLE -> invalidCode = true
                            }
                        },
                        enabled = code.length == REVIEW_ACCESS_CODE_LENGTH,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) { Text(stringResource(R.string.review_access_activate)) }
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text(stringResource(R.string.action_cancel)) }
                }
            }
        }
    }
}

private const val REVIEW_ACCESS_CODE_LENGTH = 32
