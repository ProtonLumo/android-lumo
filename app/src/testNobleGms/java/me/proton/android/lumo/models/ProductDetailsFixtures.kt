package me.proton.android.lumo.models

import me.proton.android.lumo.LumoBillingClientImpl.Companion.MONTHLY_PLAN
import me.proton.android.lumo.LumoBillingClientImpl.Companion.YEARLY_PLAN
import me.proton.android.lumo.billing.GoogleProductDetails

object ProductDetailsFixtures {

    fun productDetails() =
        listOf(
            createLumoPlus12MonthProductDetails(),
            createLumoPlus1MonthProductDetails()
        )

    fun createLumoPlus12MonthProductDetails(
        priceAmountMicros: Long = 127080000,
        priceCurrencyCode: String = "CHF",
        formattedPrice: String = "CHF 127.08",
        billingPeriod: String = "P1Y",
        title: String = "Lumo Plus (12 months) (Lumo by Proton: IAP)",
        name: String = "Lumo Plus (12 months)",
        description: String = ""
    ): GoogleProductDetails {
        return GoogleProductDetails(
            productId = YEARLY_PLAN.productId,
            productType = "subs",
            title = title,
            name = name,
            description = description,
            subscriptionOfferDetails = listOf(
                GoogleProductDetails.GoogleSubscriptionOfferDetails(
                    offerToken = "ATdH1CNtEF3XBRxFpkJ0jCGS0X1YNPZHj/OKlnf6Xtl1/sdcuFEopVernEhLAvCoSTHjzi3A0qCMH6MHlBqaLLyCpmuUw8brbF1jt6mHHJHp/44q7V0BbszZVA==",
                    basePlanId = "giaplumo-lumo2025-12-renewing",
                    pricingPhases = listOf(
                        GoogleProductDetails.GooglePricingPhase(
                            priceAmountMicros = priceAmountMicros,
                            priceCurrencyCode = priceCurrencyCode,
                            formattedPrice = formattedPrice,
                            billingPeriod = billingPeriod,
                            recurrenceMode = 1
                        )
                    ),
                    offerTags = emptyList(),
                    offerId = null,
                )
            )
        )
    }

    fun createLumoPlus1MonthProductDetails(
        priceAmountMicros: Long = 13790000,
        priceCurrencyCode: String = "CHF",
        formattedPrice: String = "CHF 13.79",
        billingPeriod: String = "P1M",
        title: String = "Lumo Plus (1 month) (Lumo by Proton: IAP)",
        name: String = "Lumo Plus (1 month)",
        description: String = ""
    ): GoogleProductDetails {
        return GoogleProductDetails(
            productId = MONTHLY_PLAN.productId,
            productType = "subs",
            title = title,
            name = name,
            description = description,
            subscriptionOfferDetails = listOf(
                GoogleProductDetails.GoogleSubscriptionOfferDetails(
                    offerToken = "ATdH1CO+E3HhNGeEngJprj2dJItH5gYnx8flGR+tmHMXPxsjFuiDKvKVWEG6XVEZXhbtJcnT9zruI5wn6LdApe5PYq9ZQX7V10upSezo+9QxvSeU9o9ECWpJmw==",
                    basePlanId = "giaplumo-lumo2025-1-renewing",
                    pricingPhases = listOf(
                        GoogleProductDetails.GooglePricingPhase(
                            priceAmountMicros = priceAmountMicros,
                            priceCurrencyCode = priceCurrencyCode,
                            formattedPrice = formattedPrice,
                            billingPeriod = billingPeriod,
                            recurrenceMode = 1
                        )
                    ),
                    offerTags = emptyList(),
                    offerId = null
                )
            )
        )
    }

    fun createAllProductDetails(): List<GoogleProductDetails> {
        return listOf(
            createLumoPlus1MonthProductDetails(),
            createLumoPlus12MonthProductDetails()
        )
    }

    fun createProductDetailsWithDifferentCurrency(
        currency: String = "USD",
        monthlyPrice: Long = 14990000, // $14.99
        monthlyFormattedPrice: String = "$14.99",
        yearlyPrice: Long = 139990000, // $139.99
        yearlyFormattedPrice: String = "$139.99"
    ): List<GoogleProductDetails> {
        return listOf(
            createLumoPlus1MonthProductDetails(
                priceAmountMicros = monthlyPrice,
                priceCurrencyCode = currency,
                formattedPrice = monthlyFormattedPrice
            ),
            createLumoPlus12MonthProductDetails(
                priceAmountMicros = yearlyPrice,
                priceCurrencyCode = currency,
                formattedPrice = yearlyFormattedPrice
            )
        )
    }
}
