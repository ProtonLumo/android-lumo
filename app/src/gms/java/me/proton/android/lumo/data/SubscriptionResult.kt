package me.proton.android.lumo.data

import me.proton.android.lumo.models.SubscriptionItemResponse
import me.proton.android.lumo.ui.components.UiText

data class SubscriptionResult(
    val subscriptions: List<SubscriptionItemResponse> = emptyList(),
    val hasValidSubscription: Boolean = false,
    val error: UiText? = null
)