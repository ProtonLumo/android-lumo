package me.proton.android.lumo.sentry

import android.content.Context
import androidx.startup.Initializer
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import me.proton.android.lumo.BuildConfig

class SentryInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        SentryAndroid.init(context.applicationContext) { options: SentryOptions ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.release = BuildConfig.VERSION_NAME
            options.isDebug = true
            options.isEnableUncaughtExceptionHandler = true
            options.setDiagnosticLevel(SentryLevel.WARNING)
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}