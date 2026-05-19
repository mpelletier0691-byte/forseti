package com.forseti.imports

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user-imported rule PDFs that the app cannot fetch automatically
 * (e.g. when an official site is down, or the user wants to keep their own
 * copy of an annotated PDF). These live in `<files>/uploaded_rules/` and are
 * deliberately **not** considered case documents — auto-filing logic skips
 * them so they stay in this tab until the user explicitly moves them.
 *
 * A small sidecar `manifest.json` keeps friendly titles + jurisdiction labels
 * separate from the on-disk filename.
 */
@Singleton
class UploadedRulesService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class UploadedRule(
        val file: File,
        val displayTitle: String,
        val jurisdiction: String,
        val source: String,
        val importedAt: Long
    )

    fun root(): File {
        val ext = context.getExternalFilesDir("uploaded_rules")
        return (ext ?: File(context.filesDir, "uploaded_rules")).apply { mkdirs() }
    }

    fun list(): List<UploadedRule> {
        val dir = root()
        val manifest = readManifest()
        val files = dir.listFiles { f -> f.isFile && f.extension.equals("pdf", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
        return files.map { f ->
            val entry = manifest.optJSONObject(f.name)
            UploadedRule(
                file = f,
                displayTitle = entry?.optString("title")?.takeIf { it.isNotBlank() } ?: f.nameWithoutExtension,
                jurisdiction = entry?.optString("jurisdiction").orEmpty(),
                source = entry?.optString("source").orEmpty(),
                importedAt = entry?.optLong("importedAt") ?: f.lastModified()
            )
        }
    }

    /**
     * Copy the picked content URI into the uploaded-rules folder. Returns the
     * resulting file or null on failure. [title], [jurisdiction], and [source]
     * are stored as friendly metadata so we don't have to mangle the filename.
     */
    fun importFromUri(
        uri: Uri,
        title: String,
        jurisdiction: String,
        source: String
    ): File? {
        val original = displayName(uri) ?: "imported.pdf"
        val safe = sanitize(original)
        val dir = root()
        var dest = File(dir, safe)
        var n = 1
        while (dest.exists()) {
            val base = safe.substringBeforeLast('.', safe)
            val ext = safe.substringAfterLast('.', "")
            dest = File(dir, if (ext.isNotBlank()) "${base}_$n.$ext" else "${base}_$n")
            n++
            if (n > 99) return null
        }
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            writeManifestEntry(dest.name, title.ifBlank { dest.nameWithoutExtension }, jurisdiction, source)
            dest
        }.getOrNull()
    }

    fun delete(file: File): Boolean {
        val ok = file.delete()
        if (ok) removeManifestEntry(file.name)
        return ok
    }

    fun rename(file: File, newDisplayTitle: String): Boolean {
        if (newDisplayTitle.isBlank()) return false
        val manifest = readManifest()
        val entry = manifest.optJSONObject(file.name) ?: JSONObject()
        entry.put("title", newDisplayTitle)
        entry.put("importedAt", entry.optLong("importedAt", file.lastModified()))
        manifest.put(file.name, entry)
        return writeManifest(manifest)
    }

    fun shareableUri(file: File): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    private fun displayName(uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        var name: String? = null
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) name = cursor.getString(idx)
                    }
                }
        }
        return name ?: uri.lastPathSegment
    }

    private fun manifestFile(): File = File(root(), "manifest.json")

    private fun readManifest(): JSONObject {
        val f = manifestFile()
        if (!f.exists()) return JSONObject()
        return runCatching { JSONObject(f.readText()) }.getOrElse { JSONObject() }
    }

    private fun writeManifest(obj: JSONObject): Boolean = runCatching {
        manifestFile().writeText(obj.toString(2))
        true
    }.getOrElse { false }

    private fun writeManifestEntry(name: String, title: String, jurisdiction: String, source: String): Boolean {
        val manifest = readManifest()
        val entry = JSONObject().apply {
            put("title", title)
            put("jurisdiction", jurisdiction)
            put("source", source)
            put("importedAt", System.currentTimeMillis())
        }
        manifest.put(name, entry)
        return writeManifest(manifest)
    }

    private fun removeManifestEntry(name: String): Boolean {
        val manifest = readManifest()
        manifest.remove(name)
        return writeManifest(manifest)
    }

    private fun sanitize(raw: String): String {
        val ext = raw.substringAfterLast('.', "").lowercase(Locale.US)
        val base = raw.substringBeforeLast('.', raw)
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .ifBlank { "rule_${SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())}" }
            .take(96)
        return if (ext.isNotBlank()) "$base.$ext" else "$base.pdf"
    }

    @Suppress("unused")
    fun jurisdictionsKnown(): List<String> = listOf(
        "Federal", "Alabama", "Alaska", "Arizona", "Arkansas", "California",
        "Colorado", "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii",
        "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana",
        "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi",
        "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey",
        "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio", "Oklahoma",
        "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota",
        "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington",
        "West Virginia", "Wisconsin", "Wyoming", "District of Columbia"
    )

    /** JSON used for export/import or backup snapshots. */
    @Suppress("unused")
    fun manifestSnapshot(): String {
        val items = list()
        val arr = JSONArray()
        items.forEach { r ->
            arr.put(
                JSONObject()
                    .put("file", r.file.name)
                    .put("title", r.displayTitle)
                    .put("jurisdiction", r.jurisdiction)
                    .put("source", r.source)
                    .put("importedAt", r.importedAt)
            )
        }
        return arr.toString(2)
    }
}
