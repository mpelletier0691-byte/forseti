package com.forseti.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forseti.R
import com.forseti.states.StateCacheManager
import com.forseti.states.StateRule
import com.forseti.states.StateRulesCatalog
import com.forseti.ui.shell.ForsetiTopBar
import com.forseti.ui.theme.ForsetiColors

@Composable
fun StatesScreen(
    sidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    viewModel: StatesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val cached by viewModel.cached.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    val refreshSummary by viewModel.refreshSummary.collectAsState()
    var query by remember { mutableStateOf("") }

    val states = remember(query) { StateRulesCatalog.states.filterByQuery(query) }
    val circuits = remember(query) { StateRulesCatalog.circuits.filterByQuery(query) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshSummary) {
        refreshSummary?.let { s ->
            val msg = when {
                s.attempted == 0 -> "Nothing cached yet — long-press an entry to download."
                s.failed == 0 -> "Refreshed ${s.refreshed} of ${s.attempted} cached rule(s)."
                else -> "Refreshed ${s.refreshed}, failed ${s.failed} of ${s.attempted}."
            }
            snackbar.showSnackbar(msg)
            viewModel.consumeRefreshSummary()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).background(ForsetiColors.Background)) {
        ForsetiTopBar(
            title = stringResource(R.string.nav_states),
            sidebarExpanded = sidebarExpanded,
            onToggleSidebar = onToggleSidebar
        )
        SourcingBanner(
            cachedCount = cached.size,
            refreshing = refreshing,
            onCheckUpdates = { viewModel.checkForUpdates() }
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.states_filter_hint)) },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = ForsetiColors.Surface,
                unfocusedContainerColor = ForsetiColors.Surface,
                focusedIndicatorColor = ForsetiColors.RuneGold,
                cursorColor = ForsetiColors.RuneGold
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            section("States & D.C.") {
                items(states, key = { it.abbreviation }) { rule ->
                    StateRow(
                        rule = rule,
                        cached = cached.contains(StateCacheManager.urlKey(rule.url)),
                        onOpen = { context.tryOpenUrl(rule.url, snackbar, scope) },
                        onOpenFallback = rule.fallbackUrl?.let { fb ->
                            { context.tryOpenUrl(fb, snackbar, scope) }
                        },
                        onLongPress = {
                            val wasCached = cached.contains(StateCacheManager.urlKey(rule.url))
                            viewModel.toggleDownload(rule)
                            scope.launch {
                                snackbar.showSnackbar(
                                    if (wasCached) "Removed offline copy of ${rule.name}"
                                    else "Downloading ${rule.name}\u2026"
                                )
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            section("Federal & Circuit Rules") {
                items(circuits, key = { it.abbreviation }) { rule ->
                    StateRow(
                        rule = rule,
                        cached = cached.contains(StateCacheManager.urlKey(rule.url)),
                        onOpen = { context.tryOpenUrl(rule.url, snackbar, scope) },
                        onOpenFallback = rule.fallbackUrl?.let { fb ->
                            { context.tryOpenUrl(fb, snackbar, scope) }
                        },
                        onLongPress = {
                            val wasCached = cached.contains(StateCacheManager.urlKey(rule.url))
                            viewModel.toggleDownload(rule)
                            scope.launch {
                                snackbar.showSnackbar(
                                    if (wasCached) "Removed offline copy of ${rule.name}"
                                    else "Downloading ${rule.name}\u2026"
                                )
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
            item {
                Text(
                    text = "Tap to open in your browser. Long-press to download for offline use; long-press again to remove. " +
                        "If a deep link 404s, tap the courthouse icon to jump to the state's main judiciary site, or use the Uploaded Rules tab to bring in your own PDF.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ForsetiColors.AshGrey
                )
            }
        }
    }
    }
}

private inline fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    body: () -> Unit
) {
    item(key = "header_$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = ForsetiColors.RuneGold,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )
    }
    body()
}

@Composable
private fun SourcingBanner(
    cachedCount: Int,
    refreshing: Boolean,
    onCheckUpdates: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.SurfaceVariant),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VerifiedUser, null, tint = ForsetiColors.RuneGold)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Official .gov sources",
                    style = MaterialTheme.typography.titleSmall,
                    color = ForsetiColors.RuneGold,
                    modifier = Modifier.weight(1f)
                )
                if (cachedCount > 0) {
                    Text(
                        "$cachedCount cached",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForsetiColors.MeadAmber
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Each link points to the state's official judiciary or legislature website. Long-press a row to cache it for offline use; tap \u201CCheck for updates\u201D to refresh from the authoritative source.",
                style = MaterialTheme.typography.bodySmall,
                color = ForsetiColors.AshGrey
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onCheckUpdates,
                    enabled = !refreshing && cachedCount > 0
                ) {
                    if (refreshing) {
                        CircularProgressIndicator(
                            color = ForsetiColors.RuneGold,
                            strokeWidth = 2.dp,
                            modifier = Modifier.height(16.dp).width(16.dp)
                        )
                    } else {
                        Icon(Icons.Outlined.Refresh, null, tint = ForsetiColors.RuneGold)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (refreshing) "Checking…" else "Check for updates",
                        color = ForsetiColors.RuneGold
                    )
                }
                if (cachedCount == 0) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Long-press any row first to cache it.",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForsetiColors.AshGrey
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StateRow(
    rule: StateRule,
    cached: Boolean,
    onOpen: () -> Unit,
    onOpenFallback: (() -> Unit)?,
    onLongPress: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.width(40.dp).height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rule.abbreviation,
                    style = MaterialTheme.typography.titleMedium,
                    color = ForsetiColors.MeadAmber
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.name, style = MaterialTheme.typography.titleMedium, color = ForsetiColors.AshWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = rule.rulesTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ForsetiColors.AshGrey,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    if (rule.isIndexPage) {
                        Text(
                            "Index page",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = ForsetiColors.RavenBlue,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    Text(
                        "Verified ${rule.lastVerified}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = ForsetiColors.AshGrey
                    )
                }
            }
            IconButton(onClick = onOpen) {
                Icon(Icons.Outlined.OpenInBrowser, "Open rule page", tint = ForsetiColors.RavenBlue)
            }
            if (onOpenFallback != null) {
                IconButton(onClick = onOpenFallback) {
                    Icon(
                        Icons.Outlined.AccountBalance,
                        "Open courts homepage if rule page 404s",
                        tint = ForsetiColors.AshGrey
                    )
                }
            }
            IconButton(onClick = onLongPress) {
                Icon(
                    imageVector = if (cached) Icons.Outlined.CloudDone else Icons.Outlined.CloudDownload,
                    contentDescription = if (cached) "Remove offline copy" else "Download for offline",
                    tint = if (cached) ForsetiColors.RuneGold else ForsetiColors.AshGrey
                )
            }
        }
    }
}

private fun List<StateRule>.filterByQuery(q: String): List<StateRule> {
    if (q.isBlank()) return this
    val needle = q.trim().lowercase()
    return filter {
        it.name.lowercase().contains(needle) ||
            it.abbreviation.lowercase().contains(needle) ||
            it.rulesTitle.lowercase().contains(needle)
    }
}

private fun android.content.Context.tryOpenUrl(
    url: String,
    snackbar: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(intent) }.onFailure {
        scope.launch { snackbar.showSnackbar("No browser available to open this link") }
    }
}
