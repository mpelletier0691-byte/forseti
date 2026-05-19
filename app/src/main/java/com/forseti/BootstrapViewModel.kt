package com.forseti

import com.forseti.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forseti.pdf.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds the splash screen until baseline initialization completes.
 *
 * Bootstrap responsibilities:
 *  - warm the FRCP PDF renderer ([android.graphics.pdf.PdfRenderer]) and outline (cached).
 *  - allow Room to migrate.
 *  - guarantee a minimum splash duration so the brand mark is actually seen.
 */
@HiltViewModel
class BootstrapViewModel @Inject constructor(
    private val pdfRepository: PdfRepository
) : ViewModel() {

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        viewModelScope.launch {
            if (ForsetiBootstrap.hasCompleted) {
                _ready.value = true
                return@launch
            }
            // Run init work concurrently with the minimum splash hold so we never block on it.
            val minHold = async { delay(MIN_SPLASH_MS) }
            val warmup = async { pdfRepository.warmFrcp() }
            minHold.await()
            warmup.await()
            ForsetiBootstrap.hasCompleted = true
            _ready.value = true
        }
    }

    companion object {
        // Held long enough for the brand mark, version, motto, sign-off, and
        // (on trial accounts) the trial countdown banner to be readable on
        // first launch.
        val MIN_SPLASH_MS: Long = if (BuildConfig.DEBUG) 1_000L else 7_000L
    }
}
