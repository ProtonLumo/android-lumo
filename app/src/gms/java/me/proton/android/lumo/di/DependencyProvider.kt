package me.proton.android.lumo.di

import android.annotation.SuppressLint
import android.app.Application
import me.proton.android.lumo.billing.BillingManagerWrapper
import me.proton.android.lumo.data.repository.SubscriptionRepository
import me.proton.android.lumo.data.repository.SubscriptionRepositoryImpl
import me.proton.android.lumo.sentry.tracer.Tracer
import me.proton.android.lumo.tracer.LumoTracer
import me.proton.android.lumo.usecase.HasOffer
import me.proton.android.lumo.usecase.HasOfferUseCase
import me.proton.android.lumo.webview.WebAppWithPaymentsInterface

/**
 * Simple dependency provider that replaces the factory pattern
 * This provides a clean way to manage dependencies without the complexity of full DI frameworks
 */
object DependencyProvider : BaseDependencyProvider() {

    private var billingManagerWrapper: BillingManagerWrapper? = null

    @SuppressLint("StaticFieldLeak")
    private var webBridge: WebAppWithPaymentsInterface? = null
    private var hasOfferUseCase: HasOfferUseCase? = null
    private var subscriptionRepository: SubscriptionRepository? = null

    override fun initialise(application: Application) {
        super.initialise(application)
        billingManagerWrapper = getBillingManagerWrapper()
        webBridge = WebAppWithPaymentsInterface()
    }

    /**
     * Get or create the BillingManagerWrapper instance
     */
    fun getBillingManagerWrapper(): BillingManagerWrapper =
        billingManagerWrapper ?: BillingManagerWrapper(application).also {
            billingManagerWrapper = it
        }

    fun getWebBridge(): WebAppWithPaymentsInterface =
        webBridge ?: WebAppWithPaymentsInterface().also { webBridge = it }

    fun getSubscriptionRepository(): SubscriptionRepository =
        subscriptionRepository ?: SubscriptionRepositoryImpl(
            billingManager = getBillingManagerWrapper().getBillingManager(),
            webBridge = getWebBridge(),
        ).also { subscriptionRepository = it }

    override fun isPaymentAvailable(): Boolean =
        billingManagerWrapper?.getBillingManager() != null

    override fun hasOfferUseCase(): HasOfferUseCase =
        hasOfferUseCase ?: HasOffer(
            subscriptionRepository = getSubscriptionRepository()
        ).also { hasOfferUseCase = it }

    override fun getMainScreenTracer(): LumoTracer =
        Tracer(LumoTracer.Operation.LoadUi)
}
