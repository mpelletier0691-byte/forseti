package com.forseti.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.forseti.R

/**
 * Shrinks font size until [text] fits on one line in the available width.
 */
@Composable
fun FitSingleLineText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 14.sp,
    textAlign: TextAlign = TextAlign.Start
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        var fontSize by remember(text, maxWidth) { mutableStateOf(style.fontSize) }

        Text(
            text = text,
            style = style.copy(fontSize = fontSize, textAlign = textAlign),
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            onTextLayout = { layout ->
                if (layout.didOverflowWidth && fontSize > minFontSize) {
                    fontSize = (fontSize.value - 1f).coerceAtLeast(minFontSize.value).sp
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ForsetiBrandTitle(
    modifier: Modifier = Modifier,
    color: Color,
    maxStyle: TextStyle = MaterialTheme.typography.displaySmall,
    minFontSize: TextUnit = 18.sp,
    textAlign: TextAlign = TextAlign.Center
) {
    FitSingleLineText(
        text = stringResource(R.string.app_name),
        style = maxStyle,
        color = color,
        minFontSize = minFontSize,
        textAlign = textAlign,
        modifier = modifier
    )
}
