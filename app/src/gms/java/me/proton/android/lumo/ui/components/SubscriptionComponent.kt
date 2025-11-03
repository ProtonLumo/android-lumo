package me.proton.android.lumo.ui.components

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import android.util.Log
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.ProductDetails
import me.proton.android.lumo.R
import me.proton.android.lumo.models.SubscriptionEntitlement
import me.proton.android.lumo.models.SubscriptionItemResponse
import me.proton.android.lumo.ui.theme.LumoTheme
import me.proton.android.lumo.utils.PriceFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubscriptionComponent(
    subscription: SubscriptionItemResponse,
    googlePlayRenewalStatus: Triple<Boolean, Boolean, Long>? = null,
    googlePlayProductDetails: List<ProductDetails>? = null,
    onManageSubscription: (Context) -> Unit
) {
    // Check if this is a mobile plan (External == 2 indicates a Google Play Store subscription)
    val isMobilePlan = subscription.external == 2

    // Log to verify the values
    if (isMobilePlan) {
        Log.d(
            "SubscriptionComponent",
            "Mobile plan detected: ${subscription.title}, External=${subscription.external}"
        )
        if (googlePlayRenewalStatus != null) {
            val (isActive, isAutoRenewing, expiryTime) = googlePlayRenewalStatus
            Log.d(
                "SubscriptionComponent",
                "Google Play Status: isActive=$isActive, isAutoRenewing=$isAutoRenewing, expiryTime=${
                    Date(expiryTime)
                }"
            )
        } else {
            Log.d(
                "SubscriptionComponent",
                "WARNING: googlePlayRenewalStatus is null for mobile plan"
            )
        }
    }

    // Check if plan is cancelled
    val isCancelled = if (isMobilePlan && googlePlayRenewalStatus != null) {
        // For mobile plans, use Google Play status
        // A subscription is considered "cancelled" if the user has disabled auto-renewal,
        // even if it's still active until the end of the current billing period
        val (isActive, isAutoRenewing, _) = googlePlayRenewalStatus
        val cancelled = !isAutoRenewing
        Log.d(
            "SubscriptionComponent",
            "Mobile plan cancellation check: isCancelled=$cancelled (!isAutoRenewing=${!isAutoRenewing}), isActive=$isActive"
        )
        cancelled
    } else {
        // For web plans, check the API Renew value
        val cancelled = subscription.renew == 0
        Log.d(
            "SubscriptionComponent",
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
                product.subscriptionOfferDetails?.any { offer ->
                    offer.pricingPhases.pricingPhaseList.any { phase ->
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
            val offer = matchingProduct.subscriptionOfferDetails!!.firstOrNull()
            val pricingPhase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()

            if (pricingPhase != null) {
                val totalPrice = pricingPhase.formattedPrice
                val periodText = when (pricingPhase.billingPeriod) {
                    "P1M" -> "month"
                    "P1Y" -> "year"
                    else -> if (subscription.cycle == 1) "month" else "year"
                }

                Log.d(
                    "SubscriptionComponent",
                    "Found Google Play pricing: $totalPrice per $periodText for product ${matchingProduct.productId}"
                )
                return Pair(totalPrice, periodText)
            }
        }

        Log.d(
            "SubscriptionComponent",
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
                    val (renewalDate, isRenewing) = if (isMobilePlan && googlePlayRenewalStatus != null) {
                        // Use Google Play subscription info for mobile plans
                        val (isActive, isAutoRenewing, expiryTimeMillis) = googlePlayRenewalStatus

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
                            Log.d(
                                "SubscriptionComponent",
                                "Using Google Play pricing: ${googlePlayPricing.first} per ${googlePlayPricing.second}"
                            )
                            Pair(googlePlayPricing.first, googlePlayPricing.second)
                        } else {
                            Log.d(
                                "SubscriptionComponent",
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

@Preview(showBackground = true)
@Preview(name = "Dark - Loading Plans", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Composable
fun SubscriptionComponentPreview() {
    // Preview both types of subscription structures based on the real API format
    val previewSubscriptions = listOf(
        // Web subscription (Mail Plus)
        SubscriptionItemResponse(
            id = "aaa==",
            invoiceId = "-aaa-7gm5YLf215MEgZCdzOtLW5psxgB8oNc8OnoFRykab4Z23EGEW1ka3GtQPF9xwx9-VUA==",
            title = "Mail Plus",
            description = "Current plan",
            name = "mail2022",
            cycle = 12,
            cycleDescription = "For 1 year",
            currency = "CHF",
            amount = 4788,
            offer = "default",
            periodStart = System.currentTimeMillis() / 1000,
            periodEnd = (System.currentTimeMillis() + 365 * 24 * 60 * 60) / 1000,
            createTime = System.currentTimeMillis() / 1000,
            couponCode = null,
            discount = 0,
            renewDiscount = 0,
            renewAmount = 4788,
            renew = 0,
            external = 0,
            billingPlatform = 1,
            entitlements = listOf(
                SubscriptionEntitlement(
                    type = "description",
                    iconName = "checkmark",
                    text = "And the free features of all other Proton products!"
                )
            ),
            decorations = emptyList(),
            isTrial = false,
            customerID = null
        ),
        // Mobile subscription (Lumo Plus)
        SubscriptionItemResponse(
            id = "nNTtf0H8g-aaa==",
            invoiceId = "aaa-ZTD8H8F6LvNaSjMaPxB5ecFkA7y-5kc3q38cGumJENGHjtSoUndkYFUx0_xlJeg==",
            title = "Lumo Plus",
            description = "Current plan",
            name = "lumo2024",
            cycle = 1,
            cycleDescription = "For 1 month",
            currency = "CHF",
            amount = 1299,
            offer = "default",
            periodStart = System.currentTimeMillis() / 1000,
            periodEnd = (System.currentTimeMillis() + 30 * 24 * 60 * 60) / 1000,
            createTime = System.currentTimeMillis() / 1000,
            couponCode = null,
            discount = 0,
            renewDiscount = 0,
            renewAmount = 1299,
            renew = 1,
            external = 2,
            billingPlatform = 1,
            entitlements = emptyList(),
            decorations = emptyList(),
            isTrial = false,
            customerID = null
        )
    )

    // Add mock Google Play subscription status
    val mockGooglePlayStatus = Triple(
        true, // isActive
        false, // isAutoRenewing (auto-renewal disabled but subscription still active)
        System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000 // expiryTimeMillis (30 days from now)
    )

    LumoTheme {
        Surface(
            modifier = Modifier.padding(16.dp),
            color = Color.White
        ) {
            Column {
                previewSubscriptions.forEach { subscription ->
                    SubscriptionComponent(
                        subscription = subscription,
                        googlePlayRenewalStatus = if (subscription.external == 2) mockGooglePlayStatus else null,
                        googlePlayProductDetails = null, // No product details in preview
                        onManageSubscription = {}
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
} 