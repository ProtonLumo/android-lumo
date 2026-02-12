package me.proton.android.lumo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.android.lumo.webview.WebAppInterface
import me.proton.android.lumo.webview.WebAppWithPaymentsInterface
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SubscriptionBinder {

    @Binds
    @Singleton
    abstract fun webAppInterface(webAppInterface: WebAppWithPaymentsInterface): WebAppInterface

}