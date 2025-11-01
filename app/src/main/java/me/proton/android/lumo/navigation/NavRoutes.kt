package me.proton.android.lumo.navigation

import kotlinx.serialization.Serializable
import me.proton.android.lumo.MainActivityViewModel.PaymentEvent

sealed interface NavRoutes {
    @Serializable
    data object Chat : NavRoutes

    @Serializable
    data class Subscription(val paymentEvent: PaymentEvent) : NavRoutes

    @Serializable
    data object NoPayment : NavRoutes

    @Serializable
    data object SpeechToText : NavRoutes
}
