package com.forseti.tts

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.forseti.ui.theme.ForsetiColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Compact "play / stop" pair for the read-aloud feature. Renders a settings
 * shortcut + helpful dialog when the system TTS engine is missing.
 *
 * @param tts shared singleton.
 * @param fetchText returns the text to speak; called every time Play is
 *   tapped so the caller (e.g. PDF reader) can OCR the current page on
 *   demand instead of pre-computing it for every page.
 */
@Composable
fun ReadAloudControls(
    tts: ForsetiTts,
    fetchText: suspend () -> String,
    iconTint: androidx.compose.ui.graphics.Color = ForsetiColors.AshWhite,
    modifier: Modifier = Modifier
) {
    val state by tts.state.collectAsState()
    val error by tts.lastError.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showUnavailable by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var activeJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(state) {
        if (state == ForsetiTts.State.Unavailable) showUnavailable = true
        if (state == ForsetiTts.State.Speaking || state == ForsetiTts.State.Ready) working = false
    }

    Row(modifier = modifier) {
        if (state == ForsetiTts.State.Speaking || working) {
            IconButton(onClick = {
                activeJob?.cancel()
                activeJob = null
                tts.stop()
                working = false
            }) {
                Icon(
                    Icons.Outlined.Stop,
                    contentDescription = "Stop reading",
                    tint = ForsetiColors.RuneGold
                )
            }
        } else {
            IconButton(onClick = {
                tts.ensureReady()
                if (tts.state.value == ForsetiTts.State.Unavailable) {
                    showUnavailable = true
                    return@IconButton
                }
                working = true
                activeJob = scope.launch {
                    val outcome = runCatching { fetchText() }
                    val text = outcome.getOrNull().orEmpty()
                    when {
                        outcome.isFailure -> {
                            // Speak the failure so the user hears something instead of nothing.
                            tts.speak("Sorry — Forseti couldn't read this page right now. Try scrolling and tapping read again.")
                        }
                        text.isBlank() -> {
                            tts.speak("Forseti found no recognizable text on this page. If it's a scan, try the next page.")
                        }
                        else -> tts.speak(text)
                    }
                }
            }) {
                Icon(
                    Icons.Outlined.RecordVoiceOver,
                    contentDescription = "Read aloud",
                    tint = iconTint
                )
            }
        }
    }

    if (showUnavailable) {
        TtsUnavailableDialog(
            message = error ?: "Text-to-speech isn't enabled on this device.",
            onDismiss = {
                tts.consumeError()
                showUnavailable = false
            },
            onOpenSettings = {
                val intents = listOf(tts.openTtsSettingsIntent(), tts.openAccessibilitySettingsIntent())
                for (i in intents) {
                    try { context.startActivity(i); break } catch (_: ActivityNotFoundException) {}
                }
            },
            onInstall = {
                runCatching { context.startActivity(tts.installTtsIntent()) }
            }
        )
    }
}

@Composable
private fun TtsUnavailableDialog(
    message: String,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onInstall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onOpenSettings(); onDismiss() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForsetiColors.RuneGold,
                    contentColor = ForsetiColors.SplashBlack
                )
            ) {
                Icon(Icons.Outlined.Settings, null)
                Spacer(Modifier.width(6.dp))
                Text("Open TTS settings")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onInstall(); onDismiss() }) {
                    Text("Install Google TTS", color = ForsetiColors.MeadAmber)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onDismiss) {
                    Text("Close", color = ForsetiColors.AshGrey)
                }
            }
        },
        title = { Text("Read-aloud needs system TTS", color = ForsetiColors.RuneGold) },
        text = {
            Text(
                message + "\n\nForseti uses whatever voice is configured in Android Settings \u2192 Accessibility \u2192 Text-to-speech output. Pick or install a voice there, then come back and try again.",
                color = ForsetiColors.AshGrey
            )
        },
        containerColor = ForsetiColors.Surface
    )
}
