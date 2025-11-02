import android.util.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.proton.android.lumo.R
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.ui.text.UiText

object PlanExtractor {

    private const val TAG = "PlanExtractor"

    fun extractPlans(dataObject: JsonObject): List<JsPlanInfo> {
        val extractedPlans = mutableListOf<JsPlanInfo>()

        val plansArray = dataObject["Plans"]?.jsonArray ?: return emptyList()

        for (planElement in plansArray) {
            val planObject = planElement.jsonObject
            val planTitle = planObject["Title"]?.jsonPrimitive?.contentOrNull ?: "Lumo Plus"
            val planId = planObject["ID"]?.jsonPrimitive?.contentOrNull ?: continue

            val instancesArray = planObject["Instances"]?.jsonArray ?: continue

            for (instanceElement in instancesArray) {
                runCatching {
                    val instance = instanceElement.jsonObject

                    val cycle = instance["Cycle"]?.jsonPrimitive?.intOrNull ?: 0
                    val description =
                        instance["Description"]?.jsonPrimitive?.contentOrNull.orEmpty()

                    // Vendors → Google → ProductID, CustomerID
                    val googleVendor = instance["Vendors"]
                        ?.jsonObject
                        ?.get("Google")
                        ?.jsonObject

                    val productId = googleVendor?.get("ProductID")?.jsonPrimitive?.contentOrNull
                    val customerId = googleVendor?.get("CustomerID")?.jsonPrimitive?.contentOrNull

                    if (!productId.isNullOrBlank()) {
                        val durationText = when (cycle) {
                            1 -> UiText.ResText(R.string.plan_duration_1_month)
                            12 -> UiText.ResText(R.string.plan_duration_12_months)
                            else -> UiText.ResText(R.string.plan_duration_n_months, cycle)
                        }

                        val jsPlan = JsPlanInfo(
                            id = "$planId-$cycle",
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
                }.onFailure { e ->
                    Log.e(TAG, "Error parsing instance", e)
                }
            }
        }

        return extractedPlans
    }
}
