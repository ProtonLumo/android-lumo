package me.proton.android.lumo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import me.proton.android.lumo.sentry.initialise
import timber.log.Timber

@HiltAndroidApp
class LumoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        this.initialise()
    }
}
