package me.proton.android.lumo

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import me.proton.android.lumo.billing.BillingAction
import me.proton.android.lumo.billing.parseSubscription
import me.proton.android.lumo.ui.text.UiText

interface LumoBillingClient {

    fun start(
        purchasesUpdatedListener: PurchasesUpdatedListener,
        stateListener: BillingClientStateListener,
    )

    fun queryPurchasesAsync(
        queryPurchasesParams: QueryPurchasesParams,
        purchasesResponseListener: PurchasesResponseListener,
    )

    fun queryProductsAsync(
        queryProductDetailsParams: QueryProductDetailsParams,
        productDetailsResponseListener: ProductDetailsResponseListener,
    )

    fun launchBilling(
        activity: Activity,
        billingFlowParams: BillingFlowParams
    )

    fun acknowledge(
        acknowledgePurchaseParams: AcknowledgePurchaseParams,
        acknowledgePurchaseResponseListener: AcknowledgePurchaseResponseListener
    )
}

class LumoBillingClientImpl(
    private val context: Context,
    private val dispatch: (BillingAction) -> Unit
) : LumoBillingClient {

    private var billingClient: BillingClient? = null

    override fun start(
        purchasesUpdatedListener: PurchasesUpdatedListener,
        stateListener: BillingClientStateListener,
    ) {
        billingClient = try {
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
    }

    override fun queryPurchasesAsync(
        queryPurchasesParams: QueryPurchasesParams,
        purchasesResponseListener: PurchasesResponseListener
    ) {
        billingClient?.queryPurchasesAsync(
            queryPurchasesParams,
            purchasesResponseListener
        )
    }

    override fun queryProductsAsync(
        queryProductDetailsParams: QueryProductDetailsParams,
        productDetailsResponseListener: ProductDetailsResponseListener
    ) {
        billingClient?.queryProductDetailsAsync(
            queryProductDetailsParams,
            productDetailsResponseListener
        )
    }

    override fun launchBilling(
        activity: Activity,
        billingFlowParams: BillingFlowParams
    ) {
        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    override fun acknowledge(
        acknowledgePurchaseParams: AcknowledgePurchaseParams,
        acknowledgePurchaseResponseListener: AcknowledgePurchaseResponseListener
    ) {
        billingClient?.acknowledgePurchase(
            acknowledgePurchaseParams,
            acknowledgePurchaseResponseListener
        )
    }
}