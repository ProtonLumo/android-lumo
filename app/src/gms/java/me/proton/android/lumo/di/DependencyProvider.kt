package me.proton.android.lumo.di

import android.annotation.SuppressLint
import android.app.Application
import me.proton.android.lumo.billing.BillingManagerWrapper
import me.proton.android.lumo.webview.WebAppInterface
import me.proton.android.lumo.webview.WebAppWithPaymentsInterface

/**
 * Simple dependency provider that replaces the factory pattern
 * This provides a clean way to manage dependencies without the complexity of full DI frameworks
 */
object DependencyProvider : BaseDependencyProvider() {

    private var billingManagerWrapper: BillingManagerWrapper? = null
    @SuppressLint("StaticFieldLeak")
    private var webBridge: WebAppWithPaymentsInterface? = null

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
}
