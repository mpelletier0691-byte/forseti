package com.forseti.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.forseti.R
import com.forseti.ui.theme.ForsetiColors

private data class WhatsNewStep(val titleRes: Int, val bodyRes: Int)

private val whatsNewSteps = listOf(
    WhatsNewStep(R.string.whats_new_step1_title, R.string.whats_new_step1_body),
    WhatsNewStep(R.string.whats_new_step2_title, R.string.whats_new_step2_body),
    WhatsNewStep(R.string.whats_new_step3_title, R.string.whats_new_step3_body),
    WhatsNewStep(R.string.whats_new_step4_title, R.string.whats_new_step4_body),
    WhatsNewStep(R.string.whats_new_step5_title, R.string.whats_new_step5_body),
    WhatsNewStep(R.string.whats_new_step6_title, R.string.whats_new_step6_body),
)

/**
 * One-time per version intro for confidence routing and edge-to-edge UI changes.
 */
@Composable
fun WhatsNewOverlay(onDismiss: () -> Unit, onOpenFullGuide: () -> Unit) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val step = whatsNewSteps[stepIndex]
    val total = whatsNewSteps.size
    val isLast = stepIndex == total - 1

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(Color(0xCC000000))
            .pointerInput(Unit) { },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ForsetiColors.Surface,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.whats_new_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = ForsetiColors.RuneGold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.whats_new_step_fmt, stepIndex + 1, total),
                    style = MaterialTheme.typography.labelMedium,
                    color = ForsetiColors.AshGrey
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(step.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = ForsetiColors.AshWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(step.bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForsetiColors.AshGrey,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (isLast) onDismiss() else stepIndex++
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForsetiColors.RuneGold,
                        contentColor = ForsetiColors.SplashBlack
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (isLast) stringResource(R.string.whats_new_done)
                        else stringResource(R.string.whats_new_next)
                    )
                }
                TextButton(onClick = onOpenFullGuide) {
                    Text(stringResource(R.string.whats_new_read_guide), color = ForsetiColors.RavenBlue)
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.whats_new_skip), color = ForsetiColors.AshGrey)
                }
            }
        }
    }
}
