package me.proton.android.lumo.billing

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.proton.android.lumo.BuildConfig
import me.proton.android.lumo.LumoBillingClient
import me.proton.android.lumo.models.PaymentTokenPayload
import me.proton.android.lumo.ui.text.UiText

class BillingEffectHandler(
    private val backend: BillingBackend,
    private val billingClient: LumoBillingClient,
    private val scope: CoroutineScope,
    private val dispatch: (BillingAction) -> Unit
) {
    @Volatile
    private var connecting = false

    fun handle(effect: BillingEffect) {
        when (effect) {
            is BillingEffect.ConnectBilling -> connect()
            is BillingEffect.QueryProducts -> queryProducts()
            is BillingEffect.QueryPurchases -> queryPurchases()
            is BillingEffect.LaunchBillingFlow -> launchBillingFlow(effect)
            is BillingEffect.AcknowledgePurchase -> acknowledge(effect.purchaseToken)
            is BillingEffect.SendPaymentToken -> sendToBackend(effect.payload)
        }
    }

    private fun connect() {
        if (connecting) return

        connecting = true

        billingClient.start(
            purchasesUpdatedListener = object : GooglePurchasesUpdatedListener {
                override fun onSuccess(googlePurchase: GooglePurchase) {
                    dispatch(BillingAction.PurchaseUpdated(googlePurchase))
                }

                override fun onError(message: String) {
                    dispatch(
                        BillingAction.Error(
                            UiText.StringText(message)
                        )
                    )
                }
            },
            stateListener = object : GoogleBillingClientStateListener {
                override fun onConnected() {
                    connecting = false
                    dispatch(BillingAction.BillingConnected)
                }

                override fun onDisconnected(reason: String?) {
                    connecting = false
                    dispatch(BillingAction.BillingDisconnected(reason))
                }
            })
    }

    private fun queryProducts() {
        scope.launch {
            val planResult = backend.fetchPlans()
            if (planResult.error != null) {
                dispatch(BillingAction.Error(planResult.error))
            } else {
                billingClient.queryProductsAsync(
                    object : GoogleProductDetailsResponseListener {
                        override fun onSuccess(
                            googleProductDetails: List<GoogleProductDetails>,
                            productDetails: List<ProductDetails>
                        ) {

                            val updatePlans = updatePlanPricing(
                                plans = planResult.planOptions,
                                productDetails = googleProductDetails,
                                offerId = BuildConfig.OFFER_ID
                            )
                            dispatch(
                                BillingAction.ProductDetailsLoaded(
                                    googleProductDetails = googleProductDetails,
                                    products = productDetails,
                                    planFeatures = planResult.planFeatures,
                                    planOptions = updatePlans,
                                )
                            )
                        }

                        override fun onError(message: String) {
                            dispatch(
                                BillingAction.Error(
                                    UiText.StringText(message)
                                )
                            )
                        }
                    })
            }
        }
    }

    private fun queryPurchases() {
        scope.launch {
            val subscriptionResult = backend.fetchSubscriptions()
            if (subscriptionResult.error != null) {
                dispatch(BillingAction.Error(subscriptionResult.error))
            } else {
                billingClient.queryPurchasesAsync(object : GooglePurchaseResponseListener {
                    override fun onSuccess(
                        googlePurchase: GooglePurchase?,
                        renewing: Boolean,
                        expiry: Long
                    ) {
                        dispatch(
                            BillingAction.PurchasesLoaded(
                                purchase = googlePurchase,
                                renewing = renewing,
                                expiry = expiry,
                                subscriptionResult = subscriptionResult,
                            )
                        )
                    }

                    override fun onError(message: String) {
                        dispatch(
                            BillingAction.Error(UiText.StringText(message))
                        )
                    }
                })
            }
        }
    }

    private fun launchBillingFlow(effect: BillingEffect.LaunchBillingFlow) {
        val result = billingClient.launchBilling(
            productDetails = effect.productDetails,
            offerToken = effect.offerToken,
            customerId = effect.customerId
        )
        if (!result) {
            dispatch(
                BillingAction.Error(
                    UiText.StringText("No active screen to launch billing")
                )
            )
        }
    }

    private fun acknowledge(token: String) {
        billingClient.acknowledge(
            token = token,
            acknowledgePurchaseResponseListener = object :
                GoogleAcknowledgePurchaseResponseListener {
                override fun onError(message: String) {
                    dispatch(
                        BillingAction.Error(
                            UiText.StringText(message)
                        )
                    )
                }
            })
    }

    private fun sendToBackend(payload: PaymentTokenPayload) {
        scope.launch {
            when (val result = backend.verifyPurchase(payload)) {
                BackendResult.Success -> {
                    dispatch(BillingAction.BackendVerificationSucceeded)
                }

                is BackendResult.Failure -> {
                    dispatch(
                        BillingAction.BackendVerificationFailed(
                            message = result.message
                        )
                    )
                }
            }
        }
    }
}

