package com.forseti.idp

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object IngestMetaStore {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun sidecarFor(file: File): File = File(file.parentFile, "${file.name}.ingest.json")

    fun save(file: File, meta: IngestMeta) {
        sidecarFor(file).writeText(json.encodeToString(meta))
    }

    fun load(file: File): IngestMeta? = runCatching {
        val sc = sidecarFor(file)
        if (!sc.exists()) return@runCatching null
        json.decodeFromString<IngestMeta>(sc.readText())
    }.getOrNull()

    /** Move sidecar metadata when the primary file is relocated. */
    fun moveWith(source: File, dest: File) {
        val meta = load(source)
        sidecarFor(source).takeIf { it.exists() }?.delete()
        if (meta != null && dest.exists()) {
            save(dest, meta)
        }
    }

    fun markFiled(file: File, suggestedFolder: String) {
        val meta = load(file) ?: return
        save(
            file,
            meta.copy(
                suggestedFolder = suggestedFolder,
                routing = "manual-filed"
            )
        )
    }
}
