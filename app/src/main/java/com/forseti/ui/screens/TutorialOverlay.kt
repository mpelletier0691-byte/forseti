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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

private data class TutorialStep(val titleRes: Int, val bodyRes: Int)

private val tutorialSteps = listOf(
    TutorialStep(R.string.tutorial_step1_title, R.string.tutorial_step1_body),
    TutorialStep(R.string.tutorial_step2_title, R.string.tutorial_step2_body),
    TutorialStep(R.string.tutorial_step3_title, R.string.tutorial_step3_body),
    TutorialStep(R.string.tutorial_step4_title, R.string.tutorial_step4_body),
    TutorialStep(R.string.tutorial_step5_title, R.string.tutorial_step5_body),
    TutorialStep(R.string.tutorial_step6_title, R.string.tutorial_step6_body),
)

@Composable
fun TutorialOverlay(onComplete: () -> Unit) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val step = tutorialSteps[stepIndex]
    val total = tutorialSteps.size
    val isLast = stepIndex == total - 1

    BackHandler {
        if (stepIndex > 0) stepIndex--
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                .widthIn(max = 480.dp)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(
                        R.string.tutorial_step_fmt,
                        stepIndex + 1,
                        total
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = ForsetiColors.AshGrey
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(step.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    color = ForsetiColors.RuneGold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(step.bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForsetiColors.AshWhite,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (isLast) onComplete()
                        else stepIndex++
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForsetiColors.RuneGold,
                        contentColor = ForsetiColors.SplashBlack
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            if (isLast) R.string.tutorial_done else R.string.tutorial_next
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
