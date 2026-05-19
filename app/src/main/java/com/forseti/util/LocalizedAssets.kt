package com.forseti.util

import android.content.Context

/**
 * Resolves bundled asset paths for the user's chosen app language.
 * Official PDFs/forms under [assets/forms] and [assets/rules] stay English.
 */
object LocalizedAssets {
    fun languageTag(context: Context): String =
        DisclaimerPrefs(context.applicationContext).languageTag

    /** e.g. `guides`, `guides-es`, `guides-zh-rCN` */
    fun guidesFolder(context: Context): String = folder(context, "guides")

    /** e.g. `glossary.json`, `glossary-es.json` */
    fun glossaryFile(context: Context): String {
        val tag = languageTag(context)
        val suffix = when (tag) {
            AppLocale.TAG_SPANISH -> "-es"
            AppLocale.TAG_CHINESE -> "-zh-rCN"
            else -> ""
        }
        return "glossary$suffix.json"
    }

    private fun folder(context: Context, base: String): String {
        val tag = languageTag(context)
        val suffix = when (tag) {
            AppLocale.TAG_SPANISH -> "-es"
            AppLocale.TAG_CHINESE -> "-zh-rCN"
            else -> ""
        }
        return "$base$suffix"
    }

    fun openAsset(context: Context, folder: String, fileName: String): java.io.InputStream? {
        val localized = "$folder/$fileName"
        return runCatching { context.assets.open(localized) }.getOrNull()
            ?: runCatching {
                val fallbackFolder = folder.replace(Regex("-(es|zh-rCN)$"), "")
                context.assets.open("$fallbackFolder/$fileName")
            }.getOrNull()
    }
}
