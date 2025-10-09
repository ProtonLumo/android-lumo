package me.proton.android.lumo.di

import android.app.Application
import me.proton.android.lumo.billing.BillingManagerWrapper

/**
 * Simple dependency provider that replaces the factory pattern
 * This provides a clean way to manage dependencies without the complexity of full DI frameworks
 */
object DependencyProvider {

    lateinit var application: Application

    private var billingManagerWrapper: BillingManagerWrapper? = null

    fun initialise(application: Application) {
        this.application = application
    }

    /**
     * Get or create the BillingManagerWrapper instance
     */
    fun getBillingManagerWrapper(): BillingManagerWrapper {
        return billingManagerWrapper ?: BillingManagerWrapper(application).also {
            billingManagerWrapper = it
        }
    }
}
