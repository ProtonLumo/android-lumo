package me.proton.android.lumo.models

import com.android.billingclient.api.Purchase
import me.proton.android.lumo.billing.BillingEffectHandler.Companion.MONTHLY_PLAN
import me.proton.android.lumo.billing.BillingEffectHandler.Companion.YEARLY_PLAN

object PurchaseFixtures {

    fun createLumoPlus12MonthPurchase(
        purchaseToken: String = "test.purchase.token.lumo12month",
        orderId: String = "GPA.1234-5678-9012-34567",
        purchaseTime: Long = System.currentTimeMillis(),
        expiryTime: Long = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000), // 1 year from now
        isAutoRenewing: Boolean = true,
        acknowledged: Boolean = true,
        purchaseState: Int = 1, // 1 = PURCHASED, 0 = PENDING
        obfuscatedAccountId: String? = "customer123"
    ): Purchase {
        val jsonString = """
        {
            "orderId": "$orderId",
            "packageName": "me.proton.lumo",
            "productId": ${YEARLY_PLAN.productId},
            "purchaseTime": $purchaseTime,
            "purchaseState": $purchaseState,
            "purchaseToken": "$purchaseToken",
            "quantity": 1,
            "autoRenewing": $isAutoRenewing,
            "acknowledged": $acknowledged,
            "expiryTimeMillis": $expiryTime,
            ${if (obfuscatedAccountId != null) "\"obfuscatedAccountId\": \"$obfuscatedAccountId\"," else ""}
            "developerPayload": ""
        }
        """.trimIndent()

        return Purchase(jsonString, "")
    }

    fun createLumoPlus1MonthPurchase(
        purchaseToken: String = "test.purchase.token.lumo1month",
        orderId: String = "GPA.9876-5432-1098-76543",
        purchaseTime: Long = System.currentTimeMillis(),
        expiryTime: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // 30 days from now
        isAutoRenewing: Boolean = true,
        acknowledged: Boolean = true,
        purchaseState: Int = 1,
        obfuscatedAccountId: String? = "customer456"
    ): Purchase {
        val jsonString = """
        {
            "orderId": "$orderId",
            "packageName": "me.proton.lumo",
            "productId": ${MONTHLY_PLAN.productId},
            "purchaseTime": $purchaseTime,
            "purchaseState": $purchaseState,
            "purchaseToken": "$purchaseToken",
            "quantity": 1,
            "autoRenewing": $isAutoRenewing,
            "acknowledged": $acknowledged,
            "expiryTimeMillis": $expiryTime,
            ${if (obfuscatedAccountId != null) "\"obfuscatedAccountId\": \"$obfuscatedAccountId\"," else ""}
            "developerPayload": ""
        }
        """.trimIndent()

        return Purchase(jsonString, "")
    }

    fun createExpiredPurchase(
        purchaseToken: String = "test.purchase.token.expired",
        orderId: String = "GPA.0000-1111-2222-33333",
        isAutoRenewing: Boolean = false
    ): Purchase {
        val purchaseTime = System.currentTimeMillis() - (400L * 24 * 60 * 60 * 1000) // 400 days ago
        val expiryTime =
            System.currentTimeMillis() - (35L * 24 * 60 * 60 * 1000) // Expired 35 days ago

        return createLumoPlus12MonthPurchase(
            purchaseToken = purchaseToken,
            orderId = orderId,
            purchaseTime = purchaseTime,
            expiryTime = expiryTime,
            isAutoRenewing = isAutoRenewing,
            acknowledged = true,
            purchaseState = 1
        )
    }

    fun createPendingPurchase(
        purchaseToken: String = "test.purchase.token.pending",
        orderId: String = "GPA.4444-5555-6666-77777"
    ): Purchase {
        val purchaseTime = System.currentTimeMillis()
        val expiryTime = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)

        return createLumoPlus12MonthPurchase(
            purchaseToken = purchaseToken,
            orderId = orderId,
            purchaseTime = purchaseTime,
            expiryTime = expiryTime,
            isAutoRenewing = false,
            acknowledged = false,
            purchaseState = 0 // PENDING
        )
    }

    fun createMismatchedPurchase(
        purchaseToken: String = "test.purchase.token.mismatch",
        orderId: String = "GPA.8888-9999-0000-11111",
        obfuscatedAccountId: String = "differentCustomer789"
    ): Purchase {
        return createLumoPlus12MonthPurchase(
            purchaseToken = purchaseToken,
            orderId = orderId,
            obfuscatedAccountId = obfuscatedAccountId,
            isAutoRenewing = true,
            acknowledged = true
        )
    }
}