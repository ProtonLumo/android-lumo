package me.proton.android.lumo.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a feature from the Entitlements array, with the Text split into parts
 */
@Serializable
data class PlanFeature(
    @SerialName("name") val name: String,              // Feature name (first part of Text)
    @SerialName("freeText") val freeText: String,          // Free tier description (second part of Text)
    @SerialName("paidText") val paidText: String,          // Paid tier description (third part of Text)
    @SerialName("iconName") val iconName: String           // From IconName field
) 