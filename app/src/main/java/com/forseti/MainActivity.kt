package com.forseti

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.forseti.BuildConfig
import com.forseti.billing.BillingService
import com.forseti.billing.EntitlementManager
import com.forseti.billing.PurchasedDialog
import com.forseti.billing.TrialEndingSoonDialog
import com.forseti.billing.TrialExpiredScreen
import com.forseti.billing.TrialPrefs
import com.forseti.billing.TrialWelcomeDialog
import com.forseti.tts.ForsetiTts
import com.forseti.ui.shell.ForsetiShell
import com.forseti.ui.shell.SplashOverlay
import com.forseti.ui.shell.LocalForceDark
import com.forseti.ui.theme.ForsetiColors
import com.forseti.ui.theme.ForsetiTheme
import com.forseti.ui.theme.setupForsetiEdgeToEdge
import com.forseti.util.AppLanguageController
import com.forseti.util.AppLocale
import com.forseti.util.DisclaimerPrefs
import com.forseti.util.LocalAppLanguage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow

val LocalEntitlement = compositionLocalOf<EntitlementManager> {
    error("LocalEntitlement not provided")
}
val LocalBilling = compositionLocalOf<BillingService> {
    error("LocalBilling not provided")
}
val LocalTts = compositionLocalOf<ForsetiTts> {
    error("LocalTts not provided")
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var entitlement: EntitlementManager
    @Inject lateinit var billing: BillingService
    @Inject lateinit var trialPrefs: TrialPrefs
    @Inject lateinit var tts: ForsetiTts

    private val firstFrameDrawn = MutableStateFlow(false)

    override fun onPause() {
        super.onPause()
        tts.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) tts.shutdown()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !firstFrameDrawn.value }
        super.onCreate(savedInstanceState)

        entitlement.start()

        setupForsetiEdgeToEdge()

        val prefs = DisclaimerPrefs(applicationContext)
        val appLanguage = AppLanguageController { tag ->
            val previous = prefs.languageTag
            val wasChosen = prefs.languageChosen
            AppLocale.applyAndPersist(applicationContext, tag)
            if (tag != previous || !wasChosen) {
                // Never recreate() synchronously from a Compose click — post to the UI queue.
                window.decorView.post {
                    if (!isFinishing && !isDestroyed) recreate()
                }
            }
        }

        setContent {
            var forceDark by remember { mutableStateOf(prefs.forceDark) }
            ForsetiTheme(forceDark = forceDark) {
                CompositionLocalProvider(
                    LocalAppLanguage provides appLanguage,
                    LocalForceDark provides ForceDarkController(
                        value = forceDark,
                        onChange = {
                            prefs.forceDark = it
                            forceDark = it
                        }
                    ),
                    LocalEntitlement provides entitlement,
                    LocalBilling provides billing,
                    LocalTts provides tts
                ) {
                    val viewModel: BootstrapViewModel = hiltViewModel()
                    val ready by viewModel.ready.collectAsState()
                    val splashEntitlement by entitlement.state.collectAsState()
                    LaunchedEffect(Unit) { firstFrameDrawn.value = true }
                    if (!ready) {
                        val msRemaining = when (val s = splashEntitlement) {
                            is EntitlementManager.Entitlement.Trial -> s.msRemaining
                            is EntitlementManager.Entitlement.TrialEndingSoon -> s.msRemaining
                            else -> 0L
                        }
                        SplashOverlay(
                            versionName = BuildConfig.VERSION_NAME,
                            entitlement = splashEntitlement,
                            formattedRemaining = entitlement.formatRemaining(msRemaining)
                        )
                    } else {
                        TrialGate(trialPrefs = trialPrefs)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrialGate(trialPrefs: TrialPrefs) {
    val entitlement = LocalEntitlement.current
    val billing = LocalBilling.current
    val state by entitlement.state.collectAsState()
    val purchased by billing.isPurchased.collectAsState()

    var showWelcome by remember { mutableStateOf(!trialPrefs.welcomeShown) }
    var showEndingSoon by remember { mutableStateOf(false) }
    var showPurchasedToast by remember { mutableStateOf(false) }
    var lastPurchased by remember { mutableStateOf(purchased) }

    LaunchedEffect(state) {
        if (state is EntitlementManager.Entitlement.TrialEndingSoon &&
            !trialPrefs.endingSoonShown
        ) {
            showEndingSoon = true
        }
    }
    LaunchedEffect(purchased) {
        if (purchased && !lastPurchased) showPurchasedToast = true
        lastPurchased = purchased
    }

    when (state) {
        EntitlementManager.Entitlement.Expired -> TrialExpiredScreen(billing) { }
        else -> ForsetiShell()
    }

    if (showWelcome) {
        TrialWelcomeDialog(onAccept = {
            trialPrefs.welcomeShown = true
            showWelcome = false
        })
    }
    if (showEndingSoon) {
        TrialEndingSoonDialog(billing = billing, onDismiss = {
            trialPrefs.endingSoonShown = true
            showEndingSoon = false
        })
    }
    if (showPurchasedToast) {
        PurchasedDialog(onDismiss = { showPurchasedToast = false })
    }
}

data class ForceDarkController(
    val value: Boolean,
    val onChange: (Boolean) -> Unit
)
