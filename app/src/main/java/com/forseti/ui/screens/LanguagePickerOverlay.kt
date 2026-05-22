package com.forseti.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.forseti.R
import com.forseti.ui.theme.ForsetiColors
import com.forseti.util.AppLocale

@Composable
fun LanguagePickerOverlay(
    initialTag: String = AppLocale.TAG_ENGLISH,
    firstRun: Boolean = false,
    onSelectionChanged: (String) -> Unit = {},
    onContinue: (tag: String) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    var selected by remember(initialTag) { mutableStateOf(initialTag) }

    LaunchedEffect(initialTag) {
        selected = initialTag
    }

    val title = AppLocale.localizedString(selected, R.string.language_picker_title)
    val subtitleRes =
        if (firstRun) R.string.language_picker_first_run_subtitle
        else R.string.language_picker_subtitle
    val subtitle = AppLocale.localizedString(selected, subtitleRes)
    val continueLabel = AppLocale.localizedString(selected, R.string.language_picker_continue)
    val cancelLabel = AppLocale.localizedString(selected, R.string.language_picker_cancel)

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
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = ForsetiColors.RuneGold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForsetiColors.AshWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                AppLocale.supported.forEach { option ->
                    LanguageOptionRow(
                        label = option.displayName,
                        selected = selected == option.tag,
                        onClick = {
                            selected = option.tag
                            onSelectionChanged(option.tag)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onContinue(selected) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForsetiColors.RuneGold,
                        contentColor = ForsetiColors.SplashBlack
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = continueLabel,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (onDismiss != null) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = cancelLabel,
                            color = ForsetiColors.AshGrey
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val borderColor = if (selected) ForsetiColors.RuneGold else ForsetiColors.Stone
    val bg = if (selected) ForsetiColors.RuneGold.copy(alpha = 0.12f) else ForsetiColors.Background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, shape)
            .background(bg, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) ForsetiColors.RuneGold else ForsetiColors.AshWhite
        )
    }
}
