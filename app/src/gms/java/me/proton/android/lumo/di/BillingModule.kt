package me.proton.android.lumo.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.proton.android.lumo.money_machine.ActivityProvider
import me.proton.android.lumo.money_machine.BillingBackend
import me.proton.android.lumo.money_machine.BillingEffectHandler
import me.proton.android.lumo.money_machine.BillingStore
import me.proton.android.lumo.money_machine.DefaultActivityProvider
import me.proton.android.lumo.money_machine.DefaultBillingStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {

    @Provides
    @Singleton
    fun billingStore(
        scope: CoroutineScope
    ): BillingStore =
        DefaultBillingStore(scope)

    @Provides
    @Singleton
    fun billingEffectHandler(
        @ApplicationContext context: Context,
        activityProvider: ActivityProvider,
        backend: BillingBackend,
        billingStore: BillingStore,
        scope: CoroutineScope
    ): BillingEffectHandler =
        BillingEffectHandler(
            context = context,
            activityProvider = activityProvider,
            backend = backend,
            scope = scope,
            dispatch = billingStore::dispatch
        ).also { handler ->
            scope.launch {
                billingStore.effects.collect { handler.handle(it) }
            }
        }

    @Provides
    @Singleton
    fun provideApplicationCoroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Provides
    @Singleton
    fun activityProvider(): ActivityProvider =
        DefaultActivityProvider()
}
