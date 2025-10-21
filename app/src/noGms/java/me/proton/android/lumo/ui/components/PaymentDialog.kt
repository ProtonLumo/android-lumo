package me.proton.android.lumo.ui.components

import androidx.compose.runtime.Composable

@Composable
fun PaymentDialog(
    isReady: Boolean,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    PurchaseLinkDialog(
        onOpenUrl = onOpenUrl,
        onDismissRequest = onDismiss,
    )
}