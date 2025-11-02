package me.proton.android.lumo.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents the structured response expected from JavaScript payment/subscription calls.
 */
@Serializable
data class PaymentJsResponse(
    @SerialName("status") val status: String, // e.g., "success", "error"
    @SerialName("data") val data: JsonElement? = null, // The actual data on success (can be any JSON structure)
    @SerialName("message") val message: String? = null // Error message on failure
) 