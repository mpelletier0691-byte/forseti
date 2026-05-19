package com.forseti.casefiles

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.forseti.data.entities.CaseEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bulk ingestion for the Case Profile "Ingest folder" / "Ingest images" buttons.
 *
 * The user picks either:
 *   • A directory tree (Storage Access Framework `OpenDocumentTree`) — every
 *     supported file inside is auto-routed into the right Brokkr-Forge folder,
 *     and `99_Inbox/` collects what the router could not classify.
 *   • One or more files (`OpenMultipleDocuments`) — same routing logic, but
 *     scoped to the user's selection so they can pull in just the screenshots
 *     they care about.
 *
 * Returns a [Report] that the UI can surface in a snackbar / toast.
 */
@Singleton
class CaseIngestService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folders: CaseFolderService
) {
    data class Report(
        val imported: Int,
        val skipped: Int,
        val unclassified: Int,
        val firstFolder: File?
    ) {
        fun summary(): String = buildString {
            append("Imported $imported file")
            if (imported != 1) append("s")
            if (unclassified > 0) append(" \u00B7 $unclassified to 99_Inbox")
            if (skipped > 0) append(" \u00B7 $skipped skipped")
        }
    }

    /** Walk a Storage Access Framework directory tree and ingest every file. */
    suspend fun ingestTree(case: CaseEntity, treeUri: Uri): Report = withContext(Dispatchers.IO) {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext empty()
        val files = mutableListOf<DocumentFile>()
        collect(tree, files)
        ingestDocs(case, files)
    }

    /** Ingest a fixed list of file URIs (e.g. from OpenMultipleDocuments). */
    suspend fun ingestUris(case: CaseEntity, uris: List<Uri>): Report = withContext(Dispatchers.IO) {
        val docs = uris.mapNotNull { DocumentFile.fromSingleUri(context, it) }
        ingestDocs(case, docs)
    }

    private fun collect(dir: DocumentFile, into: MutableList<DocumentFile>) {
        dir.listFiles().forEach { child ->
            when {
                child.isDirectory -> collect(child, into)
                child.isFile -> into += child
            }
        }
    }

    private fun ingestDocs(case: CaseEntity, docs: List<DocumentFile>): Report {
        if (docs.isEmpty()) return empty()
        val root = folders.ensureCaseRoot(case) ?: return empty()
        var imported = 0
        var skipped = 0
        var unclassified = 0
        var firstFolder: File? = null
        for (doc in docs) {
            val uri = doc.uri
            val name = displayName(uri) ?: doc.name ?: continue
            if (name.startsWith(".")) { skipped++; continue }
            val mime = doc.type ?: context.contentResolver.getType(uri)
            val ext = name.substringAfterLast('.', "")
            val target = folders.routeIngestedFile(case, name, mime, ext) ?: continue
            val saved = folders.importContent(uri, target, name)
            if (saved == null) { skipped++; continue }
            imported++
            if (target.name == "99_Inbox") unclassified++
            if (firstFolder == null) firstFolder = target
            folders.appendIndex(root, "ingest → $name", target)
        }
        return Report(imported, skipped, unclassified, firstFolder)
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx) else null
                } else null
            }
    }.getOrNull()

    private fun empty(): Report = Report(0, 0, 0, null)
}
