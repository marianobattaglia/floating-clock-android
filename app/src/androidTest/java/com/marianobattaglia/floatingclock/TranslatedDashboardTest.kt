@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class, androidx.compose.ui.test.ExperimentalTestApi::class)

package com.marianobattaglia.floatingclock

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.Switch
import com.marianobattaglia.floatingclock.ui.theme.FloatingClockTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TranslatedDashboardTest {
    @get:Rule val compose = createComposeRule()
    private val target = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs get() = target.getSharedPreferences("localization_ui_$PREFS_NAME", Context.MODE_PRIVATE)
    private fun SemanticsNodeInteraction.selectWithRemote(): SemanticsNodeInteraction {
        val nodeId = fetchSemanticsNode().id
        performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
        return compose.onNode(SemanticsMatcher("same control after preview changes") { it.id == nodeId })
            .performKeyInput { pressKey(Key.DirectionCenter) }
    }

    private fun text(id: Int) = target.getString(id)
    private val floating = mutableStateOf(true)
    private var actions = 0

    @Before fun setUp() {
        prefs.edit().clear().putBoolean(PREF_FLOATING_ENABLED, true).commit()
        compose.setContent {
            val context = LocalContext.current
            val isolatedContext = remember(context) {
                object : ContextWrapper(context) {
                    override fun getSharedPreferences(name: String, mode: Int) =
                        target.getSharedPreferences("localization_ui_$name", mode)
                }
            }
            CompositionLocalProvider(LocalContext provides isolatedContext) {
                FloatingClockTheme {
                    androidx.tv.material3.Surface(modifier = Modifier.fillMaxSize()) {
                        ClockDashboard(
                            floatingEnabled = floating.value,
                            clock = { _, _ -> },
                            proContent = { androidx.tv.material3.Text(text(R.string.pro_title)) }
                        ) {
                            SettingsRow(text(R.string.clock_show_floating), { actions++ }, setting = DashboardSetting.Floating) {
                                val select = LocalPreviewSelection.current
                                Switch(checked = floating.value, onCheckedChange = {
                                    select(DashboardSetting.Floating)
                                    floating.value = it
                                    actions++
                                })
                            }
                            FloatingClockPositionRow()
                            FloatingClockFormatRow()
                            FloatingClockAppearanceRow(hasPro = true)
                            AlarmConfigRow(isPro = true)
                        }
                    }
                }
            }
        }
    }

    @After fun tearDown() { prefs.edit().clear().commit() }

    @Test fun translatedRowsKeepPreviewIdentityAndOriginalActions() {
        // Focusing with a remote must select a preview without invoking its action.
        compose.onAllNodesWithText(text(R.string.clock_position))[0].performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithText(text(R.string.preview_position)).assertIsDisplayed()
        assertEquals(0, actions)
        compose.onAllNodesWithText(text(R.string.clock_position))[0].performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithText(text(R.string.clock_select_position)).assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.clock_position_top_left)).assertIsSelected()
        compose.onNodeWithContentDescription(text(R.string.clock_position_bottom_right)).selectWithRemote()
        compose.onNodeWithContentDescription(text(R.string.clock_position_bottom_right)).assertIsSelected()
        compose.onNodeWithContentDescription(text(R.string.clock_position_top_left)).assertIsNotSelected()
        compose.runOnIdle { assertEquals("bottom_right", prefs.getString(PREF_CLOCK_POSITION, null)) }
        compose.onNodeWithText(text(R.string.action_ok)).selectWithRemote()
        compose.onNodeWithText(text(R.string.clock_select_position)).assertDoesNotExist()
        compose.onAllNodesWithText(text(R.string.clock_position_bottom_right))[0].assertExists()
        compose.onNodeWithText(text(R.string.nav_pro)).selectWithRemote()
        compose.onNodeWithText(text(R.string.pro_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.nav_home)).selectWithRemote()
        compose.onAllNodesWithText(text(R.string.clock_show_floating))[0].assertExists()
        assertEquals(0, actions)
        compose.onAllNodes(isToggleable())[0].performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        compose.runOnIdle { assertEquals(1, actions) }
        compose.onNodeWithText(text(R.string.status_off)).assertExists()
    }

    @Test fun appearanceDescriptionsAndBackRetainSavedValueAndFocus() {
        compose.onNodeWithText(text(R.string.clock_appearance)).performScrollTo().selectWithRemote()
        compose.onNodeWithContentDescription(text(R.string.appearance_increase_edge)).selectWithRemote()
        compose.runOnIdle { assertEquals(20, prefs.getInt(PREF_CLOCK_EDGE_DISTANCE_DP, -1)) }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithContentDescription(text(R.string.appearance_increase_edge)).assertDoesNotExist()
        compose.onAllNodes(hasText(text(R.string.clock_appearance)) and isFocusable())[0].assertIsFocused()
        compose.runOnIdle { assertEquals(20, prefs.getInt(PREF_CLOCK_EDGE_DISTANCE_DP, -1)) }
    }
    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("localizationScreenshots") != "true") return
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = java.io.File(instrumentation.targetContext.filesDir, "localization")
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create screenshot directory: $directory" }
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        try {
            java.io.FileOutputStream(java.io.File(directory, "$name.png")).use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        } finally { bitmap.recycle() }
    }

    @Test fun everyTranslatedDialogRemainsReachableWithTheRemote() {
        capture("dashboard")
        for ((row, dialog, descriptionIds) in listOf(
            Triple(R.string.clock_position, R.string.clock_select_position, listOf(
                R.string.clock_position_top_left, R.string.clock_position_top_right,
                R.string.clock_position_bottom_left, R.string.clock_position_bottom_right)),
            Triple(R.string.clock_format, R.string.clock_format, emptyList()),
            Triple(R.string.clock_appearance, R.string.clock_appearance, listOf(
                R.string.appearance_decrease_edge, R.string.appearance_increase_edge,
                R.string.appearance_decrease_size, R.string.appearance_increase_size,
                R.string.appearance_decrease_spacing, R.string.appearance_increase_spacing)),
            Triple(R.string.alarm_title, R.string.alarm_configure, listOf(
                R.string.alarm_increase_hour, R.string.alarm_decrease_hour,
                R.string.alarm_increase_minute, R.string.alarm_decrease_minute))
        )) {
            compose.onNode(hasText(text(row)) and hasClickAction()).performScrollTo().selectWithRemote()
            compose.onNode(hasText(text(dialog)) and hasAnyAncestor(isDialog())).assertIsDisplayed()
            capture("dialog-${target.resources.getResourceEntryName(row)}-top")
            for (description in descriptionIds) {
                compose.onNodeWithContentDescription(text(description)).performScrollTo()
                    .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
                compose.onNodeWithContentDescription(text(description)).assertIsDisplayed()
                    .assertHasClickAction()
                    .assert(SemanticsMatcher.expectValue(
                        androidx.compose.ui.semantics.SemanticsProperties.Role,
                        androidx.compose.ui.semantics.Role.Button))
            }
            if (row == R.string.clock_format) {
                for (label in listOf(R.string.clock_show_seconds, R.string.clock_use_24_hour)) {
                    compose.onNodeWithText(text(label)).performScrollTo().assertIsDisplayed()
                }
            }
            val close = if (row == R.string.alarm_title) R.string.action_cancel else R.string.action_ok
            compose.onNodeWithText(text(close)).performScrollTo().assertIsDisplayed()
            capture("dialog-${target.resources.getResourceEntryName(row)}-bottom")
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            compose.onNode(isDialog()).assertDoesNotExist()
        }
        // The traversal itself must never schedule an alarm or modify appearance.
        compose.runOnIdle {
            assertEquals(false, prefs.getBoolean(PREF_ALARM_ENABLED, false))
            assertEquals(ClockAppearance(), prefs.readClockAppearance())
        }
    }

}
