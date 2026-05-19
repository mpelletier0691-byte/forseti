package com.forseti.glossary

import android.content.Context
import com.forseti.util.LocalizedAssets
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GlossaryTerm(val term: String, val definition: String)

@Serializable
private data class GlossaryFile(val terms: List<GlossaryTerm>)

@Singleton
class GlossaryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): List<GlossaryTerm> = withContext(Dispatchers.IO) {
        val file = LocalizedAssets.glossaryFile(context)
        runCatching {
            val raw = context.assets.open(file).bufferedReader().use { it.readText() }
            json.decodeFromString(GlossaryFile.serializer(), raw).terms.sortedBy { it.term.lowercase() }
        }.getOrElse {
            if (file != "glossary.json") {
                runCatching {
                    val raw = context.assets.open("glossary.json").bufferedReader().use { it.readText() }
                    json.decodeFromString(GlossaryFile.serializer(), raw).terms.sortedBy { it.term.lowercase() }
                }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
        }
    }
}
