package me.proton.android.lumo.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import me.proton.android.lumo.ui.components.PaymentScreen

fun NavGraphBuilder.paymentRoutes(
    isReady: Boolean,
    onDismiss: () -> Unit,
) {
    composable<NavRoutes.Subscription> {
        val paymentEvent = it.toRoute<NavRoutes.Subscription>().paymentEvent
        PaymentScreen(
            isReady = isReady,
            paymentEvent = paymentEvent,
            onDismiss = onDismiss
        )
    }
}
