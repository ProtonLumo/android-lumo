package me.proton.android.lumo.data.mapper

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.update
import me.proton.android.lumo.R
import me.proton.android.lumo.data.SubscriptionResult
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.models.SubscriptionItemResponse
import me.proton.android.lumo.models.SubscriptionsResponse
import me.proton.android.lumo.ui.components.UiText

object SubscriptionMapper {

    private const val TAG = "SubscriptionMapper"

    fun parseSubscriptions(jsResult: Result<PaymentJsResponse>): SubscriptionResult {
        var subscriptionResult = SubscriptionResult()
        try {
            jsResult.onSuccess { response ->
                // Parse subscriptions from response
                if (response.data != null && response.data.isJsonObject) {
                    val parsedSubscriptions = parseSubscriptions(response)
                    val hasValidSubscription = hasValidSubscription(parsedSubscriptions)
                    subscriptionResult = subscriptionResult.copy(
                        subscriptions = parsedSubscriptions,
                        hasValidSubscription = hasValidSubscription
                    )

                    Log.d(
                        TAG,
                        "Loaded ${parsedSubscriptions.size} subscriptions, " +
                                "hasValid=${hasValidSubscription}"
                    )
                } else {
                    Log.e(TAG, "Invalid subscription data format")
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load subscriptions: ${error.message}", error)
                subscriptionResult.copy(
                    error = UiText.ResText(R.string.error_failed_to_load_subscriptions)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading subscriptions", e)
            subscriptionResult.copy(
                error = UiText.ResText(R.string.error_loading_subscriptions)
            )
        }

        return subscriptionResult
    }

    /**
     * Parse subscriptions from API response
     */
    private fun parseSubscriptions(response: PaymentJsResponse): List<SubscriptionItemResponse> {
        if (response.data == null || !response.data.isJsonObject) {
            Log.e(TAG, "Cannot parse subscriptions: Data is null or not a JSON object")
            return emptyList()
        }

        val gson = Gson()
        val dataObject = response.data.asJsonObject

        try {
            // Try parsing as SubscriptionsResponse (multiple subscriptions)
            if (dataObject.has("Subscriptions")) {
                val subscriptionsResponse = gson.fromJson(
                    response.data,
                    SubscriptionsResponse::class.java
                )

                Log.d(
                    TAG,
                    "Parsed multiple subscriptions: ${subscriptionsResponse.Subscriptions.size}"
                )
                return subscriptionsResponse.Subscriptions
            }
            // Try parsing as SubscriptionResponse (single subscription)
            else if (dataObject.has("Subscription")) {
                val subscriptionResponse = gson.fromJson(
                    response.data,
                    SubscriptionsResponse::class.java
                )

                Log.d(TAG, "Parsed single subscription response")
                return subscriptionResponse.Subscriptions
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing subscriptions: ${e.message}", e)
        }

        return emptyList()
    }

    private fun hasValidSubscription(subscriptions: List<SubscriptionItemResponse>): Boolean {
        Log.e(TAG, "$subscriptions")
        return subscriptions.any { subscription ->
            // Check for Lumo or Visionary plans
            subscription.Name != null &&
                    (subscription.Name.contains("lumo", ignoreCase = true) ||
                            subscription.Name.contains("visionary", ignoreCase = true))
        }
    }
}