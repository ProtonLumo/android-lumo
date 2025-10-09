package me.proton.android.lumo.billing

import me.proton.android.lumo.di.DependencyProvider

object BillingDelegate {

    private lateinit var billingManagerWrapper: BillingManagerWrapper


    fun initialise() {
        billingManagerWrapper = DependencyProvider.getBillingManagerWrapper()
    }
}