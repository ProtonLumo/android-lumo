package me.proton.android.lumo.data

import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.ui.components.UiText

data class PlanResult(
    val planFeatures: List<PlanFeature> = emptyList(),
    val planOptions: List<JsPlanInfo> = emptyList(),
    val error: UiText? = null,
)