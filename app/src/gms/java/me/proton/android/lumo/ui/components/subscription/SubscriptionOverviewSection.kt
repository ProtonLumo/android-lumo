package me.proton.android.lumo.ui.components.subscription

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import me.proton.android.lumo.billing.GoogleProductDetails
import me.proton.android.lumo.billing.SubscriptionState
import me.proton.android.lumo.ui.theme.LumoTheme
import timber.log.Timber

/**
 * Displays an overview of a user's current subscriptions
 */
@Composable
fun SubscriptionOverviewSection(
    activeSubscriptions: SubscriptionState.Active,
    googleProductDetails: List<GoogleProductDetails>,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Subscription Overview",
            style = MaterialTheme.typography.titleLarge,
            color = LumoTheme.colors.textNorm,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Use the SubscriptionComponent for each subscription
        for (subscription in activeSubscriptions.subscriptions) {
            // Debug log the subscription info
            Timber.tag(TAG).i(
                "Subscription: Name=${subscription.name}, External=${subscription.external}, Renew=${subscription.renew}"
            )

            // For mobile plans (External==2), always pass the Google Play status
            // This ensures we show the correct cancellation status from Google Play
            val isGooglePlayPlan = subscription.name?.contains("lumo", ignoreCase = true) == true &&
                    subscription.external == 2

            if (isGooglePlayPlan) {
                Timber.tag(TAG).i(
                    "This is a Google Play Lumo plan - using Google Play status and product details"
                )
            }

            SubscriptionComponent(
                subscriptionState = activeSubscriptions,
                subscription = subscription,
                googlePlayProductDetails = if (isGooglePlayPlan) googleProductDetails else null,
                onManageSubscription = {
                    openSubscriptionManagementScreen(it)
                    onClose()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun openSubscriptionManagementScreen(context: Context): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = "https://play.google.com/store/account/subscriptions".toUri()

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            Timber.tag(TAG).i("Opened Google Play subscription management screen")
            true
        } else {
            // Fallback if the URI method doesn't work
            val playStoreIntent = Intent(Intent.ACTION_VIEW)
            playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            playStoreIntent.data =
                "https://play.google.com/store/apps/details?id=com.android.vending".toUri()

            if (playStoreIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(playStoreIntent)
                Timber.tag(TAG).i("Opened Google Play Store")
                true
            } else {
                Timber.tag(TAG).e("Could not open Google Play Store ")
                false
            }
        }

    } catch (e: Exception) {
        Timber.tag(TAG).e(e, "Error opening subscription management ")
        false
    }
}

private const val TAG = "SubscriptionOverview"