interface GooglePurchasesUpdatedListener {
    fun onSuccess(googlePurchase: GooglePurchase)
    fun onError(message: String)
}

data class GooglePurchase(
    val orderId: String = "",
    val packageName: String = "",
    val productId: String = "",
    val purchaseTime: Long = 0L,
    val purchaseState: Int = 0,
    val purchaseToken: String = "",
    val quantity: Int = 0,
    val isAutoRenewing: Boolean = false,
    val isAcknowledged: Boolean = false,
    val obfuscatedAccountId: String? = null,
    val developerPayload: String = "",
    val products: List<String> = emptyList(),
    val accountIdentifiers: String = "",
) {
    companion object {
        fun from(purchase: Purchase): GooglePurchase {
            return GooglePurchase(
                orderId = purchase.orderId ?: "",
                packageName = purchase.packageName,
                productId = purchase.products.firstOrNull() ?: "",
                purchaseTime = purchase.purchaseTime,
                purchaseState = purchase.purchaseState,
                purchaseToken = purchase.purchaseToken,
                quantity = purchase.quantity,
                isAutoRenewing = purchase.isAutoRenewing,
                isAcknowledged = purchase.isAcknowledged,
                obfuscatedAccountId = purchase.accountIdentifiers?.obfuscatedAccountId,
                developerPayload = purchase.developerPayload,
                products = purchase.products,
                accountIdentifiers = purchase.accountIdentifiers?.obfuscatedAccountId ?: ""
            )
        }
    }
}

interface GooglePurchaseResponseListener {
    fun onSuccess(
        googlePurchase: GooglePurchase?,
        renewing: Boolean,
        expiry: Long
    )

    fun onError(message: String)
}

interface GoogleProductDetailsResponseListener {
    fun onSuccess(
        googleProductDetails: List<GoogleProductDetails>,
        productDetails: List<ProductDetails>
    )

    fun onError(message: String)
}

data class GoogleProductDetails(
    val productId: String,
    val productType: String,
    val title: String,
    val name: String,
    val description: String,
    val subscriptionOfferDetails: List<GoogleSubscriptionOfferDetails>
) {
    data class GoogleSubscriptionOfferDetails(
        val offerToken: String,
        val basePlanId: String,
        val pricingPhases: List<GooglePricingPhase>,
        val offerTags: List<String>,
        val offerId: String?
    )

    data class GooglePricingPhase(
        val priceAmountMicros: Long,
        val priceCurrencyCode: String,
        val formattedPrice: String,
        val billingPeriod: String,
        val recurrenceMode: Int
    )

    companion object {
        fun from(productDetails: ProductDetails): GoogleProductDetails {
            return GoogleProductDetails(
                productId = productDetails.productId,
                productType = productDetails.productType,
                title = productDetails.title,
                name = productDetails.name,
                description = productDetails.description,
                subscriptionOfferDetails = productDetails.subscriptionOfferDetails?.map { offer ->
                    GoogleSubscriptionOfferDetails(
                        offerId = offer.offerId,
                        offerToken = offer.offerToken,
                        basePlanId = offer.basePlanId,
                        pricingPhases = offer.pricingPhases.pricingPhaseList.map { phase ->
                            GooglePricingPhase(
                                priceAmountMicros = phase.priceAmountMicros,
                                priceCurrencyCode = phase.priceCurrencyCode,
                                formattedPrice = phase.formattedPrice,
                                billingPeriod = phase.billingPeriod,
                                recurrenceMode = phase.recurrenceMode,
                            )
                        },
                        offerTags = offer.offerTags
                    )
                } ?: emptyList()
            )
        }
    }
}

interface GoogleAcknowledgePurchaseResponseListener {
    fun onError(message: String)
}

interface GoogleBillingClientStateListener {
    fun onConnected()
    fun onDisconnected(reason: String?)
}