package me.proton.android.lumo.money_machine

import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.proton.android.lumo.BuildConfig
import me.proton.android.lumo.models.PaymentTokenPayload
import me.proton.android.lumo.models.SubscriptionPlan
import me.proton.android.lumo.ui.text.UiText

class BillingEffectHandler(
    context: Context,
    private val activityProvider: ActivityProvider,
    private val backend: BillingBackend,
    private val scope: CoroutineScope,
    private val dispatch: (BillingAction) -> Unit
) {
    @Volatile
    private var connecting = false
    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { result, purchases ->
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchases?.forEach {
                        dispatch(BillingAction.PurchaseUpdated(it))
                    }
                }

                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    dispatch(
                        BillingAction.Error(
                            UiText.StringText("R.string.billing_user_cancelled")
                        )
                    )
                }

                else -> {
                    dispatch(
                        BillingAction.Error(
                            UiText.StringText(result.debugMessage)
                        )
                    )
                }
            }
        }

    private val billingClient: BillingClient? = try {
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
        dispatch(
            BillingAction.Error(
                UiText.StringText("Google Play Billing not available")
            )
        )
        null
    }


    fun handle(effect: BillingEffect) {
        Log.e("WTF", "Effect handler: $effect")
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
        val client = billingClient ?: return
        if (connecting) return

        connecting = true

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                dispatch(
                    BillingAction.BillingConnected(
                        result.responseCode == BillingClient.BillingResponseCode.OK
                    )
                )
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
                dispatch(BillingAction.BillingDisconnected(null))
            }
        })
    }

    private fun queryProducts() {
        val client = billingClient ?: return

        val products = SUBSCRIPTION_PLANS.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        scope.launch {
            val planResult = backend.fetchPlans()
            if (planResult.error != null) {
                dispatch(BillingAction.Error(planResult.error))
            } else {
                client.queryProductDetailsAsync(params) { result, details ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        val updatePlans = updatePlanPricing(
                            plans = planResult.planOptions,
                            productDetails = details.productDetailsList,
                            offerId = BuildConfig.OFFER_ID
                        )
                        dispatch(
                            BillingAction.ProductDetailsLoaded(
                                products = details.productDetailsList,
                                planFeatures = planResult.planFeatures,
                                planOptions = updatePlans,
                            )
                        )
                    } else {
                        dispatch(
                            BillingAction.Error(
                                UiText.StringText(result.debugMessage)
                            )
                        )
                    }
                }
            }
        }
    }

    private fun queryPurchases() {
        val client = billingClient ?: return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        scope.launch {
            val subscriptionResult = backend.fetchSubscriptions()
            if (subscriptionResult.error != null) {
                dispatch(BillingAction.Error(subscriptionResult.error))
            } else {
                client.queryPurchasesAsync(params) { result, purchases ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        purchases.firstOrNull().let {
                            val (renewing, expiry) = if (it != null) {
                                parseSubscription(it)
                            } else {
                                false to 0L
                            }

                            dispatch(
                                BillingAction.PurchasesLoaded(
                                    purchase = it,
                                    renewing = renewing,
                                    expiry = expiry,
                                    subscriptionResult = subscriptionResult,
                                )
                            )
                        }
                    } else if (subscriptionResult.hasValidSubscription) {
                        dispatch(
                            BillingAction.PurchasesLoaded(
                                purchase = null,
                                renewing = false,
                                expiry = 0L,
                                subscriptionResult = subscriptionResult,
                            )
                        )
                    } else {
                        dispatch(
                            BillingAction.Error(UiText.StringText(result.debugMessage))
                        )
                    }
                }
            }
        }
    }


    private fun launchBillingFlow(effect: BillingEffect.LaunchBillingFlow) {
        val client = billingClient ?: return
        val activity = activityProvider.currentActivity()
            ?: run {
                dispatch(
                    BillingAction.Error(
                        UiText.StringText("No active screen to launch billing")
                    )
                )
                return
            }

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(effect.productDetails)
                        .apply { effect.offerToken?.let { setOfferToken(it) } }
                        .build()
                )
            )
            .setObfuscatedAccountId(effect.customerId)
            .build()

        client.launchBillingFlow(activity, params)
    }

    private fun acknowledge(token: String) {
        val client = billingClient ?: return

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(token)
            .build()

        client.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                dispatch(
                    BillingAction.Error(
                        UiText.StringText("Failed to acknowledge purchase")
                    )
                )
            }
        }
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

    companion object {
        private val SUBSCRIPTION_PLANS = listOf(
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

    }
}
