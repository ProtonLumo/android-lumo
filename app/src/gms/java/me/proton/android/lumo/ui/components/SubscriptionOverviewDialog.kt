package me.proton.android.lumo.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.billingclient.api.ProductDetails
import me.proton.android.lumo.models.SubscriptionItemResponse
import me.proton.android.lumo.ui.theme.LumoTheme

@Composable
fun SubscriptionOverviewDialog(
    googleProductDetails: List<ProductDetails>,
    getSubscriptionPaymentStatus: () -> Triple<Boolean, Boolean, Long>,
    subscriptions: List<SubscriptionItemResponse>,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LumoTheme.colors.backgroundNorm
    ) {
        SubscriptionOverviewSection(
            subscriptions = subscriptions,
            googleProductDetails = googleProductDetails,
            getSubscriptionPaymentStatus = getSubscriptionPaymentStatus,
            onClose = onClose
        )
    }
}
