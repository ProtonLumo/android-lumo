package me.proton.android.lumo.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun appPrefers(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences(
            "lumo_prefs",
            Context.MODE_PRIVATE
        )
}