package me.proton.android.lumo.data.mapper

import android.util.Log
import me.proton.android.lumo.R
import me.proton.android.lumo.data.PlanResult
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.ui.components.UiText
import me.proton.android.lumo.utils.FeatureExtractor
import me.proton.android.lumo.utils.PlanExtractor

object PlanMapper {

    private const val TAG = "PlanMapper"

    fun parsePlans(jsResult: Result<PaymentJsResponse>): PlanResult {
        var planResult = PlanResult()
        try {
            jsResult.onSuccess { response ->
                val planFeatures = extractPlanFeatures(response)
                val extractedPlans = extractPlans(response)
                planResult = planResult.copy(
                    planFeatures = planFeatures,
                    planOptions = extractedPlans
                )
            }.onFailure { error ->
                Log.e(TAG, "Failed to load plans: ${error.message}", error)
                planResult = planResult.copy(
                    error = UiText.ResText(
                        R.string.error_failed_to_load_plans,
                        error.message ?: "Unknown error"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading plans", e)
            planResult = planResult.copy(
                error = UiText.ResText(
                    R.string.error_loading_plans,
                    e.message ?: "Unknown error"
                )
            )
        }

        return planResult
    }

    private fun extractPlanFeatures(response: PaymentJsResponse): List<PlanFeature> {
        if (response.data == null || !response.data.isJsonObject) {
            Log.e(TAG, "Cannot extract features: Data is null or not a JSON object")
            return emptyList()
        }

        val dataObject = response.data.asJsonObject

        if (dataObject.has("Plans") && dataObject.get("Plans").isJsonArray) {
            val plansArray = dataObject.getAsJsonArray("Plans")
            if (plansArray.size() > 0) {
                val firstPlanObject = plansArray[0].asJsonObject
                return FeatureExtractor.extractPlanFeatures(firstPlanObject)
            }
        }

        return emptyList()
    }

    private fun extractPlans(response: PaymentJsResponse): List<JsPlanInfo> {
        if (response.data == null || !response.data.isJsonObject) {
            Log.e(TAG, "Cannot extract plans: Data is null or not a JSON object")
            return emptyList()
        }

        return PlanExtractor.extractPlans(dataObject = response.data.asJsonObject)
    }
}