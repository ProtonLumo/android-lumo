package me.proton.android.lumo.ui.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.proton.android.lumo.R
import me.proton.android.lumo.billing.BillingManager
import me.proton.android.lumo.models.SubscriptionItemResponse
import java.util.Date

/**
 * Displays an overview of a user's current subscriptions
 */
@Composable
fun SubscriptionOverviewSection(
    billingManager: BillingManager,
    subscriptions: List<SubscriptionItemResponse>,
    onClose: () -> Unit
) {
    // Get Google Play subscription information
    val (isActive, isAutoRenewing, expiryTimeMillis) = billingManager.getSubscriptionStatus()

    // Get Google Play product details for pricing
    val googlePlayProductDetails =
        billingManager.productDetailsList.collectAsStateWithLifecycle().value

    // Log Google Play status
    Log.d(
        "SubscriptionOverview",
        "Google Play Status: isActive=$isActive, isAutoRenewing=$isAutoRenewing, expiryTime=${
            Date(expiryTimeMillis)
        }"
    )
    Log.d(
        "SubscriptionOverview",
        "Google Play Products: ${googlePlayProductDetails.size} available"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Close Button
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Lumo+ Image
        Image(
            painter = painterResource(id = R.drawable.lumo_cat_on_laptop),
            contentDescription = "Lumo Plus",
            modifier = Modifier.height(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Subscription Overview",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Use the SubscriptionComponent for each subscription
        for (subscription in subscriptions) {
            // Debug log the subscription info
            Log.d(
                "SubscriptionOverview",
                "Subscription: Name=${subscription.Name}, External=${subscription.External}, Renew=${subscription.Renew}"
            )

            // For mobile plans (External==2), always pass the Google Play status
            // This ensures we show the correct cancellation status from Google Play
            val isGooglePlayPlan = subscription.Name?.contains("lumo", ignoreCase = true) == true &&
                    subscription.External == 2

            if (isGooglePlayPlan) {
                Log.d(
                    "SubscriptionOverview",
                    "This is a Google Play Lumo plan - using Google Play status and product details"
                )
            }

            SubscriptionComponent(
                subscription = subscription,
                // Always pass Google Play status for Lumo plans with External==2
                googlePlayRenewalStatus = if (isGooglePlayPlan) Triple(
                    isActive,
                    isAutoRenewing,
                    expiryTimeMillis
                ) else null,
                // Pass Google Play product details for mobile plans to get accurate pricing
                googlePlayProductDetails = if (isGooglePlayPlan) googlePlayProductDetails else null,
                onManageSubscription = {
                    billingManager.openSubscriptionManagementScreen()
                    onClose()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
} 