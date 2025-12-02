package me.proton.android.lumo.unleash

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.getunleash.android.Unleash
import io.getunleash.android.data.Toggle
import io.getunleash.android.events.UnleashReadyListener
import io.getunleash.android.events.UnleashStateListener
import java.util.UUID
import javax.inject.Inject

interface UnleashRepository {
    fun start()
    fun isEnabled(flag: String): Boolean
    fun isVariantEnabled(flag: String): Boolean
    fun getVariant(flag: String): UnleashRepository.FeatureVariant
    fun isReady(): Boolean

    data class FeatureVariant(
        val name: String,
        val enabled: Boolean,
        val payload: String?
    ) {
        companion object {
            val DISABLED = FeatureVariant("disabled", false, null)
        }
    }
}

class UnleashRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val unleash: Unleash,
) : UnleashRepository {
    private var isReady = false

    override fun start() {
        val readyListener = object : UnleashReadyListener {
            override fun onReady() {
                isReady = true
            }
        }
        val stateChanged = object : UnleashStateListener {
            override fun onStateChanged() {
                Log.e(TAG, "AndroidVariantTest: ${isEnabled(ANDROID_VARIANT_TEST)}")
                Log.e(TAG, "AndroidVariantTest: ${unleash.getVariant(ANDROID_VARIANT_TEST)}")
            }
        }
        unleash.start(
            eventListeners = listOf(readyListener, stateChanged),
            bootstrap = listOf(
                Toggle(name = ANDROID_TEST, enabled = false),
                Toggle(name = ANDROID_VARIANT_TEST, enabled = false)
            )
        )
    }

    override fun isEnabled(flag: String): Boolean =
        unleash.isEnabled(flag)

    override fun isVariantEnabled(flag: String): Boolean =
        unleash.getVariant(flag).featureEnabled

    override fun getVariant(flag: String): UnleashRepository.FeatureVariant {
        val variant = unleash.getVariant(flag)
        return UnleashRepository.FeatureVariant(
            name = variant.name,
            enabled = variant.enabled,
            payload = variant.payload?.value
        )
    }

    override fun isReady(): Boolean = isReady

    companion object {
        private const val TAG = "UnleashRepository"
        const val ANDROID_TEST = "AndroidTest"
        const val ANDROID_VARIANT_TEST = "AndroidVariantTest"
    }
}