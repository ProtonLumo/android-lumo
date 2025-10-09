package me.proton.android.lumo.ui.components

import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable

@Composable
fun PaymentDialog(
    isReady: Boolean,
    onDismiss: () -> Unit,
) {
    SimpleAlertDialog(onDismiss = onDismiss)
}