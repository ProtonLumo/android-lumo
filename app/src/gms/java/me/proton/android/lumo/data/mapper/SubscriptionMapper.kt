package me.proton.android.lumo.data.mapper

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import me.proton.android.lumo.R
import me.proton.android.lumo.data.SubscriptionResult
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.models.SubscriptionItemResponse
import me.proton.android.lumo.models.SubscriptionsResponse
import me.proton.android.lumo.ui.text.UiText

object SubscriptionMapper {

    private const val TAG = "SubscriptionMapper"

    fun parseSubscriptions(jsResult: Result<PaymentJsResponse>): SubscriptionResult {
        var subscriptionResult = SubscriptionResult()
        try {
            jsResult.onSuccess { response ->
                // Parse subscriptions from response
                if (response.data != null && response.data is JsonObject) {
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
        if (response.data == null || response.data !is JsonObject) {
            Log.e(TAG, "Cannot parse subscriptions: Data is null or not a JSON object")
            return emptyList()
        }


        try {
            val json = Json {
                ignoreUnknownKeys = true
            }
            // Try parsing as SubscriptionsResponse (multiple subscriptions)
            val subscriptions = response.data["Subscriptions"]
            val subscription = response.data["Subscription"] // notice, SINGULAR
            if (subscriptions != null) {
                val subscriptionsResponse =
                    json.decodeFromJsonElement<SubscriptionsResponse>(response.data)
                Log.d(
                    TAG,
                    "Parsed multiple subscriptions: ${subscriptionsResponse.subscriptions.size}"
                )
                return subscriptionsResponse.subscriptions
            }
            // Try parsing as SubscriptionResponse (single subscription)
            else if (subscription != null) {
                val subscriptionResponse =
                    json.decodeFromJsonElement<SubscriptionsResponse>(response.data)

                Log.d(TAG, "Parsed single subscription response")
                return subscriptionResponse.subscriptions
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
            subscription.name != null &&
                    (subscription.name.contains("lumo", ignoreCase = true) ||
                            subscription.name.contains("visionary", ignoreCase = true))
        }
    }
}