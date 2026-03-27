package me.proton.android.lumo.featureflag

import kotlinx.coroutines.flow.Flow
import me.proton.android.lumo.featureflag.datasource.LegacyFeatureFlagDataSource
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
) : FeatureGatekeeper {

    override fun start() {
//        legacyFeatureFlagDataSource.start() todo; keep disabled til further notice
    }

    @Suppress("ForbiddenComment")
    override fun getFeature(featureId: FeatureId): FeatureFlag =
        LumoFeatureFlags.flags[featureId]?.let { flag ->
            if (flag.scope == Scope.Unleash) {
                // TODO: fetch the ff
                FeatureFlag.DEFAULT
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
