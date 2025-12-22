package me.proton.android.lumo.ui.components.payment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.proton.android.lumo.ui.theme.LumoTheme

@Composable
fun PaymentError(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = LumoTheme.colors.signalDanger,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = LumoTheme.colors.textNorm
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Display a truncated error message
        val truncatedMessage = if (message.length > 80) {
            message.take(80) + "..."
        } else {
            message
        }

        Text(
            text = truncatedMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = LumoTheme.colors.textWeak,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PaymentErrorContent(
    modifier: Modifier = Modifier,
    message: String,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        PaymentError(
            title = "Payment Error",
            message = message,
        )

        Button(
            onClick = { onRetry() },
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
                text = "Retry", style = MaterialTheme.typography.labelLarge
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
            text = "Your payment was processed but we couldn't verify it with our servers. " + "Don't worry, we'll try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = LumoTheme.colors.textWeak,
            textAlign = TextAlign.Center
        )
    }
}
