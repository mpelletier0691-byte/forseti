package com.forseti.states

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores downloaded state-rule PDFs in app-private storage.
 *
 * Files are keyed by SHA-1 of the source URL so the same URL never collides
 * with itself. We only delete entries when the user explicitly removes them.
 */
@Singleton
class StateCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: HttpClient
) {
    private val cacheDir: File = File(context.filesDir, "state_rules").apply { mkdirs() }

    private val _cached = MutableStateFlow(currentlyCached())
    val cached: StateFlow<Set<String>> = _cached.asStateFlow()

    fun fileFor(rule: StateRule): File = File(cacheDir, "${urlKey(rule.url)}.pdf")

    fun isCached(rule: StateRule): Boolean = fileFor(rule).let { it.exists() && it.length() > 0 }

    suspend fun download(rule: StateRule): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val target = fileFor(rule)
            val tmp = File(target.parentFile, "${target.name}.part")
            http.get(rule.url).bodyAsChannel().copyTo(FileOutputStream(tmp))
            if (tmp.length() < 1_000) {
                tmp.delete()
                error("Downloaded file too small (${tmp.length()} bytes)")
            }
            tmp.renameTo(target)
            _cached.value = currentlyCached()
            target
        }
    }

    fun delete(rule: StateRule) {
        fileFor(rule).delete()
        _cached.value = currentlyCached()
    }

    /**
     * Re-download every rule that already has a local cache entry. Used by the
     * "Check for updates" affordance so the user can refresh from the
     * authoritative source without manually toggling each row.
     */
    suspend fun refreshAllCached(allRules: List<StateRule>): RefreshSummary {
        val keysOnDisk = currentlyCached()
        val toRefresh = allRules.filter { keysOnDisk.contains(urlKey(it.url)) }
        var success = 0
        var failed = 0
        for (rule in toRefresh) {
            if (download(rule).isSuccess) success++ else failed++
        }
        return RefreshSummary(refreshed = success, failed = failed, attempted = toRefresh.size)
    }

    data class RefreshSummary(val refreshed: Int, val failed: Int, val attempted: Int)

    private fun currentlyCached(): Set<String> =
        cacheDir.listFiles()?.mapNotNull { f -> f.nameWithoutExtension.takeIf { f.length() > 0 } }?.toSet().orEmpty()

    companion object {
        fun urlKey(url: String): String {
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(url.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
