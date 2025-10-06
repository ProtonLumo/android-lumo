package me.proton.android.lumo.billing

import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.di.DependencyProvider

class BillingDelegateImpl() : BillingDelegate {

    private lateinit var billingManagerWrapper: BillingManagerWrapper

    override fun initialise(activity: MainActivity) {
        billingManagerWrapper = DependencyProvider.getBillingManagerWrapper()
        billingManagerWrapper.initializeBilling(activity)
    }
}