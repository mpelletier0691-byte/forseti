package com.forseti.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.forseti.R
import com.forseti.ui.theme.ForsetiColors
import com.forseti.ui.theme.ForsetiTopBarWindowInsets

/**
 * Compact top bar with a leading back arrow that returns the user to the
 * dashboard (sidebar + welcome pane) and a runic gold gradient underline divider.
 *
 * The parameters are named [sidebarExpanded] / [onToggleSidebar] for backward
 * compatibility with every screen call site, but the contract is now:
 *   • The leading icon is always a back arrow.
 *   • Tapping it invokes [onToggleSidebar] — wired up in [ForsetiShell] to
 *     clear the selected destination and return to the dashboard.
 *   • [sidebarExpanded] is unused (the sidebar is only shown on the dashboard).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForsetiTopBar(
    title: String,
    @Suppress("UNUSED_PARAMETER") sidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    Column {
        TopAppBar(
            windowInsets = ForsetiTopBarWindowInsets,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = ForsetiColors.AshWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onToggleSidebar) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back_to_dashboard),
                        tint = ForsetiColors.AshWhite
                    )
                }
            },
            actions = {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    actions()
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = ForsetiColors.Background,
                titleContentColor = ForsetiColors.AshWhite,
                navigationIconContentColor = ForsetiColors.AshWhite
            )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            ForsetiColors.Background,
                            ForsetiColors.RuneGoldDim,
                            ForsetiColors.RuneGold,
                            ForsetiColors.RuneGoldDim,
                            ForsetiColors.Background
                        )
                    )
                )
        )
    }
}
