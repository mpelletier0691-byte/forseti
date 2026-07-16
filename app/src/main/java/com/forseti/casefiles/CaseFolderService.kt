package com.forseti.casefiles

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.forseti.data.entities.CaseEntity
import com.forseti.data.entities.DeadlineEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Brokkr-Forge case workspace.
 *
 * Every case gets a deterministic on-device folder tree:
 * ```
 * Case_<id>_<slug>/
 *   00_Case_Overview/   (Notes/)
 *   01_Pleadings/       (Complaint/, Answer/, Motions/, Orders/)
 *   02_Service_of_Process/  (Proof_of_Service/, Summons/, Correspondence/)
 *   03_Discovery/       (Interrogatories/, Requests_for_Production/,
 *                        Admissions/, Depositions/, Discovery_Responses/)
 *   04_Evidence/        (Photos/, PDFs/, Screenshots/, Audio/, Video/)
 *   05_Motions/         (Drafts/, Filed/, Court_Responses/)
 *   06_Correspondence/  (Opposing_Party/, Court/, Misc/)
 *   07_Deadlines/       (Completed/)
 *   08_Exhibits/        (Labels/, Final_Exhibits/)
 *   09_Hearings/        (Notices/, Prep/, Outcomes/)
 *   10_Trial/           (Trial_Brief/, Witness_List/, Jury_Instructions/, Final_Binder/)
 *   98_Scans/           (Camera scanner inbox if no other match)
 *   99_Inbox/           (Things the auto-router couldn’t classify)
 *   00_INDEX.txt        (human-readable activity log)
 * ```
 *
 * The same service handles auto-routing for scanned pages, shared PDFs, and
 * the new "ingest folder" / "ingest images" buttons in Case Profile.
 */
@Singleton
class CaseFolderService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun ensureFoldersForDeadline(case: CaseEntity, deadline: DeadlineEntity) {
        val root = ensureCaseRoot(case) ?: return
        val target = classifyForCase(case, "${deadline.title} ${deadline.ruleCitation.orEmpty()}")
            ?: File(root, INBOX)
        target.mkdirs()
        appendIndex(root, "deadline → ${deadline.title}", target)
    }

    /** Creates the whole tree if needed and returns the case root. */
    fun ensureCaseRoot(case: CaseEntity): File? = runCatching {
        val root = caseRootOrCreate(case) ?: return@runCatching null
        SCHEMA.forEach { (phase, subs) ->
            val pdir = File(root, phase).apply { mkdirs() }
            subs.forEach { File(pdir, it).mkdirs() }
        }
        File(root, SCANS).mkdirs()
        File(root, INBOX).mkdirs()
        File(root, "00_INDEX.txt").apply {
            if (!exists()) writeText(indexHeader(case))
        }
        root
    }.getOrNull()

    /** Where the document scanner drops captures when no keyword matches. */
    fun scannerFolder(case: CaseEntity): File? {
        val root = ensureCaseRoot(case) ?: return null
        return File(root, SCANS).apply { mkdirs() }
    }

    /**
     * Auto-routing entry point. Looks at the [hint] (filename, label, or short
     * OCR snippet) and returns the best-matching subfolder for this case.
     * Returns 99_Inbox/ when nothing classified so the file is still saved
     * — never null when the case root could be created.
     */
    fun classifyForCase(case: CaseEntity, hint: String?): File? {
        val root = ensureCaseRoot(case) ?: return null
        val text = normalizeHint(hint.orEmpty())
        val rel = routeKeyword(text) ?: return File(root, INBOX).apply { mkdirs() }
        return File(root, rel).apply { mkdirs() }
    }

    /**
     * Route a file by both [hint] (filename / OCR text) and its mime/extension.
     * Used by the Case Profile "ingest folder" / "ingest images" buttons.
     *
     * Order of precedence:
     *   1. Keyword in the hint (matches the same table as [classifyForCase]).
     *   2. Mime / extension family (image → Evidence/Photos, audio → Evidence/Audio, …).
     *   3. 99_Inbox/ as a safe landing pad.
     */
    /**
     * Explains how a file was routed during ingest (for audit logs).
     */
    data class RouteDecision(
        val relativePath: String,
        val method: String,
        val matchedKeyword: String? = null
    )

    fun decideIngestRoute(
        hint: String?,
        mimeType: String?,
        extension: String?
    ): RouteDecision {
        val text = normalizeHint(hint.orEmpty())
        val keywordHit = matchKeyword(text)
        if (keywordHit != null) {
            return RouteDecision(
                relativePath = keywordHit.second,
                method = "keyword",
                matchedKeyword = keywordHit.first
            )
        }

        val mime = mimeType?.lowercase(Locale.US).orEmpty()
        val ext = extension?.lowercase(Locale.US).orEmpty().removePrefix(".")
        val isScreenshot = text.contains("screenshot") || text.contains("screen_shot")
        val byType = when {
            mime.startsWith("image/") || ext in IMAGE_EXTS ->
                if (isScreenshot) "$EVIDENCE/Screenshots" to "mime:image-screenshot"
                else "$EVIDENCE/Photos" to "mime:image-photo"
            mime.startsWith("audio/") || ext in AUDIO_EXTS -> "$EVIDENCE/Audio" to "mime:audio"
            mime.startsWith("video/") || ext in VIDEO_EXTS -> "$EVIDENCE/Video" to "mime:video"
            mime == "application/pdf" || ext == "pdf" -> INBOX to "mime:pdf-unclassified"
            mime.startsWith("text/") || ext in TEXT_EXTS -> "00_Case_Overview/Notes" to "mime:text"
            else -> INBOX to "mime:unknown"
        }
        return RouteDecision(relativePath = byType.first, method = byType.second)
    }

    fun routeIngestedFile(
        case: CaseEntity,
        hint: String?,
        mimeType: String?,
        extension: String?
    ): File? {
        val root = ensureCaseRoot(case) ?: return null
        val decision = decideIngestRoute(hint, mimeType, extension)
        return File(root, decision.relativePath).apply { mkdirs() }
    }

    /**
     * When e-filing names are generic ("COVER PAGE 10 — MA-007…"), derive a clearer
     * import name from OCR text on page 1 when possible.
     */
    fun suggestImportName(original: String, hint: String, ocrSnippet: String?): String {
        val lower = original.lowercase(Locale.US)
        val looksGeneric = lower.contains("cover page") ||
            lower.contains("document (") ||
            lower.startsWith("scan_") ||
            lower.matches(Regex(".*\\b(ma|cf|nc|cv)-\\d{3,}.*")) && routeKeyword(normalizeHint(hint)) == null
        if (!looksGeneric) return original

        val titleLine = extractTitleFromOcr(ocrSnippet) ?: return original
        val ext = original.substringAfterLast('.', "").ifBlank { "pdf" }
        val date = java.time.LocalDate.now().toString()
        return sanitize("${date}_${titleLine}.$ext")
    }

    /** Top-level folder containing every case workspace; safe target for ZIP backup. */
    fun caseWorkspaceRoot(): File =
        context.getExternalFilesDir("case_workspace")
            ?: File(context.filesDir, "case_workspace").also { it.mkdirs() }

    fun displayPath(file: File): String = file.absolutePath

    /**
     * Removes the on-disk workspace folder for [case]. Callers should run on a
     * background dispatcher. Returns true if the directory no longer exists at
     * return time (either successfully deleted or wasn't there to begin with).
     *
     * Intentionally separate from the DB delete in [DeadlineRepository.deleteCase]
     * so a partial failure on one side doesn't roll back the other.
     */
    fun deleteCaseWorkspace(case: CaseEntity): Boolean {
        val base = caseWorkspaceRoot()
        val slug = sanitize("Case_${"%03d".format(case.id)}_${case.title}")
        val root = File(base, slug)
        if (!root.exists()) return true
        return root.deleteRecursively()
    }

    // ---- In-app file browser API ----

    fun listPhases(case: CaseEntity): List<File> {
        val root = ensureCaseRoot(case) ?: return emptyList()
        return root.listFiles { f -> f.isDirectory }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    fun listSubfolders(phase: File): List<File> =
        phase.listFiles { f -> f.isDirectory }?.sortedBy { it.name } ?: emptyList()

    fun listFiles(folder: File): List<File> =
        folder.listFiles { f ->
            f.isFile &&
                !f.name.endsWith(".ingest.json") &&
                !f.name.endsWith(".partial")
        }?.sortedBy { it.name } ?: emptyList()

    fun renameFile(file: File, newDisplayName: String): File? {
        val safeName = sanitize(newDisplayName)
        if (safeName.isBlank()) return null
        val target = File(file.parentFile, safeName)
        if (target.exists()) return null
        return if (file.renameTo(target)) {
            com.forseti.idp.IngestMetaStore.moveWith(file, target)
            target
        } else {
            null
        }
    }

    fun moveFile(file: File, destFolder: File): File? {
        if (!destFolder.isDirectory) return null
        // Picking the file's current folder is a no-op success — the user just
        // confirmed they're happy where it is.
        if (file.parentFile == destFolder) return file
        val target = File(destFolder, file.name)
        if (target.exists()) return null
        val moved = if (file.renameTo(target)) target else null
        if (moved != null) {
            com.forseti.idp.IngestMetaStore.moveWith(file, moved)
        }
        return moved
    }

    fun deleteFile(file: File): Boolean {
        com.forseti.idp.IngestMetaStore.sidecarFor(file).takeIf { it.exists() }?.delete()
        return file.delete()
    }

    /** Deletes every file in [folder] (not subfolders). Returns count removed. */
    fun deleteAllFilesIn(folder: File): Int {
        if (!folder.isDirectory) return 0
        var removed = 0
        listFiles(folder).forEach { file ->
            if (deleteFile(file)) removed++
        }
        return removed
    }

    /**
     * Match [hint] against the same keyword table used for scanner / ingest routing.
     * Returns the matched needle and Brokkr-Forge relative folder path.
     */
    fun findKeywordRoute(hint: String): KeywordRouteHit? {
        val hit = matchKeyword(normalizeHint(hint)) ?: return null
        return KeywordRouteHit(needle = hit.first, folder = hit.second)
    }

    /** Locate an existing workspace file with the same display name and byte size. */
    fun findFileByNameAndSize(root: File, name: String, size: Long): File? =
        root.walkTopDown()
            .filter { file ->
                file.isFile &&
                    !file.name.endsWith(".ingest.json") &&
                    !file.name.endsWith(".partial")
            }
            .firstOrNull { it.name == name && it.length() == size }

    /** Same logical document as [name] (ignoring `_1`, `_2` suffixes) and [size]. */
    fun findFileByNormalizedNameAndSize(root: File, name: String, size: Long): File? =
        runCatching {
            val normalized = normalizeImportBaseName(name)
            root.walkTopDown()
                .filter { file ->
                    file.isFile &&
                        !file.name.endsWith(".ingest.json") &&
                        !file.name.endsWith(".partial")
                }
                .firstOrNull {
                    normalizeImportBaseName(it.name) == normalized && it.length() == size
                }
        }.getOrNull()

    data class KeywordRouteHit(
        val needle: String,
        val folder: String
    )

    /** Copy a content URI into the given folder, generating a unique filename. */
    fun importContent(uri: Uri, intoFolder: File, suggestedName: String): File? {
        if (!intoFolder.isDirectory) return null
        val safeName = sanitize(suggestedName.ifBlank { "import_${System.currentTimeMillis()}" })
        var dest = File(intoFolder, safeName)
        var n = 1
        while (dest.exists()) {
            val base = safeName.substringBeforeLast('.', safeName)
            val ext = safeName.substringAfterLast('.', "")
            dest = File(intoFolder, if (ext.isNotBlank()) "${base}_$n.$ext" else "${base}_$n")
            n++
            if (n > 99) return null
        }
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            dest
        }.getOrNull()
    }

    fun shareableUri(file: File): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    /** Append a one-line activity entry to `00_INDEX.txt`. Public so ingest service can log. */
    fun appendIndex(root: File, action: String, target: File) {
        runCatching {
            val rel = target.absolutePath.removePrefix(root.absolutePath).removePrefix(File.separator)
            val idx = File(root, "00_INDEX.txt")
            idx.appendText("- $action → ${if (rel.isBlank()) "/" else "$rel/"}\n")
        }
    }

    // ---- internal ----

    private fun caseRootOrCreate(case: CaseEntity): File? {
        val base = caseWorkspaceRoot()
        val slug = sanitize("Case_${"%03d".format(case.id)}_${case.title}")
        return File(base, slug).apply { mkdirs() }
    }

    private fun indexHeader(case: CaseEntity): String =
        buildString {
            appendLine("Forseti case workspace (Brokkr-Forge layout)")
            appendLine("Title: ${case.title}")
            appendLine("Court: ${case.court}")
            appendLine("Number: ${case.caseNumber}")
            appendLine("Role: ${case.role}")
            appendLine()
            appendLine("Activity log:")
            appendLine()
        }

    /** Return relative path inside the case root, or null if no keyword matched. */
    private fun routeKeyword(text: String): String? = matchKeyword(text)?.second

    /** Matched needle and destination path, or null. */
    private fun matchKeyword(text: String): Pair<String, String>? {
        val normalized = normalizeHint(text)
        if (normalized.isBlank()) return null
        val filed = normalized.contains("filed") ||
            normalized.contains("stamped") ||
            normalized.contains("entered") ||
            normalized.contains("e-filed")
        for ((needle, target) in KEYWORD_ROUTES) {
            if (normalized.contains(needle)) return needle to adjustForFiled(target, filed)
        }
        return null
    }

    private fun adjustForFiled(target: String, filed: Boolean): String {
        if (!filed) return target
        return when {
            target.startsWith("05_Motions/") -> "05_Motions/Filed"
            else -> target
        }
    }

    private fun extractTitleFromOcr(ocr: String?): String? {
        if (ocr.isNullOrBlank()) return null
        return ocr.lineSequence()
            .map { it.trim() }
            .firstOrNull { line ->
                line.length in 4..72 && routeKeyword(line) != null
            }
            ?.let { sanitize(it).take(56) }
    }

    private fun normalizeHint(raw: String): String {
        var t = raw.lowercase(Locale.US)
        t = t.replace('—', '-').replace('–', '-')
        t = t.replace(Regex("cover\\s*page\\s*\\d*"), " ")
        t = t.replace(Regex("document\\s*\\(\\d+\\)"), " ")
        t = t.replace(Regex("\\b(ma|cf|nc|cv|sc)-\\d{3,}\\b"), " ")
        return t.replace(Regex("\\s+"), " ").trim()
    }

    companion object {
        private const val EVIDENCE = "04_Evidence"
        private const val SCANS = "98_Scans"
        private const val INBOX = "99_Inbox"

        /** Brokkr-Forge case structure. Order matters → drives sidebar listing. */
        private val SCHEMA: List<Pair<String, List<String>>> = listOf(
            "00_Case_Overview" to listOf("Notes"),
            "01_Pleadings" to listOf("Complaint", "Answer", "Motions", "Orders"),
            "02_Service_of_Process" to listOf("Proof_of_Service", "Summons", "Correspondence"),
            "03_Discovery" to listOf(
                "Interrogatories", "Requests_for_Production",
                "Admissions", "Depositions", "Discovery_Responses"
            ),
            "04_Evidence" to listOf("Photos", "PDFs", "Screenshots", "Audio", "Video"),
            "05_Motions" to listOf("Drafts", "Filed", "Court_Responses"),
            "06_Correspondence" to listOf("Opposing_Party", "Court", "Misc"),
            "07_Deadlines" to listOf("Completed"),
            "08_Exhibits" to listOf("Labels", "Final_Exhibits"),
            "09_Hearings" to listOf("Notices", "Prep", "Outcomes"),
            "10_Trial" to listOf("Trial_Brief", "Witness_List", "Jury_Instructions", "Final_Binder")
        )

        /**
         * Order matters: more specific terms are listed first so that, e.g.,
         * "trial brief" routes to 10_Trial/Trial_Brief instead of just "trial".
         */
        private val KEYWORD_ROUTES: List<Pair<String, String>> = listOf(
            // 10_Trial
            "trial brief" to "10_Trial/Trial_Brief",
            "witness list" to "10_Trial/Witness_List",
            "witness" to "10_Trial/Witness_List",
            "jury instruction" to "10_Trial/Jury_Instructions",
            "jury" to "10_Trial/Jury_Instructions",
            "trial binder" to "10_Trial/Final_Binder",
            "final binder" to "10_Trial/Final_Binder",
            "pretrial" to "09_Hearings/Prep",
            // 09_Hearings
            "hearing notice" to "09_Hearings/Notices",
            "notice of hearing" to "09_Hearings/Notices",
            "hearing" to "09_Hearings/Notices",
            // 08_Exhibits
            "exhibit list" to "08_Exhibits/Labels",
            "final exhibit" to "08_Exhibits/Final_Exhibits",
            "exhibit" to "08_Exhibits/Labels",
            // 07_Deadlines
            "calendar" to "07_Deadlines",
            "deadline" to "07_Deadlines",
            // 06_Correspondence
            "communications" to "06_Correspondence/Misc",
            "communication" to "06_Correspondence/Misc",
            "text message" to "06_Correspondence/Misc",
            "messenger" to "06_Correspondence/Misc",
            "facebook" to "06_Correspondence/Misc",
            "opposing counsel" to "06_Correspondence/Opposing_Party",
            "opposing party" to "06_Correspondence/Opposing_Party",
            "court letter" to "06_Correspondence/Court",
            "letter" to "06_Correspondence/Misc",
            "email" to "06_Correspondence/Misc",
            // 05_Motions  (Drafts default; "filed" sub-route handled below by suffix)
            "motion to dismiss" to "05_Motions/Drafts",
            "motion in limine" to "05_Motions/Drafts",
            "notice of motion" to "05_Motions/Drafts",
            "memorandum in support" to "05_Motions/Drafts",
            "memorandum of law" to "05_Motions/Drafts",
            "memorandum" to "05_Motions/Drafts",
            "brief in support" to "05_Motions/Drafts",
            "motion" to "05_Motions/Drafts",
            "opposition" to "05_Motions/Drafts",
            "reply" to "05_Motions/Drafts",
            "court response" to "05_Motions/Court_Responses",
            // 04_Evidence — keyword overrides for known evidence kinds
            "screenshot" to "04_Evidence/Screenshots",
            "photos for" to "04_Evidence/Photos",
            "videos for" to "04_Evidence/Video",
            "evidence for" to "04_Evidence/PDFs",
            "evidence" to "04_Evidence/PDFs",
            "gmail" to "06_Correspondence/Misc",
            "declaration" to "03_Discovery",
            "affidavit" to "03_Discovery",
            // 03_Discovery
            "interrogatories" to "03_Discovery/Interrogatories",
            "interrog" to "03_Discovery/Interrogatories",
            "request for production" to "03_Discovery/Requests_for_Production",
            "requests for production" to "03_Discovery/Requests_for_Production",
            "rfp" to "03_Discovery/Requests_for_Production",
            "request for admission" to "03_Discovery/Admissions",
            "requests for admission" to "03_Discovery/Admissions",
            "rfa" to "03_Discovery/Admissions",
            "deposition" to "03_Discovery/Depositions",
            "depo" to "03_Discovery/Depositions",
            "discovery response" to "03_Discovery/Discovery_Responses",
            "discovery" to "03_Discovery",
            "subpoena duces tecum" to "03_Discovery",
            "subpoena" to "03_Discovery",
            // 02_Service_of_Process
            "proof of service" to "02_Service_of_Process/Proof_of_Service",
            "service of process" to "02_Service_of_Process/Proof_of_Service",
            "summons" to "02_Service_of_Process/Summons",
            "service" to "02_Service_of_Process/Proof_of_Service",
            // 01_Pleadings
            "counterclaim" to "01_Pleadings",
            "cross-claim" to "01_Pleadings",
            "crossclaim" to "01_Pleadings",
            "complaint" to "01_Pleadings/Complaint",
            "answer" to "01_Pleadings/Answer",
            "order" to "01_Pleadings/Orders",
            "judgment" to "01_Pleadings/Orders",
            "pleading" to "01_Pleadings",
            // 00_Case_Overview
            "parties" to "00_Case_Overview",
            "case overview" to "00_Case_Overview",
            "notes" to "00_Case_Overview/Notes"
        )

        private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "gif", "bmp")
        private val AUDIO_EXTS = setOf("mp3", "m4a", "wav", "ogg", "aac", "flac", "amr")
        private val VIDEO_EXTS = setOf("mp4", "mov", "mkv", "webm", "3gp", "avi")
        private val TEXT_EXTS = setOf("txt", "md", "rtf")

        fun sanitize(raw: String): String {
            val stripped = raw.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            return stripped.ifBlank { "case" }.take(96)
        }

        /** Strip numeric collision suffixes added during import (`file_1.pdf` → `file.pdf`). */
        fun normalizeImportBaseName(name: String): String {
            val ext = name.substringAfterLast('.', "")
            val base = name.substringBeforeLast('.', name)
            val stripped = base.replace(Regex("_\\d+$"), "")
            return if (ext.isNotBlank()) "$stripped.$ext" else stripped
        }
    }
}
