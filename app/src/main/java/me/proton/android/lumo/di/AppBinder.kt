package me.proton.android.lumo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.android.lumo.data.repository.ThemeRepository
import me.proton.android.lumo.data.repository.ThemeRepositoryImpl
import me.proton.android.lumo.data.repository.WebAppRepository
import me.proton.android.lumo.data.repository.WebAppRepositoryImpl
import me.proton.android.lumo.featureflag.FeatureGatekeeper
import me.proton.android.lumo.featureflag.FeatureGatekeeperImpl
import me.proton.android.lumo.featureflag.datasource.LegacyFeatureFlagDataSource
import me.proton.android.lumo.featureflag.datasource.LegacyFeatureFlagDataSourceImpl
import me.proton.android.lumo.featureflag.datasource.UnleashDataSource
import me.proton.android.lumo.featureflag.datasource.UnleashDataSourceImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBinder {

    @Binds
    abstract fun themeRepository(impl: ThemeRepositoryImpl): ThemeRepository

    @Binds
    abstract fun webAppRepository(impl: WebAppRepositoryImpl): WebAppRepository

    @Binds
    abstract fun unleashDataSource(impl: UnleashDataSourceImpl): UnleashDataSource

    @Binds
    abstract fun legacyFeatureFlagDataSource(impl: LegacyFeatureFlagDataSourceImpl): LegacyFeatureFlagDataSource

    @Binds
    abstract fun featureGatekeeper(impl: FeatureGatekeeperImpl): FeatureGatekeeper
}