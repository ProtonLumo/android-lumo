package me.proton.android.lumo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.android.lumo.data.repository.ThemeRepository
import me.proton.android.lumo.data.repository.ThemeRepositoryImpl
import me.proton.android.lumo.data.repository.WebAppRepository
import me.proton.android.lumo.data.repository.WebAppRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBinder {

    @Binds
    abstract fun themeRepository(impl: ThemeRepositoryImpl): ThemeRepository

    @Binds
    abstract fun webAppRepository(impl: WebAppRepositoryImpl): WebAppRepository
}