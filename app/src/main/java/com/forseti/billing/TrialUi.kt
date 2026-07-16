package com.forseti.billing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.forseti.R
import com.forseti.ui.theme.ForsetiColors
import kotlinx.coroutines.launch

/**
 * Full-screen blocking gate shown when the trial has expired and there is no
 * purchase on the user's account. Launches the Buy / Restore flows directly.
 */
@Composable
fun TrialExpiredScreen(
    billing: BillingService,
    onPurchased: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val billingState by billing.state.collectAsState()
    val product by billing.productDetails.collectAsState()
    val purchased by billing.isPurchased.collectAsState()
    val error by billing.lastError.collectAsState()

    LaunchedEffect(purchased) { if (purchased) onPurchased() }

    Surface(
        color = ForsetiColors.Background,
        modifier = Modifier.fillMaxSize().safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.Lock,
                null,
                tint = ForsetiColors.RuneGold,
                modifier = Modifier.height(64.dp).width(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.trial_expired_title),
                style = MaterialTheme.typography.headlineSmall,
                color = ForsetiColors.RuneGold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.trial_expired_body),
                style = MaterialTheme.typography.bodyLarge,
                color = ForsetiColors.AshGrey,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            product?.let { p ->
                Text(
                    p.oneTimePurchaseOfferDetails?.formattedPrice ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = ForsetiColors.MeadAmber
                )
            }
            Spacer(Modifier.height(24.dp))

            if (billingState == BillingService.BillingState.Unavailable) {
                Text(
                    stringResource(R.string.billing_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForsetiColors.AshGrey,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    val activity = context.findActivity()
                    if (activity != null) billing.launchPurchase(activity)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForsetiColors.RuneGold,
                    contentColor = ForsetiColors.SplashBlack
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.ShoppingCart, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.trial_buy))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { scope.launch { billing.restore() } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Restore, null, tint = ForsetiColors.RuneGold)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.trial_restore), color = ForsetiColors.RuneGold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { context.openPlayListing() }) {
                Text(stringResource(R.string.trial_open_play), color = ForsetiColors.AshGrey)
            }

            if (!error.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForsetiColors.Surface),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        error.orEmpty(),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = ForsetiColors.AshGrey
                    )
                }
            }
        }
    }
}

/** First-launch welcome dialog. */
@Composable
fun TrialWelcomeDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = onAccept,
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForsetiColors.RuneGold,
                    contentColor = ForsetiColors.SplashBlack
                )
            ) { Text("Begin trial") }
        },
        title = { Text(stringResource(R.string.trial_welcome_title), color = ForsetiColors.RuneGold) },
        text = {
            Text(
                stringResource(R.string.trial_welcome_body),
                color = ForsetiColors.AshGrey
            )
        },
        containerColor = ForsetiColors.Surface
    )
}

/** Soft "ending soon" reminder, dismissed once. */
@Composable
fun TrialEndingSoonDialog(
    billing: BillingService,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    context.findActivity()?.let { billing.launchPurchase(it) }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForsetiColors.RuneGold,
                    contentColor = ForsetiColors.SplashBlack
                )
            ) { Text(stringResource(R.string.trial_buy)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Later", color = ForsetiColors.AshGrey) } },
        title = { Text(stringResource(R.string.trial_ending_soon_title), color = ForsetiColors.RuneGold) },
        text = { Text(stringResource(R.string.trial_ending_soon_body), color = ForsetiColors.AshGrey) },
        containerColor = ForsetiColors.Surface
    )
}

/** Confirmation modal after a successful purchase. */
@Composable
fun PurchasedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ForsetiColors.RuneGold,
                    contentColor = ForsetiColors.SplashBlack
                )
            ) { Text("Continue") }
        },
        title = { Text(stringResource(R.string.trial_purchased_title), color = ForsetiColors.RuneGold) },
        text = { Text(stringResource(R.string.trial_purchased_body), color = ForsetiColors.AshGrey) },
        containerColor = ForsetiColors.Surface
    )
}

/** Reusable in-Settings trial banner shown to non-purchased users. */
@Composable
fun TrialBanner(
    entitlement: EntitlementManager.Entitlement,
    formatted: String,
    billing: BillingService
) {
    if (entitlement is EntitlementManager.Entitlement.Purchased ||
        entitlement is EntitlementManager.Entitlement.Loading) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val product by billing.productDetails.collectAsState()
    val priceText = product?.oneTimePurchaseOfferDetails?.formattedPrice ?: "$4.99"

    Card(
        colors = CardDefaults.cardColors(containerColor = ForsetiColors.SurfaceVariant),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lock, null, tint = ForsetiColors.MeadAmber)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val title = when (entitlement) {
                        is EntitlementManager.Entitlement.TrialEndingSoon ->
                            stringResource(R.string.trial_ending_soon_title)
                        is EntitlementManager.Entitlement.Expired ->
                            stringResource(R.string.trial_expired_title)
                        else -> stringResource(R.string.trial_remaining_fmt, formatted)
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium, color = ForsetiColors.AshWhite)
                    Text(
                        "Unlock all features permanently for $priceText \u2014 one\u2011time, no subscription.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForsetiColors.AshGrey
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row {
                Button(
                    onClick = {
                        context.findActivity()?.let { billing.launchPurchase(it) }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForsetiColors.RuneGold,
                        contentColor = ForsetiColors.SplashBlack
                    )
                ) {
                    Icon(Icons.Outlined.ShoppingCart, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.trial_buy))
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { scope.launch { billing.restore() } }) {
                    Text(stringResource(R.string.trial_restore), color = ForsetiColors.RuneGold)
                }
            }
        }
    }
}

internal fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

internal fun Context.openPlayListing() {
    runCatching {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=${packageName.removeSuffix(".debug")}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure {
        runCatching {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${packageName.removeSuffix(".debug")}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
