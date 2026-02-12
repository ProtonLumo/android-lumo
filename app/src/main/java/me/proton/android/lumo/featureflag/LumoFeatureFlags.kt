package me.proton.android.lumo.featureflag

import me.proton.android.lumo.featureflag.model.FeatureFlag
import me.proton.android.lumo.featureflag.model.FeatureId
import me.proton.android.lumo.featureflag.model.Scope

object LumoFeatureFlags {
    val androidTest = FeatureId("AndroidTest")
    val androidVariantTest = FeatureId("AndroidVariantTest")
    val ratingFeatureFlag = FeatureId("RatingAndroidLumo")
    val flags = mutableMapOf(
        androidTest to FeatureFlag(
            featureId = androidTest,
            scope = Scope.Unleash,
        ),
        androidVariantTest to FeatureFlag(
            featureId = androidVariantTest,
            scope = Scope.Unleash,
        ),
        ratingFeatureFlag to FeatureFlag(
            featureId = ratingFeatureFlag,
            scope = Scope.User
        )
    )
}
