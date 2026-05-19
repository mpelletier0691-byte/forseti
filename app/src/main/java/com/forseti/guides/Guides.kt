package com.forseti.guides

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.forseti.util.LocalizedAssets
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GuideMeta(
    val id: String,
    val title: String,
    val file: String,
    val minutes: Int
)

@Serializable
private data class GuideIndex(val guides: List<GuideMeta>)

@Singleton
class GuideRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val guidesFolder: String
        get() = LocalizedAssets.guidesFolder(context)

    suspend fun loadIndex(): List<GuideMeta> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = LocalizedAssets.openAsset(context, guidesFolder, "00_index.json")
                ?: return@withContext emptyList()
            stream.bufferedReader().use { it.readText() }
        }.mapCatching { json.decodeFromString(GuideIndex.serializer(), it).guides }
            .getOrDefault(emptyList())
    }

    suspend fun loadBody(meta: GuideMeta): String = withContext(Dispatchers.IO) {
        runCatching {
            val stream = LocalizedAssets.openAsset(context, guidesFolder, meta.file)
                ?: return@withContext fallbackBody(meta)
            stream.bufferedReader().use { it.readText() }
        }.map { sanitizeMarkdown(it) }.getOrDefault(fallbackBody(meta))
    }

    private fun fallbackBody(meta: GuideMeta): String =
        "# ${meta.title}\n\n_Content unavailable._"

    private fun sanitizeMarkdown(raw: String): String =
        raw.replace(
            Regex("^([\\s]*[-*+]\\s)\\[( |x|X)\\](\\s)", RegexOption.MULTILINE),
            "$1$3"
        )
}
