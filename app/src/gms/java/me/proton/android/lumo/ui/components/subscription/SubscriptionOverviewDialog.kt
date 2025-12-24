package me.proton.android.lumo.ui.components.subscription

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.proton.android.lumo.billing.GoogleProductDetails
import me.proton.android.lumo.billing.SubscriptionState
import me.proton.android.lumo.ui.theme.LumoTheme

@Composable
fun SubscriptionOverviewDialog(
    googleProductDetails: List<GoogleProductDetails>,
    activeSubscriptions: SubscriptionState.Active,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LumoTheme.colors.backgroundNorm
    ) {
        SubscriptionOverviewSection(
            activeSubscriptions = activeSubscriptions,
            googleProductDetails = googleProductDetails,
            onClose = onClose
        )
    }
}
