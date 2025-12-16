package me.proton.android.lumo.review

import android.app.Activity
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.proton.android.lumo.featureflag.FeatureGatekeeper
import me.proton.android.lumo.featureflag.LumoFeatureFlags
import timber.log.Timber
import javax.inject.Inject

class InAppReviewManagerImpl @Inject constructor(
    private val featureGatekeeper: FeatureGatekeeper,
    private val reviewRepository: ReviewRepository
) : InAppReviewManager {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun start(activity: Activity) {
        scope.launch {
            featureGatekeeper
                .observeFeature(LumoFeatureFlags.ratingFeatureFlag)
                .collect { featureFlag ->
                    Timber.tag(TAG).d("Feature observed: $featureFlag")
                    if (featureFlag.value && !reviewRepository.hasSeen()) {
                        requestReview(activity)
                    }
                }
        }
    }

    override fun stop() {
        scope.cancel()
    }

    private fun requestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // We got the ReviewInfo object
                val reviewInfo = task.result

                Timber.tag(TAG).d("Got review info: $reviewInfo")
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    Timber.tag(TAG).d("In-App Review flow completed")
                    scope.launch {
                        featureGatekeeper.updateLegacyFeature(
                            featureId = LumoFeatureFlags.ratingFeatureFlag,
                            isEnabled = false
                        )
                        reviewRepository.markSeen()
                    }
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow.
                }
            } else {
                // There was some problem, log or handle the error code.
                @ReviewErrorCode val reviewErrorCode =
                    (task.exception as ReviewException).errorCode
            }
        }
    }

    companion object {
        private const val TAG = "InAppReviewManager"
    }
}