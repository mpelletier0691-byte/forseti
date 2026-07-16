package com.forseti.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Persist SAF read grants so background [com.forseti.casefiles.CaseIngestWorker] can read picks. */
object IngestUriPermissions {
    fun persistTree(context: Context, uri: Uri): Boolean = runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }.isSuccess

    fun persistUris(context: Context, uris: List<Uri>): Boolean {
        if (uris.isEmpty()) return false
        var any = false
        uris.forEach { uri ->
            if (persistUri(context, uri)) any = true
        }
        return any
    }

    fun persistUri(context: Context, uri: Uri): Boolean = runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }.isSuccess
}
