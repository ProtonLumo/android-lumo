package me.proton.android.lumo.models

import me.proton.android.lumo.LumoBillingClientImpl.Companion.MONTHLY_PLAN
import me.proton.android.lumo.LumoBillingClientImpl.Companion.YEARLY_PLAN
import me.proton.android.lumo.ui.text.UiText

/**
 * Test fixtures for JsPlanInfo objects based on actual API responses.
 * Data extracted from PaymentJsResponse for Lumo Plus plans.
 */
object JsPlanInfoFixtures {

    const val LUMO_PLUS_PLAN_ID =
        "1e_hLDsjEBCzrXfR6u6QT5MXbIzmOVuIhlSEcsF88t5Q7GXF66gxo-V8vzddFjRuL-3a1yGPm4nR3rsxQdD5kw=="
    const val GOOGLE_CUSTOMER_ID = "cus_google_Xt13wHExcJCxvsoAEvxA"

    fun plansOptions() =
        listOf(
            createLumoPlusMonthlyPlan(),
            createLumoPlusYearlyPlan()
        )

    fun createLumoPlusMonthlyPlan(
        offerToken: String? = null
    ): JsPlanInfo = createPlan(
        productId = MONTHLY_PLAN.productId,
        offerToken = offerToken
    )

    fun createLumoPlusYearlyPlan(
        offerToken: String? = null
    ): JsPlanInfo = createPlan(
        productId = YEARLY_PLAN.productId,
        offerToken = offerToken
    )

    fun createPlan(
        productId: String,
        offerToken: String? = null
    ): JsPlanInfo = JsPlanInfo(
        id = LUMO_PLUS_PLAN_ID,
        name = "Lumo Plus",
        duration = UiText.StringText(if (productId == YEARLY_PLAN.productId) "$12 months" else "1 month"),
        cycle = if (productId == YEARLY_PLAN.productId) 12 else 1,
        description = if (productId == YEARLY_PLAN.productId) "Per year" else "Per month",
        productId = productId,
        customerId = GOOGLE_CUSTOMER_ID,
        pricePerMonth = "",
        totalPrice = "",
        savings = null,
        offerToken = offerToken
    )
}
