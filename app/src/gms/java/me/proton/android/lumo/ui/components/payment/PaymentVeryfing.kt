package me.proton.android.lumo.ui.components.payment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.proton.android.lumo.ui.theme.LumoTheme

@Composable
fun PaymentVerifyingContent(
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Verifying Subscription",
            style = MaterialTheme.typography.titleMedium,
            color = LumoTheme.colors.textNorm
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We're confirming your subscription with our servers...",
            style = MaterialTheme.typography.bodyLarge,
            color = LumoTheme.colors.textWeak,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = LumoTheme.colors.primary,
            strokeWidth = 4.dp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "This usually takes less than a minute. Please don't close the app.",
            style = MaterialTheme.typography.bodyMedium,
            color = LumoTheme.colors.textWeak,
            textAlign = TextAlign.Center
        )
    }
}
