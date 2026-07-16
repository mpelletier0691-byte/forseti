package com.forseti.ui.shell

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.forseti.ui.theme.ForsetiScaffoldContentInsets

/**
 * Standard destination chrome: [ForsetiTopBar] in the Scaffold top slot with
 * edge-to-edge insets split correctly (status bar on the bar, sides/bottom on
 * content). Use for every full-screen tab reached from the dashboard.
 */
@Composable
fun ForsetiScreenScaffold(
    title: String,
    sidebarExpanded: Boolean,
    onBackToDashboard: () -> Unit,
    snackbarHostState: SnackbarHostState? = null,
    floatingActionButton: @Composable () -> Unit = {},
    topBarActions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = ForsetiScaffoldContentInsets,
        topBar = {
            ForsetiTopBar(
                title = title,
                sidebarExpanded = sidebarExpanded,
                onToggleSidebar = onBackToDashboard,
                actions = topBarActions
            )
        },
        snackbarHost = {
            snackbarHostState?.let { SnackbarHost(it) }
        },
        floatingActionButton = floatingActionButton
    ) { padding ->
        content(padding)
    }
}
