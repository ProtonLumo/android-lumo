package me.proton.android.lumo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.android.lumo.sentry.tracer.Tracer
import me.proton.android.lumo.tracer.LumoTracer

@Module
@InstallIn(SingletonComponent::class)
object SentryModule {

    @Provides
    fun mainScreenTracer(): LumoTracer =
        Tracer(LumoTracer.Operation.LoadUi)
}