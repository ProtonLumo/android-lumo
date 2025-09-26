package me.proton.android.lumo.utils

import android.annotation.SuppressLint

/**
 * Utility class for formatting prices and storage sizes
 */
object PriceFormatter {

    /**
     * Format a price amount with the given currency
     *
     * @param amount The price amount in cents
     * @param currency The currency code (e.g., "USD", "EUR")
     * @return A formatted price string with currency symbol
     */
    @SuppressLint("DefaultLocale")
    fun formatPrice(amount: Int, currency: String): String {
        // Format cents to dollars/euros
        val mainAmount = amount / 100.0

        return when (currency) {
            "USD" -> "$${String.format("%.2f", mainAmount)}"
            "EUR" -> "€${String.format("%.2f", mainAmount)}"
            "GBP" -> "£${String.format("%.2f", mainAmount)}"
            else -> "${String.format("%.2f", mainAmount)} $currency"
        }
    }

    /**
     * Format a storage size in GB to a human-readable string
     *
     * @param sizeInGB The storage size in GB
     * @return A formatted string with appropriate units
     */
    @SuppressLint("DefaultLocale")
    fun formatStorageSize(sizeInGB: Float): String {
        return if (sizeInGB < 1) {
            // Convert to MB if less than 1 GB
            "${(sizeInGB * 1024).toInt()} MB"
        } else {
            // Display in GB with one decimal place
            "${String.format("%.1f", sizeInGB)} GB"
        }
    }
} 