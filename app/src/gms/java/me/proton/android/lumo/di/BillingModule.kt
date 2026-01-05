package me.proton.android.lumo.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.proton.android.lumo.ActivityProvider
import me.proton.android.lumo.LumoBillingClient
import me.proton.android.lumo.LumoBillingClientImpl
import me.proton.android.lumo.billing.BillingBackend
import me.proton.android.lumo.billing.BillingEffectHandler
import me.proton.android.lumo.billing.BillingStore
import me.proton.android.lumo.billing.ConnectionState
import me.proton.android.lumo.billing.DefaultBillingStore
import me.proton.android.lumo.billing.JsBillingBackend
import me.proton.android.lumo.data.mapper.PaymentTokenMapper
import me.proton.android.lumo.data.mapper.PlanMapper
import me.proton.android.lumo.data.mapper.SubscriptionMapper
import me.proton.android.lumo.usecase.IsPaymentAvailableUseCase
import me.proton.android.lumo.webview.WebAppWithPaymentsInterface
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {

    @Provides
    @Singleton
    fun billingStore(
        scope: CoroutineScope,
    ): BillingStore =
        DefaultBillingStore(scope)

    @Provides
    @Singleton
    fun lumoBillingClient(
        @ApplicationContext context: Context,
        activityProvider: ActivityProvider,
    ): LumoBillingClient =
        LumoBillingClientImpl(
            context = context,
            activityProvider = activityProvider,
        )

    @Provides
    @Singleton
    fun billingEffectHandler(
        backend: BillingBackend,
        billingClient: LumoBillingClient,
        billingStore: BillingStore,
        scope: CoroutineScope
    ): BillingEffectHandler =
        BillingEffectHandler(
            backend = backend,
            scope = scope,
            billingClient = billingClient,
            dispatch = billingStore::dispatch
        ).also { handler ->
            scope.launch {
                billingStore.effects.collect { handler.handle(it) }
            }
        }

    @Provides
    @Singleton
    fun provideApplicationCoroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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
    fun isPaymentAvailable(billingStore: BillingStore): IsPaymentAvailableUseCase =
        object : IsPaymentAvailableUseCase {
            override fun invoke(): Boolean =
                billingStore.state.value.connection != ConnectionState.Unavailable
        }
}
