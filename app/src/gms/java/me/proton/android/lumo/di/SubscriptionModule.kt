package me.proton.android.lumo.di

import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.android.lumo.billing.BillingManager
import me.proton.android.lumo.data.mapper.PaymentTokenMapper
import me.proton.android.lumo.data.mapper.PlanMapper
import me.proton.android.lumo.data.mapper.SubscriptionMapper
import me.proton.android.lumo.money_machine.BillingBackend
import me.proton.android.lumo.money_machine.BillingStore
import me.proton.android.lumo.money_machine.ConnectionState
import me.proton.android.lumo.money_machine.JsBillingBackend
import me.proton.android.lumo.usecase.IsPaymentAvailableUseCase
import me.proton.android.lumo.webview.WebAppWithPaymentsInterface
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SubscriptionModule {

    @Provides
    @Singleton
    fun webAppWithPaymentsInterface(): WebAppWithPaymentsInterface =
        WebAppWithPaymentsInterface()

    @Provides
    @Singleton
    fun subscriptionMapper(): SubscriptionMapper = SubscriptionMapper

    @Provides
    @Singleton
    fun planMapper(): PlanMapper = PlanMapper

    @Provides
    @Singleton
    fun paymentTokenMapper(): PaymentTokenMapper =
        PaymentTokenMapper()

    @Provides
    @Singleton
    fun billingBackend(
        webBridge: WebAppWithPaymentsInterface,
        paymentTokenMapper: PaymentTokenMapper,
        subscriptionMapper: SubscriptionMapper,
        planMapper: PlanMapper,
    ): BillingBackend =
        JsBillingBackend(
            webBridge = webBridge,
            paymentTokenMapper = paymentTokenMapper,
            subscriptionMapper = subscriptionMapper,
            planMapper = planMapper,
        )

    @Provides
    @Singleton
    fun isPaymentAvailable(billingStore: BillingStore): IsPaymentAvailableUseCase =
        object : IsPaymentAvailableUseCase {
            override fun invoke(): Boolean =
                billingStore.state.value.connection != ConnectionState.Unavailable
        }
}
