package me.proton.android.lumo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import me.proton.android.lumo.tracer.LumoTracer
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
        object : IsPaymentAvailableUseCase {
            override fun invoke(): Boolean = false
        }

    @Provides
    fun hasOffer() : HasOfferUseCase =
        object : HasOfferUseCase {
            override fun hasOffer(): Flow<Boolean> = flowOf(false)
        }

    @Provides
    fun getMainScreenTracer(): LumoTracer =
        object: LumoTracer {
            override fun startTransaction(name: String) {
            }

            override fun measureSpan(
                operation: LumoTracer.Operation,
                description: String
            ) {
            }

            override fun stopSpan(operation: LumoTracer.Operation) {
            }

            override fun finishTransaction() {
            }
        }
}