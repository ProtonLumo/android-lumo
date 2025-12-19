package me.proton.android.lumo.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.billingclient.api.ProductDetails
import me.proton.android.lumo.money_machine.SubscriptionState
import me.proton.android.lumo.ui.theme.LumoTheme

@Composable
fun SubscriptionOverviewDialog(
    googleProductDetails: List<ProductDetails>,
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
