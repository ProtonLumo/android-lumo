package me.proton.android.lumo.models

import me.proton.android.lumo.data.SubscriptionResult

object SubscriptionResultFixtures {

    const val SUBSCRIPTION_ID =
        "eJzYEcgnpgE3dEknxozA9xTFNgsk5xsxnojAJEOe-X6OnmZktXx3KdZzRZzUjGg3G5c6DYUjGm_qZlt6X9nhTw=="

    const val INVOICE_ID =
        "hUcV0_EeNwUmXA6EoyNrtO-ZTD8H8F6LvNaSjMaPxB5ecFkA7y-5kc3q38cGumJENGHjtSoUndkYFUx0_xlJeg=="

    private const val TITLE = "Lumo Plus"
    private const val DESCRIPTION = "Current plan"
    private const val CURRENCY = "CHF"
    private const val PLAN_NAME = "lumo2024"

    fun lumoPlusMonthly(
        periodStart: Long = 1_766_582_992,
        periodEnd: Long = periodStart + MONTH_SECONDS,
        hasValidSubscription: Boolean = true,
        customerId: String? = null
    ): SubscriptionResult =
        SubscriptionResult(
            subscriptions = listOf(
                subscriptionItem(
                    cycle = 1,
                    cycleDescription = "For 1 month",
                    periodStart = periodStart,
                    periodEnd = periodEnd,
                    customerId = customerId
                )
            ),
            hasValidSubscription = hasValidSubscription,
            error = null
        )

    fun lumoPlusYearly(
        periodStart: Long = 1_766_582_992,
        periodEnd: Long = periodStart + YEAR_SECONDS,
        hasValidSubscription: Boolean = true,
        customerId: String? = null
    ): SubscriptionResult =
        SubscriptionResult(
            subscriptions = listOf(
                subscriptionItem(
                    cycle = 12,
                    cycleDescription = "For 12 months",
                    periodStart = periodStart,
                    periodEnd = periodEnd,
                    customerId = customerId
                )
            ),
            hasValidSubscription = hasValidSubscription,
            error = null
        )

    private fun subscriptionItem(
        id: String = SUBSCRIPTION_ID,
        invoiceId: String = INVOICE_ID,
        cycle: Int,
        cycleDescription: String,
        periodStart: Long,
        periodEnd: Long,
        createTime: Long = periodStart - 1,
        currency: String = CURRENCY,
        amount: Int = 0,
        renew: Int = 1,
        billingPlatform: Int = 1,
        external: Int = 2,
        isTrial: Boolean = false,
        customerId: String? = null
    ): SubscriptionItemResponse =
        SubscriptionItemResponse(
            id = id,
            invoiceId = invoiceId,
            cycle = cycle,
            periodStart = periodStart,
            periodEnd = periodEnd,
            createTime = createTime,
            couponCode = null,
            currency = currency,
            amount = amount,
            discount = 0,
            renewDiscount = 0,
            renewAmount = 0,
            renew = renew,
            external = external,
            billingPlatform = billingPlatform,
            isTrial = isTrial,
            customerID = customerId,
            title = TITLE,
            description = DESCRIPTION,
            name = PLAN_NAME,
            cycleDescription = cycleDescription,
            offer = "default",
            entitlements = emptyList(),
            decorations = emptyList()
        )

    private const val DAY_SECONDS = 86_400L
    private const val MONTH_SECONDS = 30 * DAY_SECONDS
    private const val YEAR_SECONDS = 365 * DAY_SECONDS
}
