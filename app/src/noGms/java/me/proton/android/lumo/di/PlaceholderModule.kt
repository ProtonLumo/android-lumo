package me.proton.android.lumo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import me.proton.android.lumo.analytics.DefaultLumoAnalytics
import me.proton.android.lumo.analytics.LumoAnalytics
import me.proton.android.lumo.review.DefaultInAppReviewManager
import me.proton.android.lumo.review.InAppReviewManager
import me.proton.android.lumo.usecase.HasOfferUseCase
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
    fun hasOffer(): HasOfferUseCase =
        object : HasOfferUseCase {
            override fun hasOffer(): Flow<Boolean> = flowOf(false)
        }

    @Provides
    fun getMainAnalytics(): LumoAnalytics =
        DefaultLumoAnalytics()

    @Provides
    fun reviewManager(): InAppReviewManager =
        DefaultInAppReviewManager()
}
