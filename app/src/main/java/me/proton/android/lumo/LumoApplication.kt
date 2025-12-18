package me.proton.android.lumo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import me.proton.android.lumo.money_machine.BillingEffectHandler
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class LumoApplication : Application() {

    @Inject
    lateinit var billingEffectHandler: BillingEffectHandler

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        billingEffectHandler
    }
}