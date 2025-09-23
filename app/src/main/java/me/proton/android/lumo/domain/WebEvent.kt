package me.proton.android.lumo.domain

sealed interface WebEvent {
    data object ShowPaymentRequested : WebEvent
    data object StartVoiceEntryRequested : WebEvent
    data object RetryLoadRequested : WebEvent
    data class PageTypeChanged(val isLumo: Boolean, val url: String) : WebEvent
    data class Navigated(val url: String, val type: String) : WebEvent
    data object LumoContainerVisible : WebEvent
    data class KeyboardVisibilityChanged(val isVisible: Boolean, val keyboardHeightPx: Int) :
        WebEvent

    data class BillingUnavailable(val message: String) : WebEvent
    data class PostResult(val transactionId: String, val resultJson: String) : WebEvent
    data class ThemeResult(val mode: String) : WebEvent {
        val theme = when (mode) {
            "Dark" -> 1
            "Light" -> 2
            else -> 0
        }
    }
}