package me.proton.android.lumo.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun SimpleAlertDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(text = "Error")
        },
        text = {
            Text("In-app purchases are not available on this device. This may be due to an outdated Google Play Store version or device compatibility. Please try using the web version of Lumo for subscriptions.")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}