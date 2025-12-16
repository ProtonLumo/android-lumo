package me.proton.android.lumo.featureflag

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import me.proton.android.lumo.featureflag.datasource.LegacyFeatureFlagDataSource
import me.proton.android.lumo.featureflag.datasource.UnleashDataSource
import me.proton.android.lumo.featureflag.model.FeatureFlag
import me.proton.android.lumo.featureflag.model.FeatureId
import me.proton.android.lumo.featureflag.model.Scope
import javax.inject.Inject

interface FeatureGatekeeper {
    fun start()
    fun getFeature(featureId: FeatureId): FeatureFlag
    fun observeFeature(featureId: FeatureId): Flow<FeatureFlag>
    suspend fun updateLegacyFeature(
        featureId: FeatureId,
        isEnabled: Boolean
    )
}

class FeatureGatekeeperImpl @Inject constructor(
    private val legacyFeatureFlagDataSource: LegacyFeatureFlagDataSource,
    private val unleashDataSource: UnleashDataSource,
) : FeatureGatekeeper {

    override fun start() {
        legacyFeatureFlagDataSource.start()
    }

    override fun getFeature(featureId: FeatureId): FeatureFlag =
        LumoFeatureFlags.flags[featureId]?.let { flag ->
            if (flag.scope == Scope.Unleash) {
                unleashDataSource.getFeatureFlag(featureId)
            } else {
                legacyFeatureFlagDataSource.get(featureId)
            }
        } ?: FeatureFlag.DEFAULT

    override fun observeFeature(featureId: FeatureId): Flow<FeatureFlag> =
        legacyFeatureFlagDataSource.onFeaturesChanged(featureId)


    override suspend fun updateLegacyFeature(
        featureId: FeatureId,
        isEnabled: Boolean
    ) {
        LumoFeatureFlags.flags[featureId]?.let { flag ->
            if (flag.scope == Scope.Unleash) {
                // ignore
            } else {
                legacyFeatureFlagDataSource.update(
                    featureId = featureId,
                    isEnabled = isEnabled
                )
            }
        }
    }
}