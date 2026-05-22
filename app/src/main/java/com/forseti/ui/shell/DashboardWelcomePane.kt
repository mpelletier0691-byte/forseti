package com.forseti.ui.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forseti.R
import com.forseti.ui.components.ForsetiBrandTitle
import com.forseti.ui.theme.ForsetiColors

/**
 * Dashboard welcome surface shown to the right of the sidebar until the user
 * selects a destination. Displays the brand logo, name, tagline, motto, brand
 * sign-off, and the legal disclaimer so the app always opens to a coherent
 * "home" view rather than a previewed tab.
 *
 * Vertically scrollable so phones with narrow horizontal panes still surface
 * every line without truncation.
 */
@Composable
fun DashboardWelcomePane(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ForsetiColors.Background),
        contentAlignment = Alignment.Center
    ) {
        val shorter = if (maxWidth < maxHeight) maxWidth else maxHeight
        val logoSize = (shorter * 0.55f).coerceIn(120.dp, 360.dp)
        val narrowPane = maxWidth < 220.dp
        val titleStyle = when {
            maxWidth < 140.dp -> MaterialTheme.typography.titleLarge
            maxWidth < 200.dp -> MaterialTheme.typography.headlineMedium
            else -> MaterialTheme.typography.displaySmall
        }
        val taglineStyle = if (narrowPane) {
            MaterialTheme.typography.bodyMedium
        } else {
            MaterialTheme.typography.titleMedium
        }
        val bodyStyle = if (narrowPane) {
            MaterialTheme.typography.bodySmall
        } else {
            MaterialTheme.typography.bodyMedium
        }
        val mottoStyle = if (narrowPane) {
            MaterialTheme.typography.bodySmall
        } else {
            MaterialTheme.typography.bodyLarge
        }
        val scroll = rememberScrollState()

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.splash_raven_scales),
                    contentDescription = stringResource(R.string.cd_app_logo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(logoSize)
                )
                Spacer(Modifier.height(20.dp))
                ForsetiBrandTitle(
                    color = ForsetiColors.RuneGold,
                    maxStyle = titleStyle,
                    minFontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = taglineStyle,
                    color = ForsetiColors.AshWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.dashboard_welcome_subheading),
                    style = bodyStyle,
                    color = ForsetiColors.AshGrey,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (narrowPane) 4.dp else 16.dp)
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.splash_motto),
                    style = mottoStyle,
                    color = ForsetiColors.AshWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (narrowPane) 4.dp else 8.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.about_brand_signoff),
                    style = MaterialTheme.typography.labelLarge,
                    color = ForsetiColors.RuneGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.footer_disclaimer),
                    style = MaterialTheme.typography.titleSmall,
                    color = ForsetiColors.MeadAmber,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
