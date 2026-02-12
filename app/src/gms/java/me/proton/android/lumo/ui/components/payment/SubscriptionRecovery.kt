package me.proton.android.lumo.ui.components.payment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.proton.android.lumo.ui.theme.LumoTheme

@Composable
fun SubscriptionRecoveryContent(
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        PaymentError(
            title = "Subscription Recovery",
            message = "We found an active subscription on your Google Play account that isn't synced with our servers.",
        )

        Button(
            onClick = {
                onRetry()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = LumoTheme.colors.primary,
                contentColor = LumoTheme.colors.textInvert,
            ),
            shape = RoundedCornerShape(24.dp),
            enabled = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(
                text = "Recover Subscription", style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = LumoTheme.colors.primary
            ),
            border = BorderStroke(1.dp, LumoTheme.colors.primary),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = "Close", style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Click 'Recover Subscription' to sync your Google Play subscription with our servers. " +
                    "This will restore your subscription access.",
            style = MaterialTheme.typography.bodyMedium,
            color = LumoTheme.colors.textWeak,
            textAlign = TextAlign.Center
        )
    }
}
