package com.marianobattaglia.floatingclock

import android.app.Activity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.marianobattaglia.floatingclock.ui.theme.DashboardColors

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ProAccessContent(
    state: ProEntitlementUiState,
    onPurchase: (Activity) -> Unit,
    onOpenReviewAccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(stringResource(R.string.pro_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(
                R.string.pro_effective_status,
                stringResource(statusLabel(state.effectiveStatus))
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(stringResource(R.string.pro_benefits_title), fontWeight = FontWeight.SemiBold)
        Text("• ${stringResource(R.string.pro_benefit_appearance)}")
        Text("• ${stringResource(R.string.pro_benefit_alarm)}")
        Text(stringResource(R.string.pro_purchase_explanation))
        state.productPrice?.let { price ->
            Text(stringResource(R.string.pro_lifetime_price, price))
        }
        Button(
            onClick = { activity?.let(onPurchase) },
            enabled = activity != null && state.effectiveStatus != ActualProStatus.PRO
        ) {
            Text(stringResource(R.string.pro_purchase_action))
        }
        billingFeedback(state.feedback)?.let { message ->
            Text(stringResource(message))
        }
        Text(stringResource(R.string.pro_restore_note))
        if (state.reviewAccessGranted) {
            Text(stringResource(R.string.review_access_active))
        } else if (state.reviewAccessConfigured) {
            Button(onClick = onOpenReviewAccess) {
                Text(stringResource(R.string.review_access_action))
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ReviewerAccessDialog(
    onDismissRequest: () -> Unit,
    onSubmit: (String) -> ReviewAccessResult
) {
    var code by remember { mutableStateOf("") }
    var invalidCode by remember { mutableStateOf(false) }
    var codeFieldFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    SettingsDialog(onDismissRequest = onDismissRequest) {
        Text(
            stringResource(R.string.review_access_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(stringResource(R.string.review_access_instructions))
        BasicTextField(
            value = code,
            onValueChange = { updated ->
                code = updated.filter(Char::isLetterOrDigit)
                    .uppercase()
                    .take(REVIEW_ACCESS_CODE_LENGTH)
                invalidCode = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { codeFieldFocused = it.isFocused }
                .border(
                    width = if (codeFieldFocused) 2.dp else 1.dp,
                    color = if (codeFieldFocused) DashboardColors.Accent else DashboardColors.Border,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(DashboardColors.Accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (code.isEmpty()) {
                        Text(
                            stringResource(R.string.review_access_code_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (invalidCode) {
            Text(stringResource(R.string.review_access_invalid))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    when (onSubmit(code)) {
                        ReviewAccessResult.GRANTED -> onDismissRequest()
                        ReviewAccessResult.INVALID,
                        ReviewAccessResult.UNAVAILABLE -> invalidCode = true
                    }
                },
                enabled = code.length == REVIEW_ACCESS_CODE_LENGTH
            ) {
                Text(stringResource(R.string.review_access_activate))
            }
            Button(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}

private const val REVIEW_ACCESS_CODE_LENGTH = 32

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ProDebugContent(
    state: ProEntitlementUiState,
    onOverrideSelected: (DebugProOverride) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DebugOverrideButton(DebugProOverride.FREE, state.debugOverride, R.string.debug_override_free, onOverrideSelected)
            DebugOverrideButton(DebugProOverride.PRO, state.debugOverride, R.string.debug_override_pro, onOverrideSelected)
            DebugOverrideButton(DebugProOverride.USE_PLAY, state.debugOverride, R.string.debug_override_play, onOverrideSelected)
        }
        Text(stringResource(R.string.debug_override_note))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DebugOverrideButton(
    override: DebugProOverride,
    selected: DebugProOverride,
    labelRes: Int,
    onSelected: (DebugProOverride) -> Unit
) {
    Button(onClick = { onSelected(override) }, enabled = selected != override) {
        Text(stringResource(labelRes))
    }
}

@Composable
private fun statusLabel(status: ActualProStatus): Int = when (status) {
    ActualProStatus.CHECKING -> R.string.pro_status_checking
    ActualProStatus.FREE -> R.string.pro_status_free
    ActualProStatus.PRO -> R.string.pro_status_pro
    ActualProStatus.UNAVAILABLE -> R.string.pro_status_unavailable
}

private fun billingFeedback(feedback: ProBillingFeedback): Int? = when (feedback) {
    ProBillingFeedback.NONE -> null
    ProBillingFeedback.PENDING -> R.string.pro_feedback_pending
    ProBillingFeedback.CANCELED -> R.string.pro_feedback_canceled
    ProBillingFeedback.ERROR -> R.string.pro_feedback_error
    ProBillingFeedback.UNAVAILABLE -> R.string.pro_feedback_unavailable
    ProBillingFeedback.PRODUCT_UNAVAILABLE -> R.string.pro_feedback_product_unavailable
}
