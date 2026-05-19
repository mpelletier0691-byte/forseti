package com.forseti.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Brightness2
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.forseti.LocalBilling
import com.forseti.LocalEntitlement
import com.forseti.LocalTts
import com.forseti.billing.EntitlementManager
import com.forseti.billing.TrialBanner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.forseti.BuildConfig
import com.forseti.R
import com.forseti.tts.ReadAloudControls
import com.forseti.ui.shell.ForsetiTopBar
import com.forseti.ui.shell.LocalForceDark
import com.forseti.ui.theme.ForsetiColors
import com.forseti.util.AppLocale
import com.forseti.util.DisclaimerPrefs
import com.forseti.util.LocalAppLanguage

@Composable
fun SettingsScreen(sidebarExpanded: Boolean, onToggleSidebar: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { DisclaimerPrefs(context.applicationContext) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    val appLanguage = LocalAppLanguage.current
    val forceDark = LocalForceDark.current
    val entitlement = LocalEntitlement.current
    val billing = LocalBilling.current
    val tts = LocalTts.current
    val state by entitlement.state.collectAsState()

    val currentLanguageLabel = AppLocale.supported
        .firstOrNull { it.tag == prefs.languageTag }
        ?.displayName
        ?: AppLocale.supported.first().displayName

    val brandTitle = stringResource(R.string.about_brand_title)
    val brandBody = stringResource(R.string.about_brand_body)
    val brandSignoff = stringResource(R.string.about_brand_signoff)
    val disclaimerTitle = stringResource(R.string.settings_disclaimer_title)
    val disclaimerBody = stringResource(R.string.settings_disclaimer_body)
    val languageSectionTitle = stringResource(R.string.settings_section_language)
    val settingsLanguageTitle = stringResource(R.string.settings_language_title)
    val settingsLanguageSubtitle = stringResource(R.string.settings_language_subtitle)
    val trialSectionTitle = stringResource(R.string.settings_section_trial)
    val unlockedTitle = stringResource(R.string.settings_unlocked_title)
    val unlockedBody = stringResource(R.string.settings_unlocked_body)
    val appearanceSectionTitle = stringResource(R.string.settings_section_appearance)
    val forceDarkTitle = stringResource(R.string.settings_force_dark_title)
    val forceDarkSubtitleOn = stringResource(R.string.settings_force_dark_subtitle_on)
    val forceDarkSubtitleOff = stringResource(R.string.settings_force_dark_subtitle_off)
    val contentSectionTitle = stringResource(R.string.settings_section_content)
    val bundledRulesTitle = stringResource(R.string.settings_bundled_rules_title)
    val bundledRulesSubtitle = stringResource(R.string.settings_bundled_rules_subtitle)
    val bookmarksTitle = stringResource(R.string.settings_bookmarks_title)
    val bookmarksSubtitle = stringResource(R.string.settings_bookmarks_subtitle)
    val aboutSectionTitle = stringResource(R.string.settings_section_about)
    val versionLine = stringResource(R.string.settings_version_fmt, BuildConfig.VERSION_NAME)
    val buildLine = stringResource(R.string.settings_build_fmt, BuildConfig.VERSION_CODE)

    /**
     * The full Settings narration. Concatenated lazily inside [ReadAloudControls]
     * so the strings reflect the live trial state when the user taps Read.
     */
    val composeSettingsNarration: () -> String = {
        val msRemaining = when (val s = state) {
            is EntitlementManager.Entitlement.Trial -> s.msRemaining
            is EntitlementManager.Entitlement.TrialEndingSoon -> s.msRemaining
            else -> 0L
        }
        val trialLine = when (state) {
            is EntitlementManager.Entitlement.Purchased ->
                "Forseti is unlocked permanently on this Google account."
            is EntitlementManager.Entitlement.Expired ->
                "Your trial has ended. Purchase Forseti to keep using all features."
            is EntitlementManager.Entitlement.TrialEndingSoon ->
                "Your trial ends soon. ${entitlement.formatRemaining(msRemaining)} remaining."
            is EntitlementManager.Entitlement.Trial ->
                "You're in your free trial. ${entitlement.formatRemaining(msRemaining)} remaining."
            else -> "Loading entitlement state."
        }
        buildString {
            appendLine("Settings.")
            appendLine("Trial and purchase. $trialLine")
            appendLine("Appearance. Force dark theme is ${if (forceDark.value) "on" else "off"}.")
            appendLine("Bundled rules: Federal Rules of Civil Procedure, 2024 restyled edition.")
            appendLine("Bookmarks and notes are stored locally on this device. Wipe by clearing app data.")
            appendLine("About Forseti version ${BuildConfig.VERSION_NAME}, build ${BuildConfig.VERSION_CODE}.")
            appendLine("$brandTitle. $brandBody $brandSignoff")
            appendLine("$disclaimerTitle. $disclaimerBody")
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(ForsetiColors.Background)) {
        ForsetiTopBar(
            title = stringResource(R.string.nav_settings),
            sidebarExpanded = sidebarExpanded,
            onToggleSidebar = onToggleSidebar,
            actions = {
                ReadAloudControls(
                    tts = tts,
                    fetchText = { composeSettingsNarration() },
                    iconTint = ForsetiColors.AshWhite
                )
            }
        )
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            sectionHeader(trialSectionTitle)
            item {
                val msRemaining = when (val s = state) {
                    is EntitlementManager.Entitlement.Trial -> s.msRemaining
                    is EntitlementManager.Entitlement.TrialEndingSoon -> s.msRemaining
                    else -> 0L
                }
                val formatted = entitlement.formatRemaining(msRemaining)
                TrialBanner(entitlement = state, formatted = formatted, billing = billing)
                if (state is EntitlementManager.Entitlement.Purchased) {
                    InfoRow(
                        Icons.Outlined.Verified,
                        unlockedTitle,
                        unlockedBody
                    )
                }
            }

            sectionHeader(languageSectionTitle)
            item {
                InfoRow(
                    icon = Icons.Outlined.Public,
                    title = settingsLanguageTitle,
                    subtitle = "$currentLanguageLabel — $settingsLanguageSubtitle",
                    onClick = { showLanguagePicker = true }
                )
            }

            sectionHeader(appearanceSectionTitle)
            item {
                ToggleRow(
                    icon = Icons.Outlined.Brightness2,
                    title = forceDarkTitle,
                    subtitle = if (forceDark.value) forceDarkSubtitleOn else forceDarkSubtitleOff,
                    checked = forceDark.value,
                    onChange = forceDark.onChange
                )
            }
            sectionHeader(contentSectionTitle)
            item { InfoRow(Icons.Outlined.Description, bundledRulesTitle, bundledRulesSubtitle) }
            item { InfoRow(Icons.Outlined.Bookmark, bookmarksTitle, bookmarksSubtitle) }

            sectionHeader(aboutSectionTitle)
            item { InfoRow(Icons.Outlined.Info, versionLine, buildLine) }
            item {
                InfoRow(
                    Icons.Outlined.Shield,
                    brandTitle,
                    "$brandBody\n\n$brandSignoff"
                )
            }
            item {
                InfoRow(
                    Icons.Outlined.Verified,
                    disclaimerTitle,
                    disclaimerBody
                )
            }
        }
    }

    if (showLanguagePicker) {
        var previewTag by remember { mutableStateOf(prefs.languageTag) }
        LanguagePickerOverlay(
            initialTag = previewTag,
            onSelectionChanged = { previewTag = it },
            onContinue = { tag ->
                appLanguage.setTag(tag)
                showLanguagePicker = false
            },
            onDismiss = { showLanguagePicker = false }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.sectionHeader(title: String) {
    item(key = "h_$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = ForsetiColors.RuneGold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = ForsetiColors.RuneGold)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = ForsetiColors.AshWhite)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ForsetiColors.AshGrey)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = ForsetiColors.MeadAmber)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = ForsetiColors.AshWhite)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ForsetiColors.AshGrey)
            }
        }
    }
}
