package me.proton.android.lumo.billing

import android.content.Context
import android.content.pm.PackageManager
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import me.proton.android.lumo.R
import me.proton.android.lumo.models.InAppGooglePayload
import me.proton.android.lumo.models.Payment
import me.proton.android.lumo.models.PaymentTokenPayload
import me.proton.android.lumo.models.SubscriptionPlan
import me.proton.android.lumo.ui.components.PaymentProcessingState
import me.proton.android.lumo.ui.text.UiText
import timber.log.Timber
import java.util.Date

class BillingManager(private val context: Context) {

    companion object {
        private const val TAG = "BillingManager"

        // Production subscription product IDs (must match Google Play Console exactly)
        internal val SUBSCRIPTION_PLANS = listOf(
            SubscriptionPlan(
                productId = "giaplumo_lumo2025_1_renewing",
                planName = "1 Month",
                durationMonths = 1,
                description = "Monthly subscription" // Note: This is a constant, localized descriptions are handled in UI
            ),
            SubscriptionPlan(
                productId = "giaplumo_lumo2025_12_renewing",
                planName = "12 Months",
                durationMonths = 12,
                description = "Annual subscription (save 20%)" // Note: This is a constant, localized descriptions are handled in UI
            )
        )

        // Cache invalidation constants
        private const val CACHE_INVALIDATION_INTERVAL_MS = 60_000L // 1 min
        private const val PURCHASE_REFRESH_INTERVAL_MS = 30_000L // 30 sec for active subscriptions
    }

    private val _purchaseChannel = Channel<PaymentTokenPayload>()
    val purchaseChannel = _purchaseChannel.receiveAsFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.NotPurchased)
    private val _productDetailsList = MutableStateFlow<List<ProductDetails>>(emptyList())
    private val _isConnected = MutableStateFlow(false)
    private val _isSubscriptionRenewing = MutableStateFlow(false)
    protected var subscriptionExpiryTimeMillis: Long = 0L

    // Customer ID from selected plan
    protected var currentCustomerID: String? = null

    // New state for payment processing
    // TODO: make me private
    internal val _paymentProcessingState = MutableStateFlow<PaymentProcessingState?>(null)
    val paymentProcessingState = _paymentProcessingState.asStateFlow()

    // State to track if purchase refresh is in progress
    private val _isRefreshingPurchases = MutableStateFlow(false)
    val isRefreshingPurchases = _isRefreshingPurchases.asStateFlow()

    // Cache management
    private var lastPurchaseQueryTime = 0L
    private var lastProductDetailsQueryTime = 0L
    private var periodicRefreshJob: Job? = null
    private var cachedPurchases: List<Purchase> = emptyList()

    val productDetailsList = _productDetailsList.asStateFlow()

    private val _selectedPlanIndex = MutableStateFlow(0)

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        Timber.tag(TAG).d(
            "onPurchasesUpdated: ${billingResult.responseCode}, ${billingResult.debugMessage}"
        )
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _purchaseState.value = PurchaseState.Cancelled
        } else {
            _purchaseState.value = PurchaseState.Error(
                UiText.StringText(billingResult.debugMessage)
            )
        }
    }

    private val billingClient by lazy {
        try {
            BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build()
                )
                .enableAutoServiceReconnection()
                .build()
        } catch (e: Exception) {
            Timber
                .tag(TAG)
                .e(e, "Failed to create BillingClient - likely due to old Google Play Billing API")
            // Return null so the app can continue without billing
            null
        }
    }

    private var isConnected = false

    /**
     * Check if billing is available (i.e., BillingClient was created successfully)
     */
    fun isBillingAvailable(): Boolean {
        return billingClient != null
    }

    init {
        Timber.tag(TAG).i("Initializing BillingManager in PRODUCTION mode")
        try {
            // Check if Google Play is installed before proceeding
            val pInfo = context.packageManager.getPackageInfo("com.android.vending", 0)
            Timber.tag(TAG).i("Google Play Store is installed -version: ${pInfo.versionName}")

            // Initialize billing with additional safety
            initializeBilling()

        } catch (e: PackageManager.NameNotFoundException) {
            Timber.tag(TAG).e(e, "Google Play Store is not installed")
            _purchaseState.value = PurchaseState.Error(
                UiText.ResText(R.string.billing_google_play_required)
            )
        } catch (e: SecurityException) {
            Timber.tag(TAG).e(e, "Permission denied accessing Google Play Store")
            _purchaseState.value = PurchaseState.Error(
                UiText.ResText(R.string.billing_cannot_access_google_play)
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Critical error during billing initialization")
            _purchaseState.value = PurchaseState.Error(
                UiText.ResText(
                    R.string.billing_services_unavailable,
                    e.message ?: "Unknown error"
                )
            )
            // Don't let billing errors crash the app - just disable billing
            Timber.tag(TAG)
                .w("Billing initialization failed - app will continue without billing features")
        }
    }

    private fun initializeBilling() {
        Timber.tag(TAG).d("Initializing billing client")

        try {
            when {
                billingClient == null -> {
                    Timber.tag(TAG)
                        .w("Billing client is null - billing unavailable (likely old Google Play API)")
                    _purchaseState.value = PurchaseState.Error(
                        UiText.ResText(R.string.billing_api_not_available)
                    )
                    // Don't proceed with connection - app should continue without billing
                    return
                }

                else -> {
                    establishConnection()
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Exception during billing client initialization")
            _purchaseState.value = PurchaseState.Error(
                UiText.ResText(
                    R.string.billing_initialization_failed,
                    e.message ?: "Unknown error"
                )
            )
            // Don't let this crash the app
        }
    }

    private fun establishConnection() {
        Timber.tag(TAG).d("Starting billing client connection")
        if (billingClient == null) {
            Timber.tag(TAG).e("Billing client is null")
            return
        }

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.tag(TAG).d("Billing client connected successfully")
                    _isConnected.value = true

                    // Check billing features support
                    val subscriptionsSupported =
                        billingClient?.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
                    Timber.tag(TAG).d(
                        "Subscriptions supported: " +
                                "${
                                    subscriptionsSupported?.responseCode ==
                                            BillingClient.BillingResponseCode.OK
                                }"
                    )

                    val productDetailsSupported =
                        billingClient?.isFeatureSupported(BillingClient.FeatureType.PRODUCT_DETAILS)
                    Timber.tag(TAG).d(
                        "Product details supported: ${productDetailsSupported?.responseCode == BillingClient.BillingResponseCode.OK}"
                    )

                    // Query both product details and existing purchases
                    querySubscriptions()
                    queryExistingPurchases()

                    // Start periodic refresh for subscription monitoring
                    startPeriodicRefresh()
                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                    // Specific handling for BILLING_UNAVAILABLE (error code 3)
                    val debugMessage = billingResult.debugMessage
                    Timber.tag(TAG).e("Billing unavailable: $debugMessage")

                    _isConnected.value = false

                    // Provide specific error messages based on the debug message
                    val userMessage: UiText = when {
                        debugMessage.contains("API version is less than 3", ignoreCase = true) -> {
                            UiText.ResText(R.string.billing_unavailable_old_api)
                        }

                        debugMessage.contains("not supported", ignoreCase = true) -> {
                            UiText.ResText(R.string.billing_unavailable_not_supported)
                        }

                        else -> {
                            UiText.ResText(R.string.billing_unavailable_generic)
                        }
                    }
                    _purchaseState.value = PurchaseState.Error(userMessage)

                    // Check if Google Play Store is installed and updated
                    try {
                        val pInfo =
                            context.packageManager.getPackageInfo("com.android.vending", 0)
                        Timber.tag(TAG).d("Google Play Store version: ${pInfo.versionName}")
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Google Play Store not found")
                    }
                } else {
                    Timber.tag(TAG)
                        .e("Billing client connection failed: ${billingResult.responseCode} ${billingResult.debugMessage}")
                    _isConnected.value = false
                    _purchaseState.value =
                        PurchaseState.Error(
                            UiText.ResText(
                                R.string.billing_connection_failed,
                                billingResult.debugMessage
                            )
                        )
                }
            }

            override fun onBillingServiceDisconnected() {
                Timber.tag(TAG).w("Billing service disconnected")
                _isConnected.value = false
                // Retry connection after a delay
                coroutineScope.launch {
                    delay(1000)
                    establishConnection()
                }
            }
        })
    }

    private fun queryExistingPurchases(
        onComplete: ((List<Purchase>) -> Unit)? = null,
        forceRefresh: Boolean = false
    ) {
        if (!_isConnected.value) {
            Timber.tag(TAG).e("Cannot query purchases - billing client not connected")
            // If we're in a payment processing state, show error
            if (_paymentProcessingState.value != null) {
                _paymentProcessingState.value = PaymentProcessingState.Error(
                    UiText.ResText(R.string.billing_service_not_connected)
                )
            }
            onComplete?.invoke(emptyList())
            return
        }

        val currentTime = System.currentTimeMillis()
        val cacheAge = currentTime - lastPurchaseQueryTime

        // Check if we should skip query due to recent cache
        if (!forceRefresh && cacheAge < CACHE_INVALIDATION_INTERVAL_MS && lastPurchaseQueryTime > 0) {
            Timber.tag(TAG).d(
                "Using cached purchase data (age: ${cacheAge}ms, ${cachedPurchases.size} purchases)"
            )
            onComplete?.invoke(cachedPurchases) // Return cached purchases
            return
        }

        Timber.tag(TAG)
            .d("Querying existing purchases (force: $forceRefresh, cache age: ${cacheAge}ms)")
        coroutineScope.launch {
            try {
                // For test mode, we use INAPP for standard test products
                val params = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()

                billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
                    Timber.tag(TAG).d(
                        "Existing purchases query result: ${billingResult.responseCode}, purchases size: ${purchases.size}"
                    )

                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // Update cache timestamp and store purchases on successful query
                        lastPurchaseQueryTime = currentTime
                        cachedPurchases = purchases

                        if (purchases.isNotEmpty()) {
                            // Reset subscription status before processing new data
                            _isSubscriptionRenewing.value = false
                            subscriptionExpiryTimeMillis = 0

                            for (purchase in purchases) {
                                Timber.tag(TAG).d("Found purchase: ${purchase.products}")

                                // Check if the subscription is set to renew
                                val isAutoRenewing = purchase.isAutoRenewing
                                _isSubscriptionRenewing.value = isAutoRenewing
                                Timber.tag(TAG).d("Subscription auto-renewing: $isAutoRenewing")

                                // Extract expiry time from subscription
                                try {
                                    val json = Json {
                                        ignoreUnknownKeys = true
                                    }
                                    // Get expiry date from purchase token (if available)
                                    val subscriptionInfo = json.decodeFromString<JsonObject>(
                                        purchase.originalJson
                                    )
                                    Timber.tag(TAG).d("Subscription info: $subscriptionInfo")
                                    val expiryTimeMillis: Long =
                                        subscriptionInfo["expiryTimeMillis"]?.jsonPrimitive?.long
                                            ?: 0

                                    if (expiryTimeMillis > 0) {
                                        subscriptionExpiryTimeMillis = expiryTimeMillis
                                        Timber.tag(TAG).d(
                                            "Subscription expires at: ${Date(expiryTimeMillis)}"
                                        )

                                        // Check if subscription is actually expired
                                        val currentTimeMillis = System.currentTimeMillis()
                                        if (expiryTimeMillis <= currentTimeMillis) {
                                            Timber
                                                .tag(TAG)
                                                .w(
                                                    "Subscription appears to be EXPIRED (expiry: ${
                                                        Date(expiryTimeMillis)
                                                    }, current: ${Date(currentTimeMillis)})"
                                                )
                                            // Consider this as no active subscription
                                            _purchaseState.value = PurchaseState.NotPurchased
                                            _isSubscriptionRenewing.value = false
                                            continue
                                        }
                                    }
                                } catch (e: Exception) {
                                    Timber.tag(TAG).e(e, "Error parsing subscription expiry time")
                                }
                            }

                            // Only set to Purchased if we have valid, non-expired purchases
                            if (_purchaseState.value != PurchaseState.NotPurchased) {
                                _purchaseState.value = PurchaseState.Purchased
                                Timber.tag(TAG).d(
                                    "Purchase state set to Purchased - found ${purchases.size} active subscription(s)"
                                )
                            }
                        } else {
                            Timber.tag(TAG).d("No existing purchases found")
                            // Reset subscription status
                            _isSubscriptionRenewing.value = false
                            subscriptionExpiryTimeMillis = 0
                            _purchaseState.value = PurchaseState.NotPurchased
                        }

                        // Call completion callback with successful result
                        onComplete?.invoke(purchases)
                    } else {
                        Timber.tag(TAG)
                            .e("Failed to query purchases: ${billingResult.responseCode} ${billingResult.debugMessage}")

                        // If we're in a payment processing state during retry, show error
                        if (_paymentProcessingState.value is PaymentProcessingState.Loading) {
                            _paymentProcessingState.value = PaymentProcessingState.Error(
                                UiText.StringText(
                                    "Failed to query purchases: ${billingResult.debugMessage}"
                                )
                            )
                        }

                        // Call completion callback with empty list to indicate failure
                        onComplete?.invoke(emptyList())
                    }

                    // Mark refresh as complete
                    _isRefreshingPurchases.value = false
                    Timber.tag(TAG).d("Purchase refresh completed")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Exception during purchase query ")
                // If we're in a payment processing state during retry, show error
                if (_paymentProcessingState.value is PaymentProcessingState.Loading) {
                    _paymentProcessingState.value = PaymentProcessingState.Error(
                        UiText.StringText(
                            "Error querying purchases: ${e.message}"
                        )
                    )
                }
                onComplete?.invoke(emptyList())
                _isRefreshingPurchases.value = false
            }
        }
    }

    private fun querySubscriptions() {
        if (!_isConnected.value) {
            Timber.tag(TAG).e("Cannot query subscriptions - billing client not connected")
            establishConnection()
            return
        }

        Timber.tag(TAG).i("Querying products")

        // For production, query all subscription plans
        val productList = SUBSCRIPTION_PLANS.map { plan ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(plan.productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        queryProductDetails(productList)
    }

    private fun queryProductDetails(productList: List<QueryProductDetailsParams.Product>) {
        if (productList.isEmpty()) {
            Timber.tag(TAG).e("Cannot query products - empty product list")
            return
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        Timber.tag(TAG).i("Querying ${productList.size} products in PRODUCTION mode")

        billingClient?.queryProductDetailsAsync(params) { billingResult, retrievedProductDetails ->
            val responseCodeName = when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> "OK (0)"
                BillingClient.BillingResponseCode.ERROR -> "ERROR (1)"
                BillingClient.BillingResponseCode.USER_CANCELED -> "USER_CANCELED (1)"
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE (2)"
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "BILLING_UNAVAILABLE (3)"
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "ITEM_UNAVAILABLE (4)"
                BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "DEVELOPER_ERROR (5)"
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "ITEM_ALREADY_OWNED (7)"
                BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "ITEM_NOT_OWNED (8)"
                else -> "UNKNOWN (${billingResult.responseCode})"
            }

            Timber.tag(TAG)
                .i("Product details query result: $responseCodeName, debug: ${billingResult.debugMessage}")
            val productDetailsList = retrievedProductDetails.productDetailsList
            Timber.tag(TAG).i("Retrieved $productDetailsList products")

            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    Timber.tag(TAG).i(retrievedProductDetails.toString())
                    if (productDetailsList.isNotEmpty()) {
                        // Store all retrieved product details
                        _productDetailsList.value = productDetailsList

                        // Also select the first product as default
                        val firstProduct = productDetailsList[0]
                        this.productDetails = firstProduct

                        // Log info about all retrieved products
                        productDetailsList.forEach { product ->
                            Timber.tag(TAG).i("Product details retrieved:")
                            Timber.tag(TAG).i("- Product ID: $ {product.productId}")
                            Timber.tag(TAG).i("- Name: $ {product.name}")
                            Timber.tag(TAG).i("- Description: ${product.description}")

                            product.subscriptionOfferDetails?.forEach { offer ->
                                Timber.tag(TAG)
                                    .i("Offer: ${offer.basePlanId}, token: ${offer.offerToken}")
                            }
                        }

                        // Select the default plan (first one)
                        selectPlan(0)
                    } else {
                        val productIds = productList.joinToString(", ") {
                            try {
                                it.javaClass.getDeclaredMethod("zza").invoke(it) as? String
                                    ?: "unknown"
                            } catch (e: Exception) {
                                e.printStackTrace()
                                "unknown"
                            }
                        }
                        Timber.tag(TAG).e("No products found for IDs: $productIds")

                        _purchaseState.value = PurchaseState.Error(
                            UiText.StringText("No products found")
                        )
                    }
                }

                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                    Timber.tag(TAG).e("Service disconnected, reconnecting...")
                    isConnected = false
                    establishConnection()
                }

                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                    Timber.tag(TAG).e("Service unavailable -check internet connection")
                    _purchaseState.value =
                        PurchaseState.Error(
                            UiText.StringText("Service unavailable - check internet connection")
                        )
                }

                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                    Timber.tag(TAG).e("Billing unavailable -check Play Store app")
                    _purchaseState.value =
                        PurchaseState.Error(
                            UiText.StringText("Billing unavailable - check Play Store app")
                        )
                }

                else -> {
                    Timber.tag(TAG)
                        .e("Failed to retrieve product details: ${billingResult.responseCode} ${billingResult.debugMessage}")
                    _purchaseState.value =
                        PurchaseState.Error(
                            UiText.StringText("Failed to retrieve product details: ${billingResult.debugMessage}")
                        )
                }
            }
        }
    }

    private var productDetails: ProductDetails? = null

    /**
     * Select a subscription plan by index
     * @param index Index of the plan in the SUBSCRIPTION_PLANS list
     * @return true if selection was successful, false otherwise
     */
    fun selectPlan(index: Int): Boolean {
        val productDetailsList = _productDetailsList.value

        // For production mode, check if index is valid
        if (index < 0 || index >= SUBSCRIPTION_PLANS.size) {
            Timber.tag(TAG).e(
                "Invalid plan index: $index, must be between 0 and ${SUBSCRIPTION_PLANS.size - 1}"
            )
            return false
        }

        // Find the product details for the selected plan
        val planProductId = SUBSCRIPTION_PLANS[index].productId
        Timber.tag(TAG).i("Selecting plan with product ID: $planProductId")

        val selectedProduct = productDetailsList.find { it.productId == planProductId }

        if (selectedProduct != null) {
            this.productDetails = selectedProduct
            _selectedPlanIndex.value = index
            Timber.tag(TAG)
                .i("Selected plan ${SUBSCRIPTION_PLANS[index].planName} with product ID $planProductId")
            return true
        } else {
            Timber.tag(TAG)
                .e("No product details found for plan index $index with product ID $planProductId")
            Timber.tag(TAG).i("Available products: ${productDetailsList.map { it.productId }}")
            return false
        }
    }


    private fun handlePurchase(purchase: Purchase) {
        Timber.tag(TAG).i("Handling purchase: ${purchase.purchaseState}")
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            _purchaseState.value = PurchaseState.Purchased
            // Set initial payment processing state
            _paymentProcessingState.value = PaymentProcessingState.Loading

            try {
                val productDetails = this.productDetails
                if (productDetails != null) {
                    // Extract amount and currency from ProductDetails
                    val priceAmountMicros =
                        productDetails.oneTimePurchaseOfferDetails?.priceAmountMicros
                            ?: productDetails.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceAmountMicros
                            ?: 0L
                    val amountInCents = (priceAmountMicros / 10_000).toInt() // $9.99 -> 999
                    val currencyCode = productDetails.oneTimePurchaseOfferDetails?.priceCurrencyCode
                        ?: productDetails.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceCurrencyCode
                        ?: "USD"

                    Timber.tag(TAG).i(
                        "Using Google Play pricing for payment: ${amountInCents / 100.0} $currencyCode (from priceAmountMicros: $priceAmountMicros)"
                    )

                    val inAppGooglePayload = InAppGooglePayload(
                        purchaseToken = purchase.purchaseToken,
                        customerID = currentCustomerID ?: "",
                        packageName = purchase.packageName,
                        productID = purchase.products.firstOrNull() ?: "",
                        orderID = purchase.orderId.toString()
                    )
                    Timber.tag(TAG).i(
                        "Created InAppGooglePayload with customerID: ${currentCustomerID ?: "not set"}"
                    )
                    val payment = Payment(type = "google", details = inAppGooglePayload)
                    val paymentTokenPayload = PaymentTokenPayload(
                        amount = amountInCents,
                        currency = currencyCode,
                        payment = payment
                    )
                    Timber.tag(TAG).i("Sending to createPaymentToken: $paymentTokenPayload")

                    // Update state to show we're processing with server
                    _paymentProcessingState.value = PaymentProcessingState.Verifying

                    _purchaseChannel.trySend(paymentTokenPayload)
                } else {
                    Timber.tag(TAG).e("Activity or product details not available ")
                    _paymentProcessingState.value = PaymentProcessingState.Error(
                        UiText.StringText(
                            "Application error: could not process payment"
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error sending payment token to WebView ")
                _paymentProcessingState.value = PaymentProcessingState.Error(
                    UiText.StringText("Error processing payment: ${e.message}")
                )
            }
            // --- End send payment token ---

            // Acknowledge the purchase if it hasn't been acknowledged yet
            if (!purchase.isAcknowledged) {
                Timber.tag(TAG).i("Acknowledging purchase")
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                coroutineScope.launch {
                    try {
                        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                            Timber.tag(TAG)
                                .i("Acknowledge result: ${billingResult.responseCode} ${billingResult.debugMessage}")
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                Timber.tag(TAG).i("Purchase acknowledged")
                            } else {
                                Timber.tag(TAG)
                                    .e("Failed to acknowledge purchase: ${billingResult.debugMessage}")
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Error acknowledging purchase")
                    }
                }
            }
        }
    }

    /**
     * Check if the user has an active subscription
     * This method will return true even for cancelled subscriptions that are still within their valid period
     */
    fun hasActiveSubscription(): Boolean {
        return _purchaseState.value is PurchaseState.Purchased
    }

    /**
     * Check if the user has an active and renewing subscription
     * This only returns true for subscriptions that are set to automatically renew
     */
    fun hasRenewingSubscription(): Boolean {
        return hasActiveSubscription() && _isSubscriptionRenewing.value
    }

    /**
     * Get information about the subscription status
     * @return A tuple containing (isActive, isRenewing, expiryTimeMillis)
     */
    fun getSubscriptionStatus(): Triple<Boolean, Boolean, Long> {
        val isActive = hasActiveSubscription()
        val isRenewing = _isSubscriptionRenewing.value
        return Triple(isActive, isRenewing, subscriptionExpiryTimeMillis)
    }

    /**
     * Refresh the purchase status to get the latest subscription information
     * Call this when displaying subscription information to ensure it's up-to-date
     */
    fun refreshPurchaseStatus(forceRefresh: Boolean = false) {
        if (!_isConnected.value) {
            Timber.tag(TAG).i("Cannot refresh purchases - billing client not connected")
            establishConnection()
            return
        }

        Timber.tag(TAG).i("Refreshing purchase status (force: $forceRefresh)...")
        _isRefreshingPurchases.value = true
        queryExistingPurchases(forceRefresh = forceRefresh)
    }

    /**
     * Start periodic refresh for active subscriptions to catch expiration/cancellation faster
     */
    private fun startPeriodicRefresh() {
        // Cancel existing job
        periodicRefreshJob?.cancel()

        periodicRefreshJob = coroutineScope.launch {
            while (true) {
                delay(PURCHASE_REFRESH_INTERVAL_MS)

                // Only refresh if we have an active subscription
                if (hasActiveSubscription()) {
                    Timber.tag(TAG).i("Periodic refresh - checking subscription status")
                    queryExistingPurchases(forceRefresh = true)
                } else {
                    Timber.tag(TAG).i("No active subscription - skipping periodic refresh")
                }
            }
        }
    }

    /**
     * Stop periodic refresh
     */
    private fun stopPeriodicRefresh() {
        periodicRefreshJob?.cancel()
        periodicRefreshJob = null
    }

    /**
     * Invalidate all cached data and force a fresh query
     */
    fun invalidateCache() {
        Timber.tag(TAG).i("Invalidating subscription cache")
        lastPurchaseQueryTime = 0L
        lastProductDetailsQueryTime = 0L
        cachedPurchases = emptyList()
        refreshPurchaseStatus(forceRefresh = true)
    }

    /**
     * Initiates the billing flow for a specific product ID and offer token.
     *
     * @param productId The Google Play product ID to purchase.
     * @param offerToken The specific offer token for the subscription plan (can be null for base plans).
     * @param customerID The customer ID to associate with the purchase (can be null if not applicable).
     */
    fun launchBillingFlowForProduct(
        productId: String,
        offerToken: String?,
        customerID: String? = null,
        getBillingResult: (BillingClient?, BillingFlowParams) -> BillingResult?
    ) {
        if (!_isConnected.value) {
            Timber.tag(TAG).e("Cannot launch billing flow -billing client not connected.")
            _purchaseState.value = PurchaseState.Error(
                UiText.StringText("Billing service not connected.")
            )
            return
        }

        // Store customerID for use during purchase processing
        this.currentCustomerID = customerID

        Timber.tag(TAG)
            .i("Attempting to launch purchase flow for Product ID: $productId, Offer Token: $offerToken, Customer ID: $customerID")

        val productDetails = _productDetailsList.value.find { it.productId == productId }

        if (productDetails == null) {
            Timber.tag(TAG).e("Product details not found for $productId.Cannot launch flow.")
            _purchaseState.value = PurchaseState.Error(
                UiText.StringText("Selected plan details not found.")
            )
            return
        }

        // Construct the ProductDetailsParams builder
        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        // Find and set the offer token if provided and valid
        if (offerToken != null && productDetails.subscriptionOfferDetails != null) {
            val selectedOffer =
                productDetails.subscriptionOfferDetails?.find { it.offerToken == offerToken }
            if (selectedOffer != null) {
                Timber.tag(TAG).i("Found matching offer token: $offerToken")
                productDetailsParamsBuilder.setOfferToken(selectedOffer.offerToken)
            } else {
                // Handle case where the offer token from JS doesn't match any available offer
                // This might happen if eligibility changed or plans updated.
                // Fallback: Try purchasing the base plan if offerToken is invalid/not found?
                // Or show an error.
                Timber.tag(TAG)
                    .i("Provided offer token '$offerToken' not found for product $productId. Check plan configuration or user eligibility.")
                // Optionally, default to the base plan if no offer token is needed/found
                // If *only* specific offers should be purchasable, then this should be an error:
                _purchaseState.value =
                    PurchaseState.Error(
                        UiText.StringText(
                            "Selected plan offer is not available. " +
                                    "Please try again or select a different plan."
                        )
                    )
                // return // Uncomment this line if purchase should fail when offer token is invalid
            }
        } else if (offerToken != null && productDetails.subscriptionOfferDetails == null) {
            Timber.tag(TAG)
                .i("Offer token '$offerToken' provided, but product $productId has no subscription offer details.")
            _purchaseState.value = PurchaseState.Error(
                UiText.StringText("Plan configuration error.")
            )
            return
        } else {
            Timber.tag(TAG).i("No specific offer token provided or needed for product $productId.")
            // If offerToken is null, we don't call setOfferToken, which usually defaults to the base plan
        }

        val productDetailsParamsList = listOf(productDetailsParamsBuilder.build())

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .setObfuscatedAccountId(customerID!!)
            .build()

        val billingResult = getBillingResult(billingClient, billingFlowParams)

        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.tag(TAG)
                .e("Failed to launch billing flow: ${billingResult?.responseCode} ${billingResult?.debugMessage}")
            _purchaseState.value =
                PurchaseState.Error(
                    UiText.StringText("Failed to initiate purchase: ${billingResult?.debugMessage}")
                )
        } else {
            Timber.tag(TAG).i("Billing flow launched successfully.")
            // Purchase process continues in PurchasesUpdatedListener
        }
    }

    /**
     * Retry the payment process for a purchase that had an error
     */
    fun retryPaymentVerification() {
        try {
            val currentState = _paymentProcessingState.value

            if (currentState is PaymentProcessingState.SubscriptionRecovery) {
                Timber.tag(TAG)
                    .i("Retrying subscription recovery - processing existing Google Play purchase")
                triggerSubscriptionRecovery()
            } else {
                Timber.tag(TAG).i("Retrying payment verification")
                // Set state back to loading
                _paymentProcessingState.value = PaymentProcessingState.Loading

                // Query existing purchases to get the purchase token again - force refresh
                queryExistingPurchases(
                    onComplete = { purchases ->
                        if (purchases.isNotEmpty()) {
                            // Found purchase - check if it's already acknowledged and processed
                            val purchase = purchases.first()
                            Timber.tag(TAG).i("Found purchase to retry: ${purchase.products}")

                            // If we already have a purchased state, the purchase is valid
                            if (_purchaseState.value is PurchaseState.Purchased) {
                                Timber.tag(TAG)
                                    .i("Purchase already processed successfully, setting state to Success")
                                _paymentProcessingState.value = PaymentProcessingState.Success
                            } else {
                                // Process the purchase normally
                                handlePurchase(purchase)
                            }
                        } else {
                            Timber.tag(TAG).e("No purchases found during retry")
                            _paymentProcessingState.value = PaymentProcessingState.Error(
                                UiText.StringText(
                                    "No active purchase found to retry. Please try purchasing again."
                                )
                            )
                        }
                    },
                    forceRefresh = true // Always get fresh data for retry
                )
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error retrying payment verification ")
            _paymentProcessingState.value = PaymentProcessingState.Error(
                UiText.StringText(
                    "Error retrying payment: ${e.message}"
                )
            )
        }
    }


    /**
     * Reset the payment processing state
     */
    fun resetPaymentState() {
        _paymentProcessingState.value = null
    }

    /**
     * Trigger subscription recovery for API/Google Play mismatch
     */
    fun triggerSubscriptionRecovery() {
        if (!_isConnected.value) {
            Timber.tag(TAG).e("Cannot process recovery - billing client not connected ")
            _paymentProcessingState.value =
                PaymentProcessingState.Error(
                    UiText.StringText("Billing service not connected")
                )
            return
        }

        Timber.tag(TAG).i("Triggering subscription recovery")
        _paymentProcessingState.value = PaymentProcessingState.SubscriptionRecovery(
            "We found your subscription in Google Play but it's not synced with our servers. Let's fix that!"
        )

        // Process existing purchase directly - force refresh to get latest data
        queryExistingPurchases(
            onComplete = { purchases ->
                if (purchases.isNotEmpty()) {
                    val purchase = purchases.first()
                    Timber.tag(TAG).i("Processing recovery for purchase: ${purchase.products}")

                    // Extract and set customer ID from existing purchase
                    purchase.accountIdentifiers?.obfuscatedAccountId?.let { accountId ->
                        Timber.tag(TAG).i("Using obfuscated account ID from purchase: $accountId")
                        currentCustomerID = accountId
                    } ?: Timber.tag(TAG).i("No obfuscated account ID found in purchase ")

                    // Set to loading and process through normal payment flow
                    _paymentProcessingState.value = PaymentProcessingState.Loading
                    handlePurchase(purchase)
                } else {
                    _paymentProcessingState.value = PaymentProcessingState.Error(
                        UiText.StringText(
                            "No active Google Play subscription found to recover"
                        )
                    )
                }
            },
            forceRefresh = true // Always get fresh data for recovery
        )
    }

    sealed class PurchaseState {
        data object NotPurchased : PurchaseState()
        data object Purchased : PurchaseState()
        object Cancelled : PurchaseState()
        data class Error(val message: UiText) : PurchaseState()
    }
}
