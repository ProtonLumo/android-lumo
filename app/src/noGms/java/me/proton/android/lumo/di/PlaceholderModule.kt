package me.proton.android.lumo.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import me.proton.android.lumo.analytics.DefaultLumoAnalytics
import me.proton.android.lumo.analytics.LumoAnalytics
import me.proton.android.lumo.initializer.AppStartupInitializer
import me.proton.android.lumo.review.DefaultInAppReviewManager
import me.proton.android.lumo.review.InAppReviewManager
import me.proton.android.lumo.tracer.LumoTracer
import me.proton.android.lumo.usecase.IsPaymentAvailableUseCase
import me.proton.android.lumo.webview.WebAppInterface
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaceholderModule {

    @Provides
    @Singleton
    fun webAppInterface(): WebAppInterface =
        WebAppInterface()

    @Provides
    fun isPaymentAvailable(): IsPaymentAvailableUseCase =
        IsPaymentAvailableUseCase { false }

    @Provides
    fun getMainAnalytics(): LumoAnalytics =
        DefaultLumoAnalytics()

    @Provides
    fun reviewManager(): InAppReviewManager =
        DefaultInAppReviewManager()

    @Provides
    fun appStartupInitializer(): AppStartupInitializer =
        object : AppStartupInitializer {
            @Suppress("EmptyFunctionBlock")
            override fun initialize(context: Context) {
            }
        }
}
