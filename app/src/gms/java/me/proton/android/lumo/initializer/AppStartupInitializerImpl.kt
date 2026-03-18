package me.proton.android.lumo.initializer

import android.content.Context
import me.proton.android.lumo.sentry.initialiseSentry

class AppStartupInitializerImpl : AppStartupInitializer {
    override fun initialize(context: Context) {
        context.initialiseSentry()
    }
}
