package me.proton.android.lumo

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import me.proton.android.lumo.billing.GoogleAcknowledgePurchaseResponseListener
import me.proton.android.lumo.billing.GoogleBillingClientStateListener
import me.proton.android.lumo.billing.GoogleProductDetails
import me.proton.android.lumo.billing.GoogleProductDetailsResponseListener
import me.proton.android.lumo.billing.GooglePurchase
import me.proton.android.lumo.billing.GooglePurchaseResponseListener
import me.proton.android.lumo.billing.GooglePurchasesUpdatedListener
import me.proton.android.lumo.billing.parseSubscription
import me.proton.android.lumo.models.SubscriptionPlan
import timber.log.Timber

interface LumoBillingClient {

    fun start(
        purchasesUpdatedListener: GooglePurchasesUpdatedListener,
        stateListener: GoogleBillingClientStateListener,
    )

    fun queryPurchasesAsync(
        purchasesResponseListener: GooglePurchaseResponseListener,
    )

    fun queryProductsAsync(
        productDetailsResponseListener: GoogleProductDetailsResponseListener,
    )

    fun launchBilling(
        productDetails: ProductDetails,
        offerToken: String?,
        customerId: String?,
    ): Boolean

    fun acknowledge(
        token: String,
        acknowledgePurchaseResponseListener: GoogleAcknowledgePurchaseResponseListener
    )
}

class LumoBillingClientImpl(
    private val context: Context,
    private val activityProvider: ActivityProvider,
) : LumoBillingClient {

    private var billingClient: BillingClient? = null
    private var stateListener: GoogleBillingClientStateListener? = null

    @Suppress("TooGenericExceptionCaught")
    override fun start(
        purchasesUpdatedListener: GooglePurchasesUpdatedListener,
        stateListener: GoogleBillingClientStateListener,
    ) {
        this.stateListener = stateListener
        billingClient =
            try {
                BillingClient.newBuilder(context)
                    .setListener { result, purchases ->
                        when (result.responseCode) {
                            BillingClient.BillingResponseCode.OK -> {
                                purchases
                                    ?.map { GooglePurchase.from(it) }
                                    ?.distinct()
                                    ?.forEach {
                                        purchasesUpdatedListener.onSuccess(it)
                                    }
                            }

                            BillingClient.BillingResponseCode.USER_CANCELED -> {
                                // ignored
                            }

                            else -> purchasesUpdatedListener.onError(result.debugMessage)
                        }
                    }
                    .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                            .enableOneTimeProducts()
                            .build()
                    )
                    .enableAutoServiceReconnection()
                    .build()
            } catch (e: Exception) {
                Timber.tag(TAG).e("Unable to setup billing: $e")
                handleBillingClient()
                null
            }?.also {
                it.startConnection(
                    object : BillingClientStateListener {
                        override fun onBillingSetupFinished(result: BillingResult) {
                            when (result.responseCode) {
                                BillingClient.BillingResponseCode.OK ->
                                    stateListener.onConnected()

                                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
                                    stateListener.onDisconnected(
                                        reason = result.debugMessage,
                                        isBillingAvailable = false,
                                    )

                                else -> stateListener.onDisconnected(
                                    reason = null,
                                    isBillingAvailable = true,
                                )
                            }
                        }

                        override fun onBillingServiceDisconnected() {
                            stateListener.onDisconnected(
                                reason = null,
                                isBillingAvailable = true
                            )
                        }
                    }
                )
            }
    }

    override fun queryPurchasesAsync(
        purchasesResponseListener: GooglePurchaseResponseListener
    ) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        handleBillingClient { billingClient ->
            billingClient.queryPurchasesAsync(
                params
            ) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases.firstOrNull().let { purchase ->
                        val (renewing, expiry) = if (purchase != null) {
                            parseSubscription(purchase)
                        } else {
                            false to 0L
                        }

                        purchasesResponseListener.onSuccess(
                            googlePurchase = purchase?.let { GooglePurchase.from(it) },
                            renewing = renewing,
                            expiry = expiry,
                        )
                    }
                } else {
                    purchasesResponseListener.onError(result.debugMessage)
                }
            }

        }
    }

    override fun queryProductsAsync(
        productDetailsResponseListener: GoogleProductDetailsResponseListener
    ) {
        val products = SUBSCRIPTION_PLANS.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        handleBillingClient { billingClient ->
            billingClient.queryProductDetailsAsync(
                params,
            ) { result, details ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productDetails = details.productDetailsList
                    val googleProductDetails =
                        productDetails.map { GoogleProductDetails.from(it) }
                    productDetailsResponseListener.onSuccess(
                        googleProductDetails = googleProductDetails,
                        productDetails = productDetails
                    )
                } else {
                    productDetailsResponseListener.onError(result.debugMessage)
                }
            }
        }
    }

    override fun launchBilling(
        productDetails: ProductDetails,
        offerToken: String?,
        customerId: String?,
    ): Boolean {
        val activity = activityProvider.currentActivity()
        if (activity == null || customerId.isNullOrEmpty()) {
            return false
        }

        val builder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .apply { offerToken?.let { setOfferToken(it) } }
                        .build()
                )
            )

        builder.setObfuscatedAccountId(customerId)

        handleBillingClient { it.launchBillingFlow(activity, builder.build()) }

        return true
    }

    override fun acknowledge(
        token: String,
        acknowledgePurchaseResponseListener: GoogleAcknowledgePurchaseResponseListener
    ) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(token)
            .build()

        handleBillingClient {
            it.acknowledgePurchase(params) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    acknowledgePurchaseResponseListener.onError("Failed to acknowledge purchase")
                }
            }
        }
    }

    private fun handleBillingClient(handle: (BillingClient) -> Unit = {}) {
        billingClient?.let { handle(it) }
            ?: stateListener?.onDisconnected(
                reason = "Google Play Billing not available",
                isBillingAvailable = false
            )
    }

    companion object {
        val MONTHLY_PLAN = SubscriptionPlan(
            productId = "giaplumo_lumo2025_1_renewing",
            planName = "1 Month",
            durationMonths = 1,
            // Note: This is a constant, localized descriptions are handled in UI
            description = "Monthly subscription",
        )
        val YEARLY_PLAN = SubscriptionPlan(
            productId = "giaplumo_lumo2025_12_renewing",
            planName = "12 Months",
            durationMonths = 12,
            // Note: This is a constant, localized descriptions are handled in UI
            description = "Annual subscription (save 20%)",
        )
        val SUBSCRIPTION_PLANS = listOf(MONTHLY_PLAN, YEARLY_PLAN)

        private const val TAG = "LumoBillingClient"
    }
}
