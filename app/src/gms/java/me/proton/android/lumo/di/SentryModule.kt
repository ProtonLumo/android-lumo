package me.proton.android.lumo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.android.lumo.analytics.LumoAnalytics
import me.proton.android.lumo.analytics.MainScreenAnalytics
import me.proton.android.lumo.initializer.AppStartupInitializer
import me.proton.android.lumo.initializer.AppStartupInitializerImpl
import me.proton.android.lumo.sentry.tracer.Tracer
import me.proton.android.lumo.tracer.LumoTracer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SentryModule {

    @Provides
    @Singleton
    fun mainScreenTracer(): LumoTracer =
        Tracer(LumoTracer.Operation.LoadUi)

    @Provides
    @Singleton
    fun mainScreenAnalytics(lumoTracer: LumoTracer): LumoAnalytics =
        MainScreenAnalytics(lumoTracer)

    @Provides
    @Singleton
    fun appStartupInitializer(): AppStartupInitializer =
        AppStartupInitializerImpl()
}