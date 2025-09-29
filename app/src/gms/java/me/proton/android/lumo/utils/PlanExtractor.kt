package me.proton.android.lumo.utils

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import me.proton.android.lumo.R
import me.proton.android.lumo.models.JsPlanInfo

private const val TAG = "PlanExtractor"

/**
 * Helper for extracting plan information from the API response
 */
object PlanExtractor {

    /**
     * Extracts plans from the API response
     *
     * @param dataObject The JSON object containing the plans data
     * @param context Context for accessing string resources (optional)
     * @return List of extracted JsPlanInfo objects
     */
    fun extractPlans(dataObject: JsonObject, context: Context? = null): List<JsPlanInfo> {
        val extractedPlans = mutableListOf<JsPlanInfo>()

        if (dataObject.has("Plans") && dataObject.get("Plans").isJsonArray) {
            val plansArray = dataObject.getAsJsonArray("Plans")

            // Process each plan in the Plans array
            for (i in 0 until plansArray.size()) {
                val planObject = plansArray[i].asJsonObject
                val planTitle = planObject.get("Title")?.asString ?: "Lumo Plus" // Default fallback

                // Process Instances
                if (planObject.has("Instances") && planObject.get("Instances").isJsonArray) {
                    val instancesArray = planObject.getAsJsonArray("Instances")

                    for (j in 0 until instancesArray.size()) {
                        try {
                            val instance = instancesArray[j].asJsonObject
                            val cycle = instance.get("Cycle")?.asInt ?: 0
                            val description = instance.get("Description")?.asString ?: ""

                            // Get Google vendor info
                            val vendors = instance.get("Vendors")?.asJsonObject
                            val googleVendor = vendors?.get("Google")?.asJsonObject
                            val productId = googleVendor?.get("ProductID")?.asString
                            val customerId = googleVendor?.get("CustomerID")?.asString

                            // Only create plans for instances with valid Google productId
                            if (productId != null) {
                                val durationText = when (cycle) {
                                    1 -> context?.getString(R.string.plan_duration_1_month)
                                        ?: "1 month"

                                    12 -> context?.getString(R.string.plan_duration_12_months)
                                        ?: "12 months"

                                    else -> context?.getString(
                                        R.string.plan_duration_n_months,
                                        cycle
                                    ) ?: "$cycle Months"
                                }

                                val jsPlan = JsPlanInfo(
                                    id = "${planObject.get("ID")?.asString}-$cycle",
                                    name = planTitle,
                                    duration = durationText,
                                    cycle = cycle,
                                    description = description,
                                    productId = productId,
                                    customerId = customerId
                                )
                                extractedPlans.add(jsPlan)
                                Log.d(TAG, "Added plan: ${jsPlan.name} (${jsPlan.duration})")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing instance", e)
                        }
                    }
                }
            }
        }

        return extractedPlans
    }
} 