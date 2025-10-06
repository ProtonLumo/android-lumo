package me.proton.android.lumo.billing

import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.models.PaymentTokenPayload
import me.proton.android.lumo.models.Subscription
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "BillingManagerWrapper"

/**
 * Wrapper class that handles all billing-related operations and Google Play Services integration.
 * Provides a clean interface for MainActivity while handling billing availability gracefully.
 */
class BillingManagerWrapper {

    private var billingManager: BillingManager? = null

    // Map to store callbacks for JS results
    private val pendingJsCallbacks =
        ConcurrentHashMap<String, (Result<PaymentJsResponse>) -> Unit>()

    /**
     * Enum defining the types of JavaScript functions to invoke in the WebView
     */
    enum class PAYMENT_REQUEST_TYPE(val functionName: String) {
        PAYMENT_TOKEN("postPaymentToken"),
        SUBSCRIPTION("postSubscription"),
        GET_PLANS("getPlans"),
        GET_SUBSCRIPTIONS("getSubscriptions")
    }

    /**
     * Initialize billing with comprehensive Google Services availability check
     */
    fun initializeBilling(activity: MainActivity) {
        try {
            Log.d(TAG, "=== BILLING INITIALIZATION CHECK ===")

            // 1. Check if Google Play Services is available
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val playServicesStatus = googleApiAvailability.isGooglePlayServicesAvailable(activity)

            when (playServicesStatus) {
                ConnectionResult.SUCCESS -> {
                    Log.d(TAG, "✅ Google Play Services is available")
                    initializeBillingManager(activity)
                }

                ConnectionResult.SERVICE_MISSING -> {
                    Log.w(TAG, "❌ Google Play Services is not installed")
                    handleBillingUnavailable(
                        activity,
                        "Google Play Services is not available on this device"
                    )
                }

                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                    Log.w(TAG, "❌ Google Play Services needs to be updated")
                    handleBillingUnavailable(activity, "Google Play Services needs to be updated")
                }

                ConnectionResult.SERVICE_DISABLED -> {
                    Log.w(TAG, "❌ Google Play Services is disabled")
                    handleBillingUnavailable(
                        activity,
                        "Google Play Services is disabled on this device"
                    )
                }

                else -> {
                    Log.w(TAG, "❌ Google Play Services unavailable: $playServicesStatus")
                    handleBillingUnavailable(
                        activity,
                        "Google Play Services is not available (code: $playServicesStatus)"
                    )
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error during billing initialization", e)
            handleBillingUnavailable(activity, "Billing services are not available: ${e.message}")
        }
    }

    private fun initializeBillingManager(activity: MainActivity) {
        // 2. Check if Google Play Store app is installed and accessible
        try {
            val pInfo = activity.packageManager.getPackageInfo("com.android.vending", 0)
            Log.d(TAG, "✅ Google Play Store version: ${pInfo.versionName}")

            // 3. Initialize BillingManager
            try {
                Log.d(TAG, "Initializing BillingManager...")
                val tempBillingManager = BillingManager(activity.application)
                // Check if BillingClient was created successfully
                if (tempBillingManager.isBillingAvailable()) {
                    billingManager = tempBillingManager
                    Log.d(TAG, "✅ BillingManager initialized successfully")
                } else {
                    Log.w(TAG, "⚠️ BillingClient creation failed - billing unavailable")
                    handleBillingUnavailable(
                        activity,
                        "Google Play Billing API is not available on this device"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ BillingManager initialization failed", e)
                val errorMessage = e.message ?: "Unknown error"
                val reason = when {
                    errorMessage.contains("API version is less than 3", ignoreCase = true) -> {
                        "Google Play Billing API version is too old. Please update Google Play Store."
                    }

                    errorMessage.contains("not supported", ignoreCase = true) -> {
                        "In-app purchases are not supported on this device."
                    }

                    else -> {
                        "Failed to initialize billing: $errorMessage"
                    }
                }
                handleBillingUnavailable(activity, reason)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Google Play Store not accessible", e)
            handleBillingUnavailable(activity, "Google Play Store is not accessible")
        }
    }

    /**
     * Handle the case where billing is unavailable
     */
    private fun handleBillingUnavailable(
        activity: MainActivity,
        reason: String,
    ) {
        Log.w(TAG, "=== BILLING UNAVAILABLE - ENTERING GRACEFUL DEGRADATION MODE ===")
        Log.w(TAG, "Reason: $reason")

        // Set states to indicate unavailability
        billingManager = null

        // Show user-friendly notification about billing unavailability
        val message = when {
            reason.contains("API version", ignoreCase = true) -> {
                "Google Play Store needs to be updated for in-app purchases. All other features will work normally."
            }

            reason.contains("not supported", ignoreCase = true) -> {
                "In-app purchases not supported on this device. All other features will work normally."
            }

            else -> {
                "In-app purchases are not available. All other features will work normally."
            }
        }
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()

        Log.i(TAG, "✅ App will continue normally with billing features disabled")
    }

    /**
     * Get the BillingManager instance if available
     */
    fun getBillingManager(): BillingManager? = billingManager
}
