package me.proton.android.lumo.models

import com.android.billingclient.api.Purchase
import me.proton.android.lumo.LumoBillingClientImpl.Companion.MONTHLY_PLAN
import me.proton.android.lumo.LumoBillingClientImpl.Companion.YEARLY_PLAN
import me.proton.android.lumo.billing.GooglePurchase

object GooglePurchaseFixtures {

    private const val PACKAGE_NAME = "me.proton.lumo"

    fun createLumoPlus12MonthPurchase(
        purchaseToken: String = "test.purchase.token.lumo12month",
        orderId: String = "GPA.1234-5678-9012-34567",
        purchaseTime: Long = System.currentTimeMillis(),
        isAutoRenewing: Boolean = true,
        acknowledged: Boolean = true,
        purchaseState: Int = 1, // PURCHASED
        obfuscatedAccountId: String? = "customer123"
    ): GooglePurchase =
        GooglePurchase(
            orderId = orderId,
            packageName = PACKAGE_NAME,
            productId = YEARLY_PLAN.productId,
            purchaseTime = purchaseTime,
            purchaseState = purchaseState,
            purchaseToken = purchaseToken,
            quantity = 1,
            isAutoRenewing = isAutoRenewing,
            isAcknowledged = acknowledged,
            obfuscatedAccountId = obfuscatedAccountId,
            developerPayload = "",
            products = listOf(YEARLY_PLAN.productId),
            accountIdentifiers = obfuscatedAccountId.orEmpty()
        )

    fun createLumoPlus1MonthPurchase(
        purchaseToken: String = "test.purchase.token.lumo1month",
        orderId: String = "GPA.9876-5432-1098-76543",
        purchaseTime: Long = System.currentTimeMillis(),
        isAutoRenewing: Boolean = true,
        acknowledged: Boolean = true,
        purchaseState: Int = 1,
        obfuscatedAccountId: String? = "customer456"
    ): GooglePurchase =
        GooglePurchase(
            orderId = orderId,
            packageName = PACKAGE_NAME,
            productId = MONTHLY_PLAN.productId,
            purchaseTime = purchaseTime,
            purchaseState = purchaseState,
            purchaseToken = purchaseToken,
            quantity = 1,
            isAutoRenewing = isAutoRenewing,
            isAcknowledged = acknowledged,
            obfuscatedAccountId = obfuscatedAccountId,
            developerPayload = "",
            products = listOf(MONTHLY_PLAN.productId),
            accountIdentifiers = obfuscatedAccountId.orEmpty()
        )

    fun createExpiredPurchase(
        purchaseToken: String = "test.purchase.token.expired",
        orderId: String = "GPA.0000-1111-2222-33333",
        isAutoRenewing: Boolean = false
    ): GooglePurchase {
        val purchaseTime =
            System.currentTimeMillis() - (400L * 24 * 60 * 60 * 1000)

        return createLumoPlus12MonthPurchase(
            purchaseToken = purchaseToken,
            orderId = orderId,
            purchaseTime = purchaseTime,
            isAutoRenewing = isAutoRenewing,
            acknowledged = true,
            purchaseState = 1
        )
    }

    fun createPendingPurchase(
        purchaseToken: String = "test.purchase.token.pending",
        orderId: String = "GPA.4444-5555-6666-77777"
    ): GooglePurchase =
        createLumoPlus12MonthPurchase(
            purchaseToken = purchaseToken,
            orderId = orderId,
            isAutoRenewing = false,
            acknowledged = false,
            purchaseState = 0 // PENDING
        )

    fun createMismatchedPurchase(
        purchaseToken: String = "test.purchase.token.mismatch",
        orderId: String = "GPA.8888-9999-0000-11111",
        obfuscatedAccountId: String = "differentCustomer789"
    ): GooglePurchase =
        createLumoPlus12MonthPurchase(
            purchaseToken = purchaseToken,
            orderId = orderId,
            obfuscatedAccountId = obfuscatedAccountId,
            isAutoRenewing = true,
            acknowledged = true
        )
}
