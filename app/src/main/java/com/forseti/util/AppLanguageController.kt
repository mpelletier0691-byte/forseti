package com.forseti.util

import androidx.compose.runtime.compositionLocalOf

/** Persists locale and restarts the activity so Compose picks up the new language. */
fun interface AppLanguageController {
    fun setTag(tag: String)
}

val LocalAppLanguage = compositionLocalOf<AppLanguageController> {
    error("LocalAppLanguage not provided")
}
