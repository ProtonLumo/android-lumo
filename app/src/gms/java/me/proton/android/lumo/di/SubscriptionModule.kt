package me.proton.android.lumo.di

import android.content.Context
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.proton.android.lumo.billing.BillingManager
import me.proton.android.lumo.billing.BillingManagerWrapper
import me.proton.android.lumo.data.mapper.PaymentTokenMapper
import me.proton.android.lumo.data.mapper.PlanMapper
import me.proton.android.lumo.data.mapper.SubscriptionMapper
import me.proton.android.lumo.data.mapper.SubscriptionPurchaseHandler
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
    fun paymentTokenMapper(billingManager: BillingManager?): PaymentTokenMapper? =
        billingManager?.let { PaymentTokenMapper(billingManager = it) }

    @Provides
    @Singleton
    fun subscriptionPurchaseHandler(billingManager: BillingManager?): SubscriptionPurchaseHandler? =
        billingManager?.let { SubscriptionPurchaseHandler(billingManager = it) }

    @Provides
    @Singleton
    fun billingManagerWrapper(@ApplicationContext context: Context): BillingManagerWrapper =
        BillingManagerWrapper(context)

    @Provides
    @Singleton
    fun billingManager(billingManagerWrapper: BillingManagerWrapper): BillingManager? =
        billingManagerWrapper.getBillingManager()

    @Provides
    @Singleton
    fun isPaymentAvailable(billingManager: Lazy<BillingManager?>): IsPaymentAvailableUseCase =
        object : IsPaymentAvailableUseCase {
            override fun invoke(): Boolean = billingManager.get() != null
        }
}