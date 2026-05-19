package com.forseti.ui.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.forseti.R
import com.forseti.billing.EntitlementManager
import com.forseti.ui.theme.ForsetiColors

/**
 * Brand splash held until [com.forseti.BootstrapViewModel.ready] flips. The logo
 * is large but the column is vertically scrollable so app name, tagline,
 * version, subtitle, the bold disclaimer line, and (for trial accounts) the
 * trial countdown banner are always reachable on small or notched displays.
 *
 * Pass `entitlement = null` when the entitlement manager hasn't reported in
 * yet; the trial banner will simply be omitted instead of guessing.
 */
@Composable
fun SplashOverlay(
    versionName: String,
    entitlement: EntitlementManager.Entitlement? = null,
    formattedRemaining: String = ""
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ForsetiColors.SplashBlack),
        contentAlignment = Alignment.Center
    ) {
        // Logo target: 65% of the shorter dimension, capped for tablets so it doesn't
        // crowd the labels stacked below.
        val shorter = if (maxWidth < maxHeight) maxWidth else maxHeight
        val logoSize = (shorter * 0.65f).coerceAtMost(440.dp)
        val scroll = rememberScrollState()

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
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
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayMedium,
                    color = ForsetiColors.RuneGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.titleMedium,
                    color = ForsetiColors.AshWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.splash_version_fmt, versionName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = ForsetiColors.AshGrey,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.splash_subtitle),
                    style = MaterialTheme.typography.labelLarge,
                    color = ForsetiColors.RavenBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (entitlement != null) {
                    TrialSplashBanner(
                        entitlement = entitlement,
                        formattedRemaining = formattedRemaining
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.splash_motto),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForsetiColors.AshWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(12.dp))
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

/**
 * Trial countdown chip shown on the splash for non-purchased users. Purchased
 * users see nothing here — by spec, the splash stays clean for paying users.
 * Loading / Expired states each get their own copy.
 */
@Composable
private fun TrialSplashBanner(
    entitlement: EntitlementManager.Entitlement,
    formattedRemaining: String
) {
    val (label, accent) = when (entitlement) {
        is EntitlementManager.Entitlement.Trial ->
            "Free trial: $formattedRemaining remaining" to ForsetiColors.RuneGold
        is EntitlementManager.Entitlement.TrialEndingSoon ->
            "Trial ending soon — $formattedRemaining remaining" to ForsetiColors.MeadAmber
        is EntitlementManager.Entitlement.Expired ->
            "Trial ended — purchase Forseti to continue" to ForsetiColors.MeadAmber
        EntitlementManager.Entitlement.Loading,
        EntitlementManager.Entitlement.Purchased -> return
    }
    Spacer(Modifier.height(20.dp))
    Surface(
        color = ForsetiColors.Surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(0.9f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.HourglassBottom,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                textAlign = TextAlign.Center
            )
        }
    }
}
