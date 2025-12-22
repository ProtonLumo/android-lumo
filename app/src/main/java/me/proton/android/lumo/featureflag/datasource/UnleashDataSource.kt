package me.proton.android.lumo.featureflag.datasource

import io.getunleash.android.Unleash
import io.getunleash.android.data.Variant
import io.getunleash.android.events.UnleashReadyListener
import me.proton.android.lumo.featureflag.LumoFeatureFlags
import me.proton.android.lumo.featureflag.model.FeatureFlag
import me.proton.android.lumo.featureflag.model.FeatureId
import me.proton.android.lumo.featureflag.model.Scope
import javax.inject.Inject

interface UnleashDataSource {
    fun getFeatureFlag(featureId: FeatureId): FeatureFlag
    fun isReady(): Boolean
}

class UnleashDataSourceImpl @Inject constructor(
    private val unleash: Unleash,
) : UnleashDataSource {
    private var isReady = true // todo; keep disabled til further notice

    init {
        if (!isReady) {
            val readyListener = object : UnleashReadyListener {
                override fun onReady() {
                    isReady = true
                }
            }

            unleash.start(eventListeners = listOf(readyListener))
        }
    }

    override fun getFeatureFlag(featureId: FeatureId): FeatureFlag =
        with(unleash.getVariant(featureId.id)) {
            if (this == DISABLED) {
                FeatureFlag(
                    featureId = featureId,
                    scope = Scope.Unleash,
                    defaultValue = false,
                    value = unleash.isEnabled(featureId.id),
                    variantName = null,
                    payloadType = null,
                    payloadValue = null,
                ).also { LumoFeatureFlags.flags[featureId] = it }
            } else {
                FeatureFlag(
                    featureId = featureId,
                    scope = Scope.Unleash,
                    defaultValue = false,
                    value = featureEnabled,
                    variantName = name,
                    payloadType = payload?.type,
                    payloadValue = payload?.value,
                ).also { LumoFeatureFlags.flags[featureId] = it }
            }

        }

    override fun isReady(): Boolean = isReady

    companion object Companion {
        private const val TAG = "UnleashRepository"
        private val DISABLED = Variant(
            name = "disabled",
            enabled = false,
            featureEnabled = false,
            payload = null
        )
    }
}