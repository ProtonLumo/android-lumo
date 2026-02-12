package me.proton.android.lumo.billing

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import me.proton.android.lumo.BuildConfig
import me.proton.android.lumo.data.PlanResult
import me.proton.android.lumo.data.SubscriptionResult
import me.proton.android.lumo.models.GooglePurchaseFixtures.createLumoPlus1MonthPurchase
import me.proton.android.lumo.models.JsPlanInfoFixtures.plansOptions
import me.proton.android.lumo.models.PaymentTokenPayload
import me.proton.android.lumo.models.ProductDetailsFixtures.productDetails
import me.proton.android.lumo.models.SubscriptionResultFixtures.lumoPlusMonthly
import me.proton.android.lumo.ui.text.UiText
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingEffectHandlerTest {

    private val backend = TestBillingBackend()
    private val billingClient = TestBillingClient()
    private val testDispatcher = StandardTestDispatcher()
    private val testScope: TestScope = TestScope(testDispatcher)
    private val dispatchedActions = mutableListOf<BillingAction>()
    private lateinit var effectHandler: BillingEffectHandler

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        effectHandler = BillingEffectHandler(
            backend = backend,
            billingClient = billingClient,
            scope = testScope,
            dispatch = { action -> dispatchedActions.add(action) })
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `When ConnectBilling Then connected successfully`() {
        billingClient.connected = true
        effectHandler.handle(BillingEffect.ConnectBilling)

        assertThat(dispatchedActions).containsExactly(BillingAction.BillingConnected)
    }

    @Test
    fun `When ConnectBilling Then disconnected`() {
        effectHandler.handle(BillingEffect.ConnectBilling)

        assertThat(dispatchedActions).containsExactly(
            BillingAction.BillingDisconnected(
                reason = null,
                isBillingAvailable = false
            )
        )
    }

    @Test
    fun `Given fetch plans returns error When QueryProducts Then error response`() {
        val errorMessage = UiText.StringText("error")
        backend.planResult = PlanResult(error = errorMessage)

        effectHandler.handle(BillingEffect.QueryProducts)
        testScope.runCurrent()

        assertThat(dispatchedActions).containsExactly(BillingAction.Error(errorMessage))
    }

    @Test
    fun `Given fetch plans empty And billing api error When QueryProducts Then error response`() {
        val errorMessage = UiText.StringText("null products")
        backend.planResult = PlanResult()

        effectHandler.handle(BillingEffect.QueryProducts)
        testScope.runCurrent()

        assertThat(dispatchedActions).containsExactly(BillingAction.Error(errorMessage))
    }

    @Test
    fun `Given valid plans And billing api success When QueryProducts Then plans emitted`() {
        val googleProductDetails = productDetails()
        billingClient.googleProductDetails = googleProductDetails
        val planOptions = plansOptions()
        backend.planResult = PlanResult(planOptions = planOptions)
        val updatedPlans = updatePlanPricing(
            plans = planOptions,
            productDetails = googleProductDetails,
            offerId = BuildConfig.OFFER_ID
        )

        effectHandler.handle(BillingEffect.QueryProducts)
        testScope.runCurrent()

        assertThat(dispatchedActions).containsExactly(
            BillingAction.ProductDetailsLoaded(
                googleProductDetails = googleProductDetails,
                products = emptyList(),
                planFeatures = emptyList(),
                planOptions = updatedPlans,
            )
        )
    }

    @Test
    fun `Given fetch subscriptions error When QueryPurchases Then error response`() {
        val error = UiText.StringText("error")
        backend.subscriptionResult = SubscriptionResult(error = error)

        effectHandler.handle(BillingEffect.QueryPurchases)
        testScope.runCurrent()

        assertThat(dispatchedActions).containsExactly(BillingAction.Error(error))
    }

    @Test
    fun `Given fetch subscription empty And billing api error When QueryPurchases Then error response`() {
        backend.subscriptionResult = SubscriptionResult()

        effectHandler.handle(BillingEffect.QueryPurchases)
        testScope.runCurrent()

        assertThat(dispatchedActions).containsExactly(
            BillingAction.Error(
                UiText.StringText("no purchases")
            )
        )
    }

    @Test
    fun `Given fetch subscription And billing api When QueryPurchases Then Purchases loaded`() {
        val subscriptionResult = lumoPlusMonthly()
        backend.subscriptionResult = subscriptionResult
        val purchase = createLumoPlus1MonthPurchase()
        billingClient.purchase = purchase

        effectHandler.handle(BillingEffect.QueryPurchases)
        testScope.runCurrent()

        assertThat(dispatchedActions).containsExactly(
            BillingAction.PurchasesLoaded(
                purchase = purchase,
                renewing = true,
                expiry = 0L,
                subscriptionResult = subscriptionResult,
            )
        )
    }

    @Test
    fun `Given acknowledge error When AcknowledgePurchase Then error response`() {
        billingClient.acknowledgeSuccess = false

        effectHandler.handle(BillingEffect.AcknowledgePurchase("token"))

        assertThat(dispatchedActions).containsExactly(
            BillingAction.Error(
                UiText.StringText("unable to ack")
            )
        )
    }

    @Test
    fun `Given acknowledge success When AcknowledgePurchase Then no response`() {
        billingClient.acknowledgeSuccess = true

        effectHandler.handle(BillingEffect.AcknowledgePurchase("token"))

        assertThat(dispatchedActions).isEmpty()
    }

    @Test
    fun `Given error When SendPaymentToken Then error response`() {
        val error = UiText.StringText("error")
        backend.backendResult = BackendResult.Failure(error)

        effectHandler.handle(
            BillingEffect.SendPaymentToken(
                payload = PaymentTokenPayload(
                    amount = 0,
                    currency = "CHF",
                )
            )
        )
        testScope.runCurrent()

        assertThat(dispatchedActions).containsExactly(
            BillingAction.BackendVerificationFailed(error)
        )
    }

    @Test
    fun `Given success When SendPaymentToken Then success response`() {
        backend.backendResult = BackendResult.Success

        effectHandler.handle(
            BillingEffect.SendPaymentToken(
                payload = PaymentTokenPayload(
                    amount = 0,
                    currency = "CHF",
                )
            )
        )
        testScope.runCurrent()

        assertThat(dispatchedActions).containsExactly(
            BillingAction.BackendVerificationSucceeded
        )
    }

    @Test
    fun `Given invalid customer id When LaunchBillingFlow Then error response`() {
        effectHandler.handle(
            BillingEffect.LaunchBillingFlow(
                productDetails = mockk(),
                offerToken = null,
                customerId = null,
            )
        )

        assertThat(dispatchedActions).containsExactly(
            BillingAction.Error(
                UiText.StringText("No active screen to launch billing")
            )
        )
    }

    @Test
    fun `Given valid customer id When LaunchBillingFlow Then empty response`() {
        effectHandler.handle(
            BillingEffect.LaunchBillingFlow(
                productDetails = mockk(),
                offerToken = null,
                customerId = "customerId",
            )
        )

        assertThat(dispatchedActions).isEmpty()
    }
}
