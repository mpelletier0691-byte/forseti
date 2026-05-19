package com.forseti.util

import android.content.Context

/**
 * Tiny SharedPreferences wrapper for app gates that must be readable synchronously
 * from composition (where DataStore's suspending API is awkward).
 *
 *  - [isAccepted]: first-launch legal disclaimer acknowledgment.
 *  - [forceDark]:  Settings-screen toggle that pins the dark color scheme.
 *  - [languageChosen] / [languageTag]: per-app UI locale (en, es, zh-CN).
 *  - [tutorialCompleted]: one-time onboarding tour finished.
 */
class DisclaimerPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var isAccepted: Boolean
        get() = prefs.getBoolean(KEY_ACCEPTED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ACCEPTED, value).apply()
        }

    var forceDark: Boolean
        get() = prefs.getBoolean(KEY_FORCE_DARK, true)
        set(value) {
            prefs.edit().putBoolean(KEY_FORCE_DARK, value).apply()
        }

    var languageChosen: Boolean
        get() = prefs.getBoolean(KEY_LANGUAGE_CHOSEN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_LANGUAGE_CHOSEN, value).apply()
        }

    var languageTag: String
        get() = normalizeLanguageTag(
            prefs.getString(KEY_LANGUAGE_TAG, AppLocale.TAG_ENGLISH) ?: AppLocale.TAG_ENGLISH
        )
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE_TAG, normalizeLanguageTag(value)).apply()
        }

    private fun normalizeLanguageTag(tag: String): String =
        if (tag == "zh-Hans") AppLocale.TAG_CHINESE else tag

    var tutorialCompleted: Boolean
        get() = prefs.getBoolean(KEY_TUTORIAL_COMPLETED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_TUTORIAL_COMPLETED, value).apply()
        }

    companion object {
        private const val NAME = "forseti_prefs"
        private const val KEY_ACCEPTED = "disclaimer_accepted"
        private const val KEY_FORCE_DARK = "force_dark"
        private const val KEY_LANGUAGE_CHOSEN = "language_chosen"
        private const val KEY_LANGUAGE_TAG = "app_language_tag"
        private const val KEY_TUTORIAL_COMPLETED = "tutorial_completed"
    }
}
