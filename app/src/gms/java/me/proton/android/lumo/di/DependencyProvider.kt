package me.proton.android.lumo.di

import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.billing.BillingManagerWrapper

/**
 * Simple dependency provider that replaces the factory pattern
 * This provides a clean way to manage dependencies without the complexity of full DI frameworks
 */
object DependencyProvider {

    private var billingManagerWrapper: BillingManagerWrapper? = null

    /**
     * Get or create the BillingManagerWrapper instance
     */
    fun getBillingManagerWrapper(activity: MainActivity): BillingManagerWrapper {
        return billingManagerWrapper ?: BillingManagerWrapper(activity).also {
            billingManagerWrapper = it
        }
    }
}
