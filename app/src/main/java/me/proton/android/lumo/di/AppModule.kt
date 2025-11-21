package me.proton.android.lumo.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.proton.android.lumo.data.repository.ThemeRepository
import me.proton.android.lumo.data.repository.ThemeRepositoryImpl
import me.proton.android.lumo.data.repository.WebAppRepository
import me.proton.android.lumo.data.repository.WebAppRepositoryImpl
import me.proton.android.lumo.sentry.tracer.Tracer
import me.proton.android.lumo.tracer.LumoTracer
import me.proton.android.lumo.usecase.HasOffer
import me.proton.android.lumo.usecase.HasOfferUseCase

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun mainScreenTracer(): LumoTracer =
        Tracer(LumoTracer.Operation.LoadUi)

    @Provides
    fun appPrefers(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences(
            "lumo_prefs",
            Context.MODE_PRIVATE
        )
}