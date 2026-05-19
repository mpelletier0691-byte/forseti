package com.forseti.ui.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.forseti.LocalBilling
import com.forseti.LocalEntitlement
import com.forseti.R
import com.forseti.billing.EntitlementManager
import com.forseti.billing.findActivity
import com.forseti.ui.shell.Destination
import com.forseti.ui.theme.ForsetiColors

/**
 * Persistent dashboard rail. The sidebar is the app's home: it always renders
 * the brand header, the destination list, the trial banner, and the legal
 * footer. There is intentionally no collapse affordance — when the user picks
 * a destination, [ForsetiShell] replaces the entire dashboard view with that
 * page, and the page's top-bar back arrow brings the user back here.
 *
 * [current] is nullable: pass null while the user is on the dashboard so no
 * row appears selected.
 */
@Composable
fun Sidebar(
    current: Destination?,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(ForsetiColors.Sidebar)
    ) {
        SidebarHeader()
        HorizontalDivider(color = ForsetiColors.Stone)
        // The destination list owns all the flexible vertical space so that on
        // shorter screens — or as we add more tabs in future releases — users
        // can scroll the dashboard items independently of the pinned trial
        // banner and footer disclaimer below.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(Destination.SidebarOrder) { dest ->
                SidebarItem(
                    destination = dest,
                    selected = dest == current,
                    onClick = { onSelect(dest) }
                )
            }
        }
        SidebarTrialBanner()
        HorizontalDivider(color = ForsetiColors.Stone)
        Text(
            text = stringResource(R.string.footer_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = ForsetiColors.AshGrey,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun SidebarTrialBanner() {
    val entitlement = LocalEntitlement.current
    val billing = LocalBilling.current
    val context = LocalContext.current
    val state by entitlement.state.collectAsState()
    val remainingMs = when (val s = state) {
        is EntitlementManager.Entitlement.Trial -> s.msRemaining
        is EntitlementManager.Entitlement.TrialEndingSoon -> s.msRemaining
        else -> null
    } ?: return
    val formatted = entitlement.formatRemaining(remainingMs)
    val accent = when (state) {
        is EntitlementManager.Entitlement.TrialEndingSoon -> ForsetiColors.MeadAmber
        else -> ForsetiColors.RuneGold
    }
    Surface(
        color = ForsetiColors.SurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                context.findActivity()?.let { billing.launchPurchase(it) }
            }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.sidebar_trial_heading),
                style = MaterialTheme.typography.titleSmall,
                color = accent
            )
            Text(
                text = stringResource(R.string.sidebar_trial_cta),
                style = MaterialTheme.typography.bodySmall,
                color = ForsetiColors.AshWhite
            )
            Text(
                text = stringResource(R.string.sidebar_trial_remaining, formatted),
                style = MaterialTheme.typography.labelSmall,
                color = ForsetiColors.AshGrey
            )
        }
    }
}

@Composable
private fun SidebarHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ForsetiColors.SplashBlack)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_logo_full),
                contentDescription = stringResource(R.string.cd_app_logo),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = ForsetiColors.RuneGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.labelSmall,
                color = ForsetiColors.AshGrey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SidebarItem(
    destination: Destination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) ForsetiColors.SidebarSelected else ForsetiColors.Sidebar
    val tint = if (selected) ForsetiColors.RuneGold else ForsetiColors.AshWhite
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = stringResource(destination.titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
