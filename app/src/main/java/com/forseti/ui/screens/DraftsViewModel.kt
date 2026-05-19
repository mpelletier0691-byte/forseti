package com.forseti.ui.screens

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.forseti.drafts.DraftDoc
import com.forseti.drafts.DraftGenerator
import com.forseti.drafts.DraftPrefill
import com.forseti.drafts.DraftPrinting
import com.forseti.drafts.DraftSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Result of building a printable draft. Surfaces a user-facing error string so
 * the screen can show a snackbar instead of silently failing (the previous
 * behavior, which made tapping a missing bundled form look like a crash).
 */
sealed interface DraftMaterialization {
    data class Success(val file: File) : DraftMaterialization
    data class Error(val message: String) : DraftMaterialization
}

@HiltViewModel
class DraftsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val generator: DraftGenerator
) : ViewModel() {

    /**
     * Materialize either by copying the bundled asset to cache or by generating.
     * Does file I/O / PDF rendering: callers must invoke from a background dispatcher.
     */
    fun materialize(doc: DraftDoc): DraftMaterialization = build(doc, prefill = null)

    /** Generated-only path that re-renders the draft with OCR-derived prefill. */
    fun materializeWithPrefill(doc: DraftDoc, prefill: DraftPrefill): DraftMaterialization =
        build(doc, prefill)

    private fun build(doc: DraftDoc, prefill: DraftPrefill?): DraftMaterialization {
        return runCatching {
            when (val src = doc.source) {
                is DraftSource.Bundled ->
                    DraftPrinting.copyAssetToCache(context, src.assetPath)
                        ?: error("Bundled PDF missing: ${src.assetPath}. The official form did not ship with this build; tap Share on a Generated draft instead.")

                is DraftSource.Generated ->
                    if (prefill != null) generator.generate(doc, prefill) else generator.generate(doc)
            }
        }.fold(
            onSuccess = { DraftMaterialization.Success(it) },
            onFailure = {
                Log.e(TAG, "Failed to materialize ${doc.id}: ${it.message}", it)
                DraftMaterialization.Error(it.message ?: "Could not build ${doc.title}.")
            }
        )
    }

    private companion object {
        const val TAG = "DraftsViewModel"
    }
}
