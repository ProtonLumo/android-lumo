package me.proton.android.lumo.utils

import android.annotation.SuppressLint
import android.util.Log
import com.android.billingclient.api.ProductDetails
import me.proton.android.lumo.models.JsPlanInfo
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private const val TAG = "PlanPricingHelper"

/**
 * Helper class for updating plan pricing information by matching with Google Play ProductDetails
 */
object PlanPricingHelper {

    /**
     * Updates plan pricing information using Google Play product details
     *
     * @param plans List of plans to update with pricing information
     * @param googleProducts List of Google Play product details
     * @return Updated list of plans with pricing information
     */
    @SuppressLint("DefaultLocale")
    fun updatePlanPricing(
        plans: List<JsPlanInfo>,
        googleProducts: List<ProductDetails>,
        offerId: String?,
    ): List<JsPlanInfo> {
        if (googleProducts.isEmpty()) return plans

        // Step 1: Build lookup maps
        val productMap = googleProducts.associateBy { it.productId }

        val offerMap = googleProducts.flatMap { product ->
            (product.subscriptionOfferDetails ?: emptyList())
                .map { offer ->
                    val key = "${product.productId}:${offer.offerId ?: "BASE"}"
                    key to offer
                }
        }.toMap()

        // Step 2: Create updated plans
        return plans.map { plan ->
            val product = productMap[plan.productId]
            if (product == null || product.subscriptionOfferDetails.isNullOrEmpty()) {
                Log.w(TAG, "No matching Google product found for ${plan.productId}")
                return@map plan
            }

            // Step 3: Pick offer by explicit ID or fallback
            val bestOffer = run {
                offerId?.let { offerMap["${plan.productId}:$it"] }
                    ?: offerMap["${plan.productId}:BASE"] // Then try base
                    ?: product.subscriptionOfferDetails?.first() // Fallback to first available
            }

            val pricingPhase = bestOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()
            if (pricingPhase == null) return@map plan

            val updated = plan.copy(
                totalPrice = pricingPhase.formattedPrice,
                offerToken = bestOffer.offerToken,
                pricePerMonth = computeMonthlyPrice(plan, pricingPhase),
                savings = computeSavings(plan, plans, productMap, pricingPhase)
            )

            Log.d(
                TAG,
                "Updated ${plan.name}: total=${updated.totalPrice}, " +
                        "monthly=${updated.pricePerMonth}, savings=${updated.savings}"
            )

            updated
        }
    }

    private fun computeMonthlyPrice(
        plan: JsPlanInfo,
        pricingPhase: ProductDetails.PricingPhase
    ): String {
        return if (plan.cycle > 1 && pricingPhase.priceAmountMicros > 0) {
            val monthlyPrice = pricingPhase.priceAmountMicros / (plan.cycle * 1_000_000.0)
            formatPriceWithCurrency(monthlyPrice, pricingPhase.priceCurrencyCode)
        } else {
            pricingPhase.formattedPrice
        }
    }

    private fun computeSavings(
        plan: JsPlanInfo,
        plans: List<JsPlanInfo>,
        productMap: Map<String, ProductDetails>,
        yearlyPhase: ProductDetails.PricingPhase
    ): String? {
        if (plan.cycle != 12 || yearlyPhase.priceAmountMicros <= 0) return null

        val monthlyPlan =
            plans.find { it.productId.contains("_1_") && it.cycle == 1 } ?: return null
        val monthlyProduct = productMap[monthlyPlan.productId] ?: return null
        val monthlyPhase = monthlyProduct.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull() ?: return null

        if (monthlyPhase.priceAmountMicros <= 0) return null

        val annualMonthlyTotal = (monthlyPhase.priceAmountMicros * 12) / 1_000_000.0
        val annualCost = yearlyPhase.priceAmountMicros / 1_000_000.0
        val savingsPercent = ((annualMonthlyTotal - annualCost) / annualMonthlyTotal * 100).toInt()

        return if (savingsPercent > 0) "$savingsPercent%" else null
    }

    /**
     * Format a price amount with the correct currency symbol
     * @param amount The price amount as a double
     * @param currencyCode The currency code (e.g., "USD", "GBP", "EUR")
     * @return Formatted price string with correct currency symbol
     */
    private fun formatPriceWithCurrency(amount: Double, currencyCode: String): String {
        return try {
            val locale = when (currencyCode) {
                "GBP" -> Locale.UK
                "EUR" -> Locale.GERMANY
                "USD" -> Locale.US
                else -> Locale.US // Default to US for unknown currencies
            }
            val formatter = NumberFormat.getCurrencyInstance(locale)
            formatter.currency = Currency.getInstance(currencyCode)
            formatter.format(amount)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to format currency $currencyCode, falling back to simple format", e)
            // Fallback to simple format if currency formatting fails
            String.format("%.2f %s", amount, currencyCode)
        }
    }
} 