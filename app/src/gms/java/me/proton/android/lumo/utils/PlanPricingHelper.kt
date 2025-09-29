package me.proton.android.lumo.utils

import android.annotation.SuppressLint
import android.util.Log
import com.android.billingclient.api.ProductDetails
import me.proton.android.lumo.models.JsPlanInfo
import java.text.NumberFormat
import java.util.*

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
        googleProducts: List<ProductDetails>
    ): List<JsPlanInfo> {
        // Create a copy to avoid modifying the original list
        val updatedPlans = plans.toMutableList()

        updatedPlans.forEach { plan ->
            // Find matching Google product by productId
            val matchingProduct = googleProducts.find { it.productId == plan.productId }

            if (matchingProduct != null && matchingProduct.subscriptionOfferDetails != null) {
                // Try to find an offer matching the cycle/duration
                val cycleMapping = when (plan.cycle) {
                    1 -> "P1M" // Monthly billing period
                    12 -> "P1Y" // Yearly billing period
                    else -> null
                }

                // Find the best matching offer
                val matchingOffer = if (cycleMapping != null) {
                    matchingProduct.subscriptionOfferDetails!!.find { offer ->
                        offer.pricingPhases.pricingPhaseList.any { phase ->
                            phase.billingPeriod == cycleMapping
                        }
                    }
                } else null

                // Use the first offer if no matching one found
                val bestOffer =
                    matchingOffer ?: matchingProduct.subscriptionOfferDetails!!.firstOrNull()

                if (bestOffer != null) {
                    val pricingPhase = bestOffer.pricingPhases.pricingPhaseList.firstOrNull()

                    if (pricingPhase != null) {
                        // Set total price
                        plan.totalPrice = pricingPhase.formattedPrice

                        // Calculate price per month for yearly plans
                        if (plan.cycle > 1 && pricingPhase.priceAmountMicros > 0) {
                            val monthlyPrice =
                                pricingPhase.priceAmountMicros / (plan.cycle * 1_000_000.0)
                            // Use the same currency as the total price to ensure consistency
                            val currencyCode = pricingPhase.priceCurrencyCode
                            plan.pricePerMonth = formatPriceWithCurrency(monthlyPrice, currencyCode)

                            // Calculate savings compared to monthly plan if we have both plans
                            if (plan.cycle == 12) {
                                // Try to find the monthly plan
                                val monthlyPlan =
                                    plans.find { it.productId.contains("_1_") && it.cycle == 1 }
                                if (monthlyPlan != null) {
                                    val monthlyProduct =
                                        googleProducts.find { it.productId == monthlyPlan.productId }
                                    if (monthlyProduct != null && monthlyProduct.subscriptionOfferDetails != null) {
                                        val monthlyOffer =
                                            monthlyProduct.subscriptionOfferDetails!!.firstOrNull()
                                        val monthlyPhase =
                                            monthlyOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()

                                        if (monthlyPhase != null && monthlyPhase.priceAmountMicros > 0) {
                                            // Calculate annual cost if paying monthly
                                            val annualMonthlyTotal =
                                                (monthlyPhase.priceAmountMicros * 12) / 1_000_000.0
                                            val annualCost =
                                                pricingPhase.priceAmountMicros / 1_000_000.0

                                            if (annualCost < annualMonthlyTotal) {
                                                val savingsPercent =
                                                    ((annualMonthlyTotal - annualCost) / annualMonthlyTotal * 100).toInt()
                                                if (savingsPercent > 0) {
                                                    plan.savings = "Save ${savingsPercent}%"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            plan.pricePerMonth = pricingPhase.formattedPrice
                        }

                        // Set offerToken
                        plan.offerToken = bestOffer.offerToken

                        Log.d(
                            TAG, "Updated plan ${plan.name} (${plan.duration}): " +
                                    "customerID=${plan.customerId} " +
                                    "price=${plan.totalPrice}, monthly=${plan.pricePerMonth}, " +
                                    "savings=${plan.savings}, offerToken=${plan.offerToken}"
                        )
                    }
                }
            } else {
                Log.w(TAG, "No matching Google product found for: ${plan.productId}")
                Log.w(
                    TAG,
                    "  - Available Google product IDs: ${googleProducts.map { it.productId }}"
                )
            }
        }

        return updatedPlans
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