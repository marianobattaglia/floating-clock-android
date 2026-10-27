package com.marianobattaglia.floatingclock

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Locale

class LocalizationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun localized(tags: String): Context {
        val configuration = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= 24) {
            configuration.setLocales(LocaleList.forLanguageTags(tags))
        } else {
            configuration.setLocale(Locale.forLanguageTag(tags.substringBefore(',')))
        }
        return context.createConfigurationContext(configuration)
    }

    @Test
    fun systemLanguagesRegionsAndFallbackResolveCompleteCatalogs() {
        for ((tag, expected) in mapOf(
            "en-US" to "Preview", "en-GB" to "Preview",
            "es-AR" to "Vista previa", "es-ES" to "Vista previa",
            "pt-BR" to "Pré-visualização", "pt-PT" to "Pré-visualização",
            "fr-FR" to "Aperçu", "fr-CA" to "Aperçu",
            "de-DE" to "Preview", "ja-JP" to "Preview"
        )) {
            val resources = localized(tag)
            assertEquals(tag, expected, resources.getString(R.string.preview_title))
            assertFalse(resources.getString(R.string.appearance_summary, 16, 100, 100).contains("%1$"))
            assertEquals("FloatingClock", resources.getString(R.string.app_name))
        }
    }

    @Test
    fun orderedLanguageListsHonorEnglishAndSecondarySupportedLanguages() {
        if (Build.VERSION.SDK_INT < 24) return
        for ((tags, expected) in mapOf(
            "de-DE,fr-FR" to "Aperçu", "es-AR,fr-FR" to "Vista previa",
            "en-GB,fr-FR" to "Preview", "de-DE,ja-JP" to "Preview"
        )) assertEquals(tags, expected, localized(tags).getString(R.string.preview_title))
    }

    @Test
    fun currentConfigurationDeterminesFormattingWithoutChangingSavedSettings() {
        assertEquals("pt", configurationLocale(localized("pt-BR").resources.configuration).language)
        val prefs = context.getSharedPreferences("localization_regression", Context.MODE_PRIVATE)
        try {
            prefs.edit().putString(PREF_CLOCK_POSITION, "bottom_right")
                .putBoolean(PREF_CLOCK_USE_24H, false).putBoolean(PREF_CLOCK_SHOW_SECONDS, true)
                .putInt(PREF_ALARM_HOUR, 23).putInt(PREF_ALARM_MINUTE, 59).commit()
            val before = prefs.all.toMap()
            for (tag in listOf("en", "es", "pt", "fr", "de")) {
                localized(tag).getString(FloatingClockPosition.BOTTOM_RIGHT.labelRes)
                assertEquals(before, prefs.all)
                assertEquals(FloatingClockPosition.BOTTOM_RIGHT, FloatingClockPosition.fromId(prefs.getString(PREF_CLOCK_POSITION, null)))
            }
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
