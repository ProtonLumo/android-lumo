package me.proton.android.lumo.utils

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.proton.android.lumo.models.PlanFeature
import timber.log.Timber

object FeatureExtractor {

    private const val TAG = "FeatureExtractor"
    private const val EXPECTED_TEXT_PARTS = 3

    /**
     * Extracts plan features from the API response
     *
     * @param planObject The JSON object containing the plan data
     * @return List of extracted PlanFeature objects
     */
    fun extractPlanFeatures(planObject: JsonObject): List<PlanFeature> {
        val features = mutableListOf<PlanFeature>()

        val entitlementsArray = planObject["Entitlements"]?.jsonArray ?: return emptyList()

        for (entitlementElement in entitlementsArray) {
            runCatching {
                val entitlement = entitlementElement.jsonObject

                // Only process "description" type entitlements
                if (entitlement["Type"]?.jsonPrimitive?.contentOrNull == "description") {
                    val text =
                        entitlement["Text"]?.jsonPrimitive?.contentOrNull ?: return@runCatching
                    val textParts = text.split("::")

                    if (textParts.size >= EXPECTED_TEXT_PARTS) {
                        val feature = PlanFeature(
                            name = textParts[0],
                            freeText = textParts[1],
                            paidText = textParts[2],
                            iconName = entitlement["IconName"]?.jsonPrimitive?.contentOrNull
                                ?: "checkmark"
                        )
                        features.add(feature)
                        Timber.tag(TAG).i("Added feature: ${feature.name}")
                    }
                }
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "Error parsing entitlement")
            }
        }

        return features
    }
}
