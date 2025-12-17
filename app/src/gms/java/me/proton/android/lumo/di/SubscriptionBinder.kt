package me.proton.android.lumo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.android.lumo.data.repository.SubscriptionRepository
import me.proton.android.lumo.data.repository.SubscriptionRepositoryImpl
import me.proton.android.lumo.usecase.HasOffer
import me.proton.android.lumo.usecase.HasOfferUseCase
import me.proton.android.lumo.webview.WebAppInterface
import me.proton.android.lumo.webview.WebAppWithPaymentsInterface
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SubscriptionBinder {

    @Binds
    @Singleton
    abstract fun subscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun webAppInterface(webAppInterface: WebAppWithPaymentsInterface): WebAppInterface

    @Binds
    @Singleton
    abstract fun hasOffer(impl: HasOffer): HasOfferUseCase
}