package com.forseti.tts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-aloud service backed by the system [TextToSpeech] engine. We never
 * ship our own voices — Forseti just plumbs whatever the user has enabled
 * under Android Settings → Accessibility → Text-to-speech output.
 *
 * If the engine is missing or fails to initialize, [state] flips to
 * [State.Unavailable] and the UI shows a "Set up TTS" call-to-action that
 * deep-links to the system settings page.
 */
@Singleton
class ForsetiTts @Inject constructor(
    @ApplicationContext private val context: Context
) {
    enum class State { Idle, Initializing, Ready, Speaking, Paused, Unavailable }

    private val _state = MutableStateFlow(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var tts: TextToSpeech? = null
    private var queuedText: String? = null

    /**
     * Lazily initialize the engine. Idempotent — safe to call from every
     * read-aloud entry point.
     */
    fun ensureReady() {
        if (tts != null && _state.value !in setOf(State.Idle, State.Initializing)) return
        if (_state.value == State.Initializing) return
        _state.value = State.Initializing
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return@TextToSpeech
                val locale = Locale.getDefault().takeIf { engine.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
                    ?: Locale.US
                engine.language = locale
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { _state.value = State.Speaking }
                    override fun onDone(utteranceId: String?) {
                        if (_state.value == State.Speaking) _state.value = State.Ready
                    }
                    @Deprecated("Use the (id, errorCode) overload, but keep this for older devices.")
                    override fun onError(utteranceId: String?) {
                        _lastError.value = "Speech engine reported an error."
                        _state.value = State.Ready
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _lastError.value = "Speech engine error (code $errorCode)."
                        _state.value = State.Ready
                    }
                })
                _state.value = State.Ready
                queuedText?.let {
                    queuedText = null
                    speak(it)
                }
            } else {
                Log.w(TAG, "TTS init failed status=$status")
                _state.value = State.Unavailable
                _lastError.value =
                    "Text-to-speech is not enabled on this device. Open system settings to install or pick a voice."
            }
        }
    }

    /**
     * Speak [text] now. If the engine is still booting, queue the request and
     * fire it as soon as init succeeds. Splits long inputs into chunks so we
     * don't trip the engine's per-utterance length cap.
     */
    fun speak(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        ensureReady()
        val engine = tts
        if (engine == null || _state.value == State.Initializing) {
            queuedText = trimmed
            return
        }
        if (_state.value == State.Unavailable) return

        val maxChunk = (TextToSpeech.getMaxSpeechInputLength() - 16).coerceAtLeast(512)
        val chunks = chunkText(trimmed, maxChunk)
        engine.stop()
        chunks.forEachIndexed { i, chunk ->
            val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(chunk, mode, null, "forseti-${UUID.randomUUID()}")
        }
        _state.value = State.Speaking
    }

    fun stop() {
        tts?.stop()
        if (_state.value == State.Speaking || _state.value == State.Paused) {
            _state.value = State.Ready
        }
    }

    fun shutdown() {
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        _state.value = State.Idle
    }

    fun consumeError() { _lastError.value = null }

    /**
     * Intent that opens the system TTS settings page so the user can install
     * or pick a voice if [State.Unavailable] is reported.
     */
    fun openTtsSettingsIntent(): Intent =
        Intent("com.android.settings.TTS_SETTINGS")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Fallback if the dedicated TTS-settings intent isn't supported on the device. */
    fun openAccessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Best-effort marketplace deep-link to install Google's TTS engine. */
    fun installTtsIntent(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.tts"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun chunkText(text: String, max: Int): List<String> {
        if (text.length <= max) return listOf(text)
        val out = ArrayList<String>(text.length / max + 1)
        var i = 0
        while (i < text.length) {
            val end = (i + max).coerceAtMost(text.length)
            // Try to break on a sentence boundary so playback feels natural.
            val slice = text.substring(i, end)
            val cut = slice.lastIndexOfAny(charArrayOf('.', '!', '?', '\n'))
            val take = if (end < text.length && cut > max / 2) cut + 1 else slice.length
            out += slice.substring(0, take).trim()
            i += take
        }
        return out.filter { it.isNotBlank() }
    }

    companion object { private const val TAG = "ForsetiTts" }
}
