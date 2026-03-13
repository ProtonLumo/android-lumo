package me.proton.android.lumo.sentry

import android.content.Context
import io.sentry.SentryLevel
import io.sentry.SentryLogLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.android.timber.SentryTimberIntegration
import me.proton.android.lumo.BuildConfig


fun Context.initialise() {
    SentryAndroid.init(this.applicationContext) { options: SentryOptions ->
        with(options) {
            dsn = BuildConfig.SENTRY_DSN
            release = BuildConfig.VERSION_NAME
            isDebug = true
            environment = BuildConfig.BASE_DOMAIN
            isEnableUncaughtExceptionHandler = true
            setDiagnosticLevel(SentryLevel.ERROR)
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