package com.forseti.casefiles

import android.content.Context
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bundles the on-device case workspace into a single ZIP for the user to share
 * or save off-device. Writes atomically to the app cache, then exposes a
 * shareable [androidx.core.content.FileProvider] URI.
 */
@Singleton
class BackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folders: CaseFolderService
) {

    data class BackupResult(val file: File, val sizeBytes: Long, val entries: Int)

    /** Builds a ZIP under cache/backups and returns it. Empty workspace => empty zip. */
    fun createBackup(): BackupResult {
        val outDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val final = File(outDir, "forseti_backup_$stamp.zip")
        val tmp = File(outDir, "forseti_backup_$stamp.partial")

        val workspace = folders.caseWorkspaceRoot()
        var entries = 0

        ZipOutputStream(FileOutputStream(tmp)).use { zip ->
            // Always include a manifest so an empty workspace still produces something.
            zip.putNextEntry(ZipEntry("MANIFEST.txt"))
            zip.write(buildManifest(workspace).toByteArray())
            zip.closeEntry()
            entries++

            if (workspace.exists()) {
                val basePath = workspace.absolutePath
                workspace.walkTopDown().filter { it.isFile }.forEach { file ->
                    val rel = file.absolutePath
                        .removePrefix(basePath)
                        .trimStart(File.separatorChar)
                    zip.putNextEntry(ZipEntry("case_workspace/$rel"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    entries++
                }
            }
        }

        if (!tmp.renameTo(final)) {
            tmp.copyTo(final, overwrite = true)
            tmp.delete()
        }
        return BackupResult(file = final, sizeBytes = final.length(), entries = entries)
    }

    /** Wraps [file] in a content URI suitable for share intents. */
    fun shareableUri(file: File): android.net.Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Lists previous backups, newest first. */
    fun listBackups(): List<File> {
        val dir = File(context.cacheDir, "backups")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".zip") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun delete(file: File): Boolean = runCatching { file.delete() }.getOrDefault(false)

    private fun buildManifest(workspace: File): String = buildString {
        appendLine("Forseti backup manifest")
        appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        appendLine("Workspace: ${workspace.absolutePath}")
        appendLine("App package: ${context.packageName}")
        appendLine()
        appendLine("This archive contains the on-device case workspace tree.")
        appendLine("Restore by extracting case_workspace/* into the same folder on a")
        appendLine("device with Forseti installed (path shown in the Backup tab).")
    }
}
