package com.forseti.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.forseti.R
import com.forseti.ForceDarkController
import com.forseti.util.LocalAppLanguage
import com.forseti.ui.screens.BackupScreen
import com.forseti.ui.screens.CaseStudiesScreen
import com.forseti.ui.screens.CasesScreen
import com.forseti.ui.screens.DeadlinesScreen
import com.forseti.ui.screens.DisclaimerOverlay
import com.forseti.ui.screens.GateOverlay
import com.forseti.ui.screens.DraftsScreen
import com.forseti.ui.screens.GlossaryScreen
import com.forseti.ui.screens.GuidesScreen
import com.forseti.ui.screens.LanguagePickerOverlay
import com.forseti.ui.screens.NotesScreen
import com.forseti.ui.screens.QuickJumpScreen
import com.forseti.ui.screens.ReferencesScreen
import com.forseti.ui.screens.ScannerScreen
import com.forseti.ui.screens.SettingsScreen
import com.forseti.ui.screens.StatesScreen
import com.forseti.ui.screens.TutorialOverlay
import com.forseti.ui.screens.UploadedRulesScreen
import com.forseti.ui.sidebar.Sidebar
import com.forseti.util.AppLocale
import com.forseti.util.DisclaimerPrefs

/**
 * CompositionLocal hosting the dark-theme override, set by [MainActivity] so any
 * descendant (currently SettingsScreen) can toggle the theme without threading
 * the prefs reference through every composable.
 */
val LocalForceDark = compositionLocalOf<ForceDarkController> {
    error("LocalForceDark not provided")
}

/**
 * Top-level shell with a two-state navigation model:
 *
 *   1. **Dashboard** (`selectedRoute == null`): the sidebar acts as the home
 *      surface, shown alongside [DashboardWelcomePane] (brand logo, motto,
 *      tagline). This is what the user sees on app launch.
 *   2. **Destination page** (`selectedRoute != null`): the sidebar is hidden
 *      and the chosen screen takes the full window. Every screen's top-bar
 *      back arrow returns to the dashboard by clearing [selectedRoute].
 *
 * First launch: pre-install gate 1 → gate 2 → language (final) → disclaimer → tutorial → main UI.
 */
@Composable
fun ForsetiShell() {
    var selectedRoute: String? by rememberSaveable { mutableStateOf<String?>(null) }
    val selected: Destination? = selectedRoute?.let { route ->
        Destination.entries.firstOrNull { it.route == route }
    }

    val context = LocalContext.current
    val appLanguage = LocalAppLanguage.current
    val prefs = remember { DisclaimerPrefs(context.applicationContext) }

    var disclaimerAccepted by remember { mutableStateOf(prefs.isAccepted) }
    var tutorialCompleted by remember { mutableStateOf(prefs.tutorialCompleted) }

    LaunchedEffect(Unit) {
        if (prefs.isAccepted && !prefs.languageChosen) {
            appLanguage.setTag(AppLocale.guessFromSystem(context))
        }
    }

    BackHandler(enabled = selectedRoute != null) {
        selectedRoute = null
    }

    // Locale change uses activity recreate(); read prefs directly (no rememberSaveable).
    if (!prefs.languageChosen) {
        var preInstall1Done by remember { mutableStateOf(prefs.preInstallDisclaimer1Accepted) }
        var preInstall2Done by remember { mutableStateOf(prefs.preInstallDisclaimer2Accepted) }

        if (!preInstall1Done) {
            GateOverlay(
                title = stringResource(R.string.pre_install_1_title),
                body = stringResource(R.string.pre_install_1_body),
                acceptLabel = stringResource(R.string.pre_install_1_accept),
                onAccept = {
                    prefs.preInstallDisclaimer1Accepted = true
                    preInstall1Done = true
                }
            )
            return
        }
        if (!preInstall2Done) {
            GateOverlay(
                title = stringResource(R.string.pre_install_2_title),
                body = stringResource(R.string.pre_install_2_body),
                acceptLabel = stringResource(R.string.pre_install_2_accept),
                onAccept = {
                    prefs.preInstallDisclaimer2Accepted = true
                    preInstall2Done = true
                }
            )
            return
        }
        var previewTag by remember { mutableStateOf(prefs.languageTag) }
        LanguagePickerOverlay(
            initialTag = previewTag,
            firstRun = true,
            onSelectionChanged = { previewTag = it },
            onContinue = { tag -> appLanguage.setTag(tag) }
        )
        return
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selected == null) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Sidebar(
                        current = null,
                        onSelect = { selectedRoute = it.route }
                    )
                    DashboardWelcomePane(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            } else {
                DestinationContent(
                    destination = selected,
                    onBackToDashboard = { selectedRoute = null }
                )
            }

            if (!disclaimerAccepted) {
                DisclaimerOverlay(onAccept = {
                    prefs.isAccepted = true
                    disclaimerAccepted = true
                })
            } else if (!tutorialCompleted) {
                TutorialOverlay(onComplete = {
                    prefs.tutorialCompleted = true
                    tutorialCompleted = true
                })
            }
        }
    }
}

@Composable
private fun DestinationContent(
    destination: Destination,
    onBackToDashboard: () -> Unit
) {
    val expanded = false
    when (destination) {
        Destination.QuickJump -> QuickJumpScreen(expanded, onBackToDashboard)
        Destination.Drafts -> DraftsScreen(expanded, onBackToDashboard)
        Destination.Guides -> GuidesScreen(expanded, onBackToDashboard)
        Destination.States -> StatesScreen(expanded, onBackToDashboard)
        Destination.Imports -> UploadedRulesScreen(expanded, onBackToDashboard)
        Destination.Deadlines -> DeadlinesScreen(expanded, onBackToDashboard)
        Destination.Cases -> CasesScreen(expanded, onBackToDashboard)
        Destination.Scanner -> ScannerScreen(expanded, onBackToDashboard)
        Destination.CaseStudies -> CaseStudiesScreen(expanded, onBackToDashboard)
        Destination.Backup -> BackupScreen(expanded, onBackToDashboard)
        Destination.Glossary -> GlossaryScreen(expanded, onBackToDashboard)
        Destination.Notes -> NotesScreen(expanded, onBackToDashboard)
        Destination.References -> ReferencesScreen(expanded, onBackToDashboard)
        Destination.Settings -> SettingsScreen(expanded, onBackToDashboard)
    }
}
