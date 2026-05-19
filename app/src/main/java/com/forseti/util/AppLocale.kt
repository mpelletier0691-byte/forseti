package com.forseti.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.os.LocaleListCompat
import com.forseti.R
import java.util.Locale

/**
 * Supported UI languages. Guide / FRCP content stays English; only chrome is localized.
 *
 * Uses [AppCompatDelegate.setApplicationLocales] (one API, no Compose context hacks).
 * After the user picks a language, [MainActivity] calls [applyAndPersist] then [recreate];
 * bootstrap is cached so the restart is quick.
 */
object AppLocale {
    const val TAG_ENGLISH = "en"
    const val TAG_SPANISH = "es"
    /** Matches [com.forseti.R] `values-zh-rCN`. */
    const val TAG_CHINESE = "zh-CN"

    val supported: List<LocaleOption> = listOf(
        LocaleOption(TAG_ENGLISH, "English"),
        LocaleOption(TAG_SPANISH, "Español"),
        LocaleOption(TAG_CHINESE, "简体中文"),
    )

    data class LocaleOption(val tag: String, val displayName: String)

    /** Cold start: apply saved locale before any activity opens. */
    fun applyStored(context: Context) {
        val prefs = DisclaimerPrefs(context.applicationContext)
        if (!prefs.languageChosen) return
        setLocales(prefs.languageTag)
    }

    /** Save choice and register with AppCompat (call before [android.app.Activity.recreate]). */
    fun applyAndPersist(context: Context, tag: String) {
        val prefs = DisclaimerPrefs(context.applicationContext)
        prefs.languageTag = tag
        prefs.languageChosen = true
        setLocales(tag)
    }

    private fun setLocales(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /** Language-picker preview strings (does not touch activity locale). */
    fun string(context: Context, languageTag: String, @StringRes resId: Int): String {
        val app = context.applicationContext
        val config = Configuration(app.resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(languageTag))
        return app.createConfigurationContext(config).getString(resId)
    }

    @Composable
    fun localizedString(languageTag: String, @StringRes resId: Int): String {
        val context = androidx.compose.ui.platform.LocalContext.current
        return remember(languageTag, resId) {
            string(context, languageTag, resId)
        }
    }

    fun guessFromSystem(context: Context): String {
        val locales = context.resources.configuration.locales
        for (i in 0 until locales.size()) {
            val locale = locales[i]
            supportedTagFor(locale)?.let { return it }
        }
        return TAG_ENGLISH
    }

    private fun supportedTagFor(locale: Locale): String? {
        val lang = locale.language.lowercase(Locale.ROOT)
        return when {
            lang == "es" -> TAG_SPANISH
            lang == "zh" -> TAG_CHINESE
            lang == "en" -> TAG_ENGLISH
            else -> null
        }
    }
}
