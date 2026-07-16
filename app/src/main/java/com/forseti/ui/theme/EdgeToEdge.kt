package com.forseti.ui.theme

import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat

/**
 * Android 15 (SDK 35) edge-to-edge compliance.
 *
 * Google Play requires apps targeting SDK 35 to draw behind system bars and handle
 * insets. We call [enableEdgeToEdge] on every activity (backward compatible to API 26)
 * and apply Compose [WindowInsets] on scaffolds and top bars.
 *
 * @see <a href="https://developer.android.com/develop/ui/views/layout/edge-to-edge">Edge-to-edge guide</a>
 */
fun ComponentActivity.setupForsetiEdgeToEdge() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
    }
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(
            lightScrim = Color.TRANSPARENT,
            darkScrim = Color.TRANSPARENT
        ),
        navigationBarStyle = SystemBarStyle.auto(
            lightScrim = Color.TRANSPARENT,
            darkScrim = Color.TRANSPARENT
        )
    )
}

/** Status-bar insets for [com.forseti.ui.shell.ForsetiTopBar] under edge-to-edge. */
@OptIn(ExperimentalMaterial3Api::class)
val ForsetiTopBarWindowInsets: WindowInsets
    @Composable get() = TopAppBarDefaults.windowInsets

/**
 * Scaffold content insets when a [com.forseti.ui.shell.ForsetiTopBar] occupies the top
 * slot (status bar handled by the bar; sides + nav bar on content).
 */
val ForsetiScaffoldContentInsets: WindowInsets
    @Composable
    get() = ScaffoldDefaults.contentWindowInsets.only(
        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
    )

/**
 * Full safe drawing insets for standalone screens (case detail drill-in, overlays)
 * that do not use [com.forseti.ui.shell.ForsetiScreenScaffold].
 */
val ForsetiStandaloneContentInsets: WindowInsets
    @Composable
    get() = ScaffoldDefaults.contentWindowInsets

/** Shell root: child destinations manage their own insets (see [ForsetiDestinationScaffold]). */

/**
 * Standard destination [Scaffold] used by tabs that embed [ForsetiTopBar] in the top slot
 * (Scanner, Deadlines, Drafts, etc.).
 */
@Composable
fun ForsetiDestinationScaffold(
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        contentWindowInsets = ForsetiScaffoldContentInsets,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        topBar = topBar,
        content = content
    )
}
