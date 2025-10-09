package me.proton.android.lumo.billing

import android.app.Application
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

private const val TAG = "BillingManagerWrapper"

/**
 * Wrapper class that handles all billing-related operations and Google Play Services integration.
 * Provides a clean interface for MainActivity while handling billing availability gracefully.
 */
class BillingManagerWrapper(private val application: Application) {

    private var billingManager: BillingManager? = null

    /**
     * Enum defining the types of JavaScript functions to invoke in the WebView
     */
    enum class PaymentRequestType(val functionName: String) {
        PAYMENT_TOKEN("postPaymentToken"),
        SUBSCRIPTION("postSubscription"),
        GET_PLANS("getPlans"),
        GET_SUBSCRIPTIONS("getSubscriptions")
    }

    init {
        try {
            Log.d(TAG, "=== BILLING INITIALIZATION CHECK ===")

            // 1. Check if Google Play Services is available
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val playServicesStatus = googleApiAvailability
                .isGooglePlayServicesAvailable(application)

            when (playServicesStatus) {
                ConnectionResult.SUCCESS -> {
                    Log.d(TAG, "✅ Google Play Services is available")
                    initializeBillingManager()
                }

                ConnectionResult.SERVICE_MISSING -> {
                    Log.w(TAG, "❌ Google Play Services is not installed")
                    handleBillingUnavailable(
                        "Google Play Services is not available on this device"
                    )
                }

                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                    Log.w(TAG, "❌ Google Play Services needs to be updated")
                    handleBillingUnavailable("Google Play Services needs to be updated")
                }

                ConnectionResult.SERVICE_DISABLED -> {
                    Log.w(TAG, "❌ Google Play Services is disabled")
                    handleBillingUnavailable(
                        "Google Play Services is disabled on this device"
                    )
                }

                else -> {
                    Log.w(TAG, "❌ Google Play Services unavailable: $playServicesStatus")
                    handleBillingUnavailable(
                        "Google Play Services is not available (code: $playServicesStatus)"
                    )
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error during billing initialization", e)
            handleBillingUnavailable("Billing services are not available: ${e.message}")
        }
    }

    private fun initializeBillingManager() {
        // 2. Check if Google Play Store app is installed and accessible
        try {
            val pInfo = application.packageManager.getPackageInfo("com.android.vending", 0)
            Log.d(TAG, "✅ Google Play Store version: ${pInfo.versionName}")

            // 3. Initialize BillingManager
            try {
                Log.d(TAG, "Initializing BillingManager...")
                val tempBillingManager = BillingManager(application)
                // Check if BillingClient was created successfully
                if (tempBillingManager.isBillingAvailable()) {
                    billingManager = tempBillingManager
                    Log.d(TAG, "✅ BillingManager initialized successfully")
                } else {
                    Log.w(TAG, "⚠️ BillingClient creation failed - billing unavailable")
                    handleBillingUnavailable(
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
                handleBillingUnavailable(reason)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Google Play Store not accessible", e)
            handleBillingUnavailable("Google Play Store is not accessible")
        }
    }

    /**
     * Handle the case where billing is unavailable
     */
    private fun handleBillingUnavailable(reason: String) {
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
        Toast.makeText(application, message, Toast.LENGTH_LONG).show()

        Log.i(TAG, "✅ App will continue normally with billing features disabled")
    }

    /**
     * Get the BillingManager instance if available
     */
    fun getBillingManager(): BillingManager? = billingManager
}
