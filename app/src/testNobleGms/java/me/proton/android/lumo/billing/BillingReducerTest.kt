package me.proton.android.lumo.billing

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.lumo.data.SubscriptionResult
import me.proton.android.lumo.models.GooglePurchaseFixtures.createLumoPlus1MonthPurchase
import me.proton.android.lumo.models.JsPlanInfoFixtures.plansOptions
import me.proton.android.lumo.models.ProductDetailsFixtures.productDetails
import me.proton.android.lumo.models.SubscriptionResultFixtures.lumoPlusMonthly
import me.proton.android.lumo.ui.text.UiText
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingReducerTest {

    private val initialState = BillingState()

    @Test
    fun `When Initialize Then loading states and emits ConnectBilling`() {
        val (state, effects) = billingReducer(
            state = initialState,
            action = BillingAction.Initialize
        )

        assertThat(state.connection).isEqualTo(ConnectionState.Connecting)
        assertThat(state.subscriptionState).isEqualTo(SubscriptionState.Loading)
        assertThat(state.plansState).isEqualTo(PlansState.Loading)
        assertThat(state.paymentState).isEqualTo(PaymentState.Idle)
        assertThat(state.error).isNull()

        assertThat(effects).containsExactly(BillingEffect.ConnectBilling)
    }

    @Test
    fun `When RetryConnection Then connecting and emits ConnectBilling`() {
        val (state, effects) = billingReducer(
            state = initialState,
            action = BillingAction.RetryConnection
        )

        assertThat(state.connection).isEqualTo(ConnectionState.Connecting)

        assertThat(effects).containsExactly(BillingEffect.ConnectBilling)
    }

    @Test
    fun `When BillingConnected Then connected and emits queries`() {
        val (state, effects) = billingReducer(
            state = initialState,
            action = BillingAction.BillingConnected
        )

        assertThat(state.connection).isEqualTo(ConnectionState.Connected)

        assertThat(effects).containsExactly(
            BillingEffect.QueryProducts,
            BillingEffect.QueryPurchases
        )
    }

    @Test
    fun `When BillingDisconnected Then retries connection`() {
        val (state, effects) = billingReducer(
            state = initialState,
            action = BillingAction.BillingDisconnected(reason = null)
        )

        assertThat(state.connection).isEqualTo(ConnectionState.Connecting)

        assertThat(effects).containsExactly(BillingEffect.ConnectBilling)
    }

    @Test
    fun `When ProductDetailsLoaded Then updates plans state`() {
        val googleProducts = productDetails()
        val planOptions = plansOptions()

        val (state, effects) = billingReducer(
            state = initialState,
            action = BillingAction.ProductDetailsLoaded(
                googleProductDetails = googleProducts,
                products = emptyList(),
                planFeatures = emptyList(),
                planOptions = planOptions
            )
        )

        assertThat(state.plansState).isInstanceOf(PlansState.Success::class.java)
        val success = state.plansState as PlansState.Success
        assertThat(success.googleProductDetails).isEqualTo(googleProducts)
        assertThat(success.planOptions).isEqualTo(planOptions)

        assertThat(effects).isEmpty()
    }

    @Test
    fun `When PurchasesLoaded with no purchase and no subscription Then None`() {
        val result = SubscriptionResult(hasValidSubscription = false)

        val (state, effects) = billingReducer(
            state = initialState,
            action = BillingAction.PurchasesLoaded(
                purchase = null,
                subscriptionResult = result
            )
        )

        assertThat(state.subscriptionState).isEqualTo(SubscriptionState.None)

        assertThat(effects).isEmpty()
    }

    @Test
    fun `When PurchasesLoaded with active subscription Then Active`() {
        val result = lumoPlusMonthly()

        val (state, effects) = billingReducer(
            state = initialState,
            action = BillingAction.PurchasesLoaded(
                purchase = null,
                subscriptionResult = result
            )
        )

        assertThat(state.subscriptionState).isEqualTo(
            SubscriptionState.Active(subscriptions = result.subscriptions)
        )

        assertThat(effects).isEmpty()
    }

    @Test
    fun `When PurchasesLoaded with no active subscription and purchase Then Mismatch`() {
        val result = lumoPlusMonthly().copy(hasValidSubscription = false)
        val purchase = createLumoPlus1MonthPurchase()

        val (state, effects) = billingReducer(
            state = initialState,
            action = BillingAction.PurchasesLoaded(
                purchase = purchase,
                subscriptionResult = result
            )
        )

        assertThat(state.subscriptionState).isEqualTo(
            SubscriptionState.Mismatch(purchase)
        )

        assertThat(effects).isEmpty()
    }

    @Test
    fun `When SelectPlan Then updates selected index`() {
        val plansState = PlansState.Success(
            planOptions = plansOptions(),
            planFeatures = emptyList(),
            googleProductDetails = productDetails(),
            hasOffer = false,
            productDetails = emptyList(),
            selectedPlanIndex = 0
        )

        val (state, effects) = billingReducer(
            state = initialState.copy(plansState = plansState),
            action = BillingAction.SelectPlan(index = 1)
        )

        val updated = state.plansState as PlansState.Success
        assertThat(updated.selectedPlanIndex).isEqualTo(1)

        assertThat(effects).isEmpty()
    }

    @Test
    fun `When LaunchPurchase with missing product Then emits no effects`() {
        val (state, effects) = billingReducer(
            state = initialState,
            action = BillingAction.LaunchPurchase(
                productId = "unknown",
                offerToken = null,
                customerId = "id"
            )
        )

        assertThat(state).isEqualTo(initialState)
        assertThat(effects).isEmpty()
    }

    @Test
    fun `When BackendVerificationSucceeded Then resets payment state`() {
        val (state, _) = billingReducer(
            state = initialState.copy(
                paymentState = PaymentState.Verifying,
                customerId = "id"
            ),
            action = BillingAction.BackendVerificationSucceeded
        )

        assertThat(state.paymentState).isEqualTo(PaymentState.Success)
        assertThat(state.customerId).isEmpty()
        assertThat(state.error).isNull()
    }

    @Test
    fun `When BackendVerificationFailed Then error payment state`() {
        val error = UiText.StringText("error")

        val (state, _) = billingReducer(
            state = initialState,
            action = BillingAction.BackendVerificationFailed(error)
        )

        assertThat(state.paymentState).isEqualTo(
            PaymentState.Error(error)
        )
    }
}
