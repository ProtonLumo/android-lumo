package me.proton.android.lumo.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.proton.android.lumo.ActivityProvider
import me.proton.android.lumo.DefaultActivityProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun appPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences(
            "lumo_prefs",
            Context.MODE_PRIVATE
        )

    @Provides
    @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("lumo_prefs") }
        )

    @Provides
    @Singleton
    fun activityProvider(): ActivityProvider =
        DefaultActivityProvider()
}
