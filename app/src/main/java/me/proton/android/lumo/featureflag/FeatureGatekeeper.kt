package me.proton.android.lumo.featureflag

import me.proton.android.lumo.featureflag.datasource.LegacyFeatureFlagDataSource
import me.proton.android.lumo.featureflag.datasource.UnleashDataSource
import me.proton.android.lumo.featureflag.model.FeatureFlag
import me.proton.android.lumo.featureflag.model.FeatureId
import me.proton.android.lumo.featureflag.model.Scope
import timber.log.Timber
import javax.inject.Inject

interface FeatureGatekeeper {
    fun start()
    fun getFeature(featureId: FeatureId): FeatureFlag
}

class FeatureGatekeeperImpl @Inject constructor(
    private val legacyFeatureFlagDataSource: LegacyFeatureFlagDataSource,
    private val unleashDataSource: UnleashDataSource,
) : FeatureGatekeeper {

    override fun start() {
        Timber.tag("WTF").e("Starting feature gatekeeper")
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
}