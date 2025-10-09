package me.proton.android.lumo

import android.app.Application
import me.proton.android.lumo.billing.BillingDelegate
import me.proton.android.lumo.di.DependencyProvider

class LumoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        DependencyProvider.initialise(this)
        BillingDelegate.initialise()
    }
}