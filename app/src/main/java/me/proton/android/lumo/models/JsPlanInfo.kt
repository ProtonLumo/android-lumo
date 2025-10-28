package me.proton.android.lumo.models

import me.proton.android.lumo.ui.text.UiText

/**
 * Represents a plan object parsed from the API response Instances
 */
data class JsPlanInfo(
    val id: String,                // Plan ID from the instance
    val name: String,              // From parent Plan's Title (e.g., "Lumo Plus")
    val duration: UiText,          // Readable duration text (e.g., "Monthly", "Annual")
    val cycle: Int,                // Number of months (1, 12)
    val description: String,       // Description from Instance
    val productId: String,         // From Vendors.Google.ProductID
    val customerId: String?,       // From Vendors.Google.CustomerID (optional)

    // Placeholders to be filled from Google Play
    var pricePerMonth: String = "",    // To be filled from Google Play data
    var totalPrice: String = "",       // To be filled from Google Play data
    var previousTotalPrice: String = "",
    var savings: String? = null,       // e.g., "Save 20%" - calculated if applicable
    var offerToken: String? = null     // To be populated from Google Play
) 