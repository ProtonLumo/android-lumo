package me.proton.android.lumo.ui.components.subscription

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.proton.android.lumo.R
import me.proton.android.lumo.billing.GoogleProductDetails
import me.proton.android.lumo.billing.SubscriptionState
import me.proton.android.lumo.models.SubscriptionItemResponse
import me.proton.android.lumo.ui.theme.LumoTheme
import me.proton.android.lumo.utils.PriceFormatter
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubscriptionComponent(
    subscriptionState: SubscriptionState.Active,
    subscription: SubscriptionItemResponse,
    googlePlayProductDetails: List<GoogleProductDetails>? = null,
    onManageSubscription: (Context) -> Unit
) {
    // Check if this is a mobile plan (External == 2 indicates a Google Play Store subscription)
    val isMobilePlan = subscription.external == 2

    // Log to verify the values
    if (isMobilePlan) {
        Timber.tag("SubscriptionComponent").i(
            "Mobile plan detected: ${subscription.title}, External=${subscription.external}"
        )
    }

    // Check if plan is cancelled
    val isCancelled = if (isMobilePlan && !subscriptionState.internal) {
        // For mobile plans, use Google Play status
        // A subscription is considered "cancelled" if the user has disabled auto-renewal,
        // even if it's still active until the end of the current billing period
        val isActive = true
        val cancelled = !subscriptionState.renewing
        Timber.tag("SubscriptionComponent").i(
            "Mobile plan cancellation check: isCancelled=$cancelled (!isAutoRenewing=${!subscriptionState.renewing}), isActive=$isActive"
        )
        cancelled
    } else {
        // For web plans, check the API Renew value
        val cancelled = subscription.renew == 0
        Timber.tag("SubscriptionComponent").i(
            "Web plan cancellation check: isCancelled=$cancelled (Renew=${subscription.renew})"
        )
        cancelled
    }

    // Helper function to get Google Play pricing for mobile plans
    fun getGooglePlayPricing(): Pair<String, String>? {
        if (!isMobilePlan || googlePlayProductDetails.isNullOrEmpty()) {
            return null
        }

        // Try to find matching product by checking the subscription name/plan
        // Look for products that match the cycle pattern
        val expectedPeriod = when (subscription.cycle) {
            1 -> "P1M" // Monthly
            12 -> "P1Y" // Yearly
            else -> null
        }

        // Find matching product - look for products containing lumo and matching cycle
        val matchingProduct = googlePlayProductDetails.find { product ->
            val hasMatchingCycle = expectedPeriod?.let { period ->
                product.subscriptionOfferDetails.any { offer ->
                    offer.pricingPhases.any { phase ->
                        phase.billingPeriod == period
                    }
                }
            } ?: false

            // Check if this product matches the subscription cycle
            val isLumoProduct = product.productId.contains("lumo", ignoreCase = true)
            val isCycleMatch = when (subscription.cycle) {
                1 -> product.productId.contains("_1_")
                12 -> product.productId.contains("_12_")
                else -> false
            }

            isLumoProduct && (hasMatchingCycle || isCycleMatch)
        }

        if (matchingProduct?.subscriptionOfferDetails != null) {
            val offer = matchingProduct.subscriptionOfferDetails.firstOrNull()
            val pricingPhase = offer?.pricingPhases?.firstOrNull()

            if (pricingPhase != null) {
                val totalPrice = pricingPhase.formattedPrice
                val periodText = when (pricingPhase.billingPeriod) {
                    "P1M" -> "month"
                    "P1Y" -> "year"
                    else -> if (subscription.cycle == 1) "month" else "year"
                }

                Timber.tag("SubscriptionComponent").i(
                    "Found Google Play pricing: $totalPrice per $periodText for product ${matchingProduct.productId}"
                )
                return Pair(totalPrice, periodText)
            }
        }

        Timber.tag("SubscriptionComponent").i(
            "No matching Google Play product found for subscription cycle ${subscription.cycle}"
        )
        return null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(
                width = 1.dp,
                color = LumoTheme.colors.borderNorm,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(LumoTheme.colors.backgroundNorm)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - Plan name and description
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Get plan title from either direct Title field or from Plans array
                        val planTitle = subscription.title ?: subscription.name

                        planTitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleMedium,
                                color = LumoTheme.colors.primary
                            )
                        }

                        // Show cancellation badge if cancelled
                        if (isCancelled) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFFFFECEC),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.cancelled),
                                    fontSize = 12.sp,
                                    color = Color(0xFFE53935),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Determine renewal date and status:
                    // For mobile plans, use Google Play status if available
                    // For web plans, use the API subscription information
                    val (renewalDate, isRenewing) = if (isMobilePlan && !subscriptionState.internal) {
                        // Use Google Play subscription info for mobile plans
                        val isAutoRenewing = subscriptionState.renewing
                        val expiryTimeMillis = subscriptionState.expiryTimeMillis
                        // Format the expiry date
                        val date = if (expiryTimeMillis > 0) {
                            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            dateFormat.format(Date(expiryTimeMillis))
                        } else {
                            // Fallback to API data if Google Play doesn't provide expiry
                            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            dateFormat.format(Date(subscription.periodEnd * 1000))
                        }

                        Pair(date, isAutoRenewing)
                    } else {
                        // Use API subscription info for web plans
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val date = if (subscription.periodEnd > 0) {
                            dateFormat.format(Date(subscription.periodEnd * 1000))
                        } else "Unknown"

                        Pair(date, subscription.renew == 1)
                    }

                    val message = if (isRenewing) {
                        stringResource(id = R.string.subscription_renews, renewalDate)
                    } else {
                        stringResource(id = R.string.subscription_expires, renewalDate)
                    }

                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = LumoTheme.colors.textNorm,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Show cycle description if available
                    subscription.cycleDescription?.let {
                        Text(
                            text = it,
                            fontSize = 14.sp,
                            color = LumoTheme.colors.textWeak
                        )
                    }
                }

                // Right side - Price info
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    // Use Google Play pricing for mobile plans when available, otherwise fall back to API pricing
                    // This ensures that the amount displayed matches what was actually charged by Google Play
                    val (priceText, periodText) = if (isMobilePlan) {
                        val googlePlayPricing = getGooglePlayPricing()
                        if (googlePlayPricing != null) {
                            Timber.tag("SubscriptionComponent").i(
                                "Using Google Play pricing: ${googlePlayPricing.first} per ${googlePlayPricing.second}"
                            )
                            Pair(googlePlayPricing.first, googlePlayPricing.second)
                        } else {
                            Timber.tag("SubscriptionComponent").i(
                                "Falling back to API pricing for mobile plan"
                            )
                            val formattedPrice = PriceFormatter.formatPrice(
                                subscription.amount,
                                subscription.currency
                            )
                            val period = if (subscription.cycle == 1) "month" else "year"
                            Pair(formattedPrice, period)
                        }
                    } else {
                        // For web plans, always use API pricing
                        val formattedPrice =
                            PriceFormatter.formatPrice(subscription.amount, subscription.currency)
                        val period = if (subscription.cycle == 1) "month" else "year"
                        Pair(formattedPrice, period)
                    }

                    Text(
                        text = priceText,
                        style = MaterialTheme.typography.titleSmall,
                        color = LumoTheme.colors.textNorm
                    )
                    Text(
                        text = "a $periodText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LumoTheme.colors.textWeak
                    )
                }
            }

            // Show entitlements if available
            subscription.entitlements?.let { entitlements ->
                if (entitlements.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        entitlements.forEach { entitlement ->
                            if (entitlement.type.equals("description", ignoreCase = true)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = LumoTheme.colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = entitlement.text,
                                        fontSize = 14.sp,
                                        color = LumoTheme.colors.textWeak
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Conditionally show either the manage button or the info message
            if (isMobilePlan) {
                // Show Manage subscription button for mobile plans (Google Play Store)
                val context = LocalContext.current
                Button(
                    onClick = { onManageSubscription(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LumoTheme.colors.primary,
                        contentColor = LumoTheme.colors.textInvert,
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.subscription_manage),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                // Show message for web-based plans
                Text(
                    text = stringResource(id = R.string.subscription_manage_info),
                    fontSize = 14.sp,
                    color = LumoTheme.colors.textWeak,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}
