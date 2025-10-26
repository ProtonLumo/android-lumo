package me.proton.android.lumo.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import java.util.*

@Composable
fun SubscriptionComponent(
    subscription: SubscriptionItemResponse,
    googlePlayRenewalStatus: Triple<Boolean, Boolean, Long>? = null,
    googlePlayProductDetails: List<ProductDetails>? = null,
    onManageSubscription: (Context) -> Unit
) {
    // Check if this is a mobile plan (External == 2 indicates a Google Play Store subscription)
    val isMobilePlan = subscription.External == 2

    // Log to verify the values
    if (isMobilePlan) {
        Log.d(
            "SubscriptionComponent",
            "Mobile plan detected: ${subscription.Title}, External=${subscription.External}"
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
        val cancelled = subscription.Renew == 0
        Log.d(
            "SubscriptionComponent",
            "Web plan cancellation check: isCancelled=$cancelled (Renew=${subscription.Renew})"
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
        val expectedPeriod = when (subscription.Cycle) {
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
            val isCycleMatch = when (subscription.Cycle) {
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
                    else -> if (subscription.Cycle == 1) "month" else "year"
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
            "No matching Google Play product found for subscription cycle ${subscription.Cycle}"
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
            .background(Color.White)
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
                        val planTitle = subscription.Title ?: subscription.Name

                        planTitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
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
                            dateFormat.format(Date(subscription.PeriodEnd * 1000))
                        }

                        Pair(date, isAutoRenewing)
                    } else {
                        // Use API subscription info for web plans
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val date = if (subscription.PeriodEnd > 0) {
                            dateFormat.format(Date(subscription.PeriodEnd * 1000))
                        } else "Unknown"

                        Pair(date, subscription.Renew == 1)
                    }

                    val message = if (isRenewing) {
                        stringResource(id = R.string.subscription_renews, renewalDate)
                    } else {
                        stringResource(id = R.string.subscription_expires, renewalDate)
                    }

                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Show cycle description if available
                    subscription.CycleDescription?.let {
                        Text(
                            text = it,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                subscription.Amount,
                                subscription.Currency
                            )
                            val period = if (subscription.Cycle == 1) "month" else "year"
                            Pair(formattedPrice, period)
                        }
                    } else {
                        // For web plans, always use API pricing
                        val formattedPrice =
                            PriceFormatter.formatPrice(subscription.Amount, subscription.Currency)
                        val period = if (subscription.Cycle == 1) "month" else "year"
                        Pair(formattedPrice, period)
                    }

                    Text(
                        text = priceText,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "a $periodText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Show entitlements if available
            subscription.Entitlements?.let { entitlements ->
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
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = entitlement.text,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        containerColor = MaterialTheme.colorScheme.primary
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun StorageUsageIndicator(
    usedStorage: Float,
    totalStorage: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "${PriceFormatter.formatStorageSize(usedStorage)} of ${
                PriceFormatter.formatStorageSize(
                    totalStorage
                )
            }",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Progress bar - fixed the height issue
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(LumoTheme.colors.interactionNorm)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(usedStorage / totalStorage)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LumoTheme.colors.signalWarning)
            )
        }
    }
}

@Composable
fun FeatureItem(
    text: String,
    iconName: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Choose icon based on name - this is a simple implementation
        // You might want to extend this with more icon mappings
        val icon = when (iconName.lowercase()) {
            "shield" -> Icons.Default.Check
            "chat" -> Icons.Default.Check
            "star" -> Icons.Default.Check
            else -> Icons.Default.Check
        }

        // Display icon with a light purple background circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(LumoTheme.colors.interactionNorm, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SubscriptionComponentPreview() {
    // Preview both types of subscription structures based on the real API format
    val previewSubscriptions = listOf(
        // Web subscription (Mail Plus)
        SubscriptionItemResponse(
            ID = "aaa==",
            InvoiceID = "-aaa-7gm5YLf215MEgZCdzOtLW5psxgB8oNc8OnoFRykab4Z23EGEW1ka3GtQPF9xwx9-VUA==",
            Title = "Mail Plus",
            Description = "Current plan",
            Name = "mail2022",
            Cycle = 12,
            CycleDescription = "For 1 year",
            Currency = "CHF",
            Amount = 4788,
            Offer = "default",
            PeriodStart = System.currentTimeMillis() / 1000,
            PeriodEnd = (System.currentTimeMillis() + 365 * 24 * 60 * 60) / 1000,
            CreateTime = System.currentTimeMillis() / 1000,
            CouponCode = null,
            Discount = 0,
            RenewDiscount = 0,
            RenewAmount = 4788,
            Renew = 0,
            External = 0,
            BillingPlatform = 1,
            Entitlements = listOf(
                SubscriptionEntitlement(
                    type = "description",
                    iconName = "checkmark",
                    text = "And the free features of all other Proton products!"
                )
            ),
            Decorations = emptyList(),
            IsTrial = false,
            CustomerID = null
        ),
        // Mobile subscription (Lumo Plus)
        SubscriptionItemResponse(
            ID = "nNTtf0H8g-aaa==",
            InvoiceID = "aaa-ZTD8H8F6LvNaSjMaPxB5ecFkA7y-5kc3q38cGumJENGHjtSoUndkYFUx0_xlJeg==",
            Title = "Lumo Plus",
            Description = "Current plan",
            Name = "lumo2024",
            Cycle = 1,
            CycleDescription = "For 1 month",
            Currency = "CHF",
            Amount = 1299,
            Offer = "default",
            PeriodStart = System.currentTimeMillis() / 1000,
            PeriodEnd = (System.currentTimeMillis() + 30 * 24 * 60 * 60) / 1000,
            CreateTime = System.currentTimeMillis() / 1000,
            CouponCode = null,
            Discount = 0,
            RenewDiscount = 0,
            RenewAmount = 1299,
            Renew = 1,
            External = 2,
            BillingPlatform = 1,
            Entitlements = emptyList(),
            Decorations = emptyList(),
            IsTrial = false,
            CustomerID = null
        )
    )

    // Add mock Google Play subscription status
    val mockGooglePlayStatus = Triple(
        true, // isActive
        false, // isAutoRenewing (auto-renewal disabled but subscription still active)
        System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000 // expiryTimeMillis (30 days from now)
    )

    MaterialTheme {
        Surface(
            modifier = Modifier.padding(16.dp),
            color = Color.White
        ) {
            Column {
                previewSubscriptions.forEach { subscription ->
                    SubscriptionComponent(
                        subscription = subscription,
                        googlePlayRenewalStatus = if (subscription.External == 2) mockGooglePlayStatus else null,
                        googlePlayProductDetails = null, // No product details in preview
                        onManageSubscription = {}
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
} 