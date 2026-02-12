package me.proton.android.lumo.billing

import com.android.billingclient.api.ProductDetails
import me.proton.android.lumo.LumoBillingClient

class TestBillingClient : LumoBillingClient {

    var connected: Boolean = false
    var handleUpdate: Boolean = false
    var purchase: GooglePurchase? = null
    var acknowledgeSuccess: Boolean = false
    var googleProductDetails: List<GoogleProductDetails>? = null

    override fun start(
        purchasesUpdatedListener: GooglePurchasesUpdatedListener,
        stateListener: GoogleBillingClientStateListener
    ) {
        if (connected) {
            stateListener.onConnected()
        } else {
            stateListener.onDisconnected(
                reason = null,
                isBillingAvailable = false
            )
        }

        if (handleUpdate) {
            purchase?.let {
                purchasesUpdatedListener.onSuccess(it)
            } ?: purchasesUpdatedListener.onError("No purchase available")
        }
    }

    override fun queryPurchasesAsync(purchasesResponseListener: GooglePurchaseResponseListener) {
        purchase?.let {
            purchasesResponseListener.onSuccess(
                googlePurchase = it,
                renewing = true,
                expiry = 0L
            )
        } ?: purchasesResponseListener.onError("no purchases")
    }

    override fun queryProductsAsync(productDetailsResponseListener: GoogleProductDetailsResponseListener) {
        googleProductDetails?.let {
            productDetailsResponseListener.onSuccess(
                googleProductDetails = it,
                productDetails = emptyList()
            )
        } ?: productDetailsResponseListener.onError("null products")

    }

    override fun launchBilling(
        productDetails: ProductDetails,
        offerToken: String?,
        customerId: String?
    ): Boolean = customerId != null

    override fun acknowledge(
        token: String,
        acknowledgePurchaseResponseListener: GoogleAcknowledgePurchaseResponseListener
    ) {
        if (!acknowledgeSuccess) {
            acknowledgePurchaseResponseListener.onError("unable to ack")
        }
    }
}