package me.proton.android.lumo.sentry

import android.content.Context
import androidx.startup.Initializer
import io.sentry.SentryLevel
import io.sentry.SentryLogLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.android.timber.SentryTimberIntegration
import me.proton.android.lumo.BuildConfig


class SentryInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        SentryAndroid.init(context.applicationContext) { options: SentryOptions ->
            with(options) {
                dsn = BuildConfig.SENTRY_DSN
                release = BuildConfig.VERSION_NAME
                isDebug = false
                environment = BuildConfig.BASE_DOMAIN
                isEnableUncaughtExceptionHandler = true
                setDiagnosticLevel(SentryLevel.DEBUG)
                tracesSampleRate = 0.2
                addIntegration(
                    SentryTimberIntegration(
                        minEventLevel = SentryLevel.ERROR,
                        minBreadcrumbLevel = SentryLevel.ERROR,
                        minLogsLevel = SentryLogLevel.ERROR
                    )
                )
            }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}