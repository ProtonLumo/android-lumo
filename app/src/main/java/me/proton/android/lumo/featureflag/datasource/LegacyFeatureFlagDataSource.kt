package me.proton.android.lumo.featureflag.datasource

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import me.proton.android.lumo.featureflag.LumoFeatureFlags
import me.proton.android.lumo.featureflag.model.FeatureFlag
import me.proton.android.lumo.featureflag.model.FeatureId
import me.proton.android.lumo.webview.WebAppInterface
import timber.log.Timber
import javax.inject.Inject

interface LegacyFeatureFlagDataSource {

    fun start()

    suspend fun update(
        featureId: FeatureId,
        isEnabled: Boolean,
    ): FeatureFlag?

    fun get(featureId: FeatureId): FeatureFlag
    fun onFeaturesChanged(featureId: FeatureId): Flow<FeatureFlag>
}

class LegacyFeatureFlagDataSourceImpl @Inject constructor(
    private val webBridge: WebAppInterface,
) : LegacyFeatureFlagDataSource {
    private val legacyFeatures: List<FeatureId> = listOf(LumoFeatureFlags.ratingFeatureFlag)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshTrigger = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1
    )
    private val featuresChangedFlow = MutableSharedFlow<MutableMap<FeatureId, FeatureFlag>>(
        replay = 0,
        extraBufferCapacity = 1,
    )

    init {
        refreshTrigger
            .onEach {
                Timber.tag(TAG).d("Loading feature flags")

                webBridge.getFeatures(legacyFeatures)
                    .onSuccess { response ->
                        Timber.tag(TAG).d("Loaded feature flags: $response")

                        response.features
                            .map { it.toFeatureFlag() }
                            .forEach { featureFlag ->
                                LumoFeatureFlags.flags[featureFlag.featureId] = featureFlag
                            }

                        featuresChangedFlow.tryEmit(LumoFeatureFlags.flags)
                    }
                    .onFailure {
                        Timber.tag(TAG).d("Failure: $it")
                    }
            }
            .launchIn(scope)
    }

    override fun start() {
        refreshTrigger.tryEmit(Unit)
    }

    override suspend fun update(
        featureId: FeatureId,
        isEnabled: Boolean,
    ): FeatureFlag? =
        withContext(Dispatchers.IO) {
            val response = webBridge.updateFeatureValue(
                featureId = featureId,
                isEnabled = isEnabled,
            )
            if (response.isSuccess) {
                response.getOrNull()?.feature?.toFeatureFlag()?.also { featureFlag ->
                    LumoFeatureFlags.flags[featureFlag.featureId] = featureFlag
                }
            } else {
                null
            }
        }

    override fun get(featureId: FeatureId): FeatureFlag =
        LumoFeatureFlags.flags[featureId] ?: FeatureFlag.DEFAULT

    override fun onFeaturesChanged(featureId: FeatureId): Flow<FeatureFlag> =
        featuresChangedFlow
            .map { map -> map[featureId] ?: FeatureFlag.DEFAULT }
            .filter { it != FeatureFlag.DEFAULT }
            .distinctUntilChanged()

    companion object {
        private const val TAG = "LegacyFeatureFlagDataSource"
    }
}
