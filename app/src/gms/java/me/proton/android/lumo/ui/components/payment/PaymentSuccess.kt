package me.proton.android.lumo.ui.components.payment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.proton.android.lumo.ui.theme.LumoTheme

@Composable
fun PaymentSuccessContent(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Payment Successful!",
            style = MaterialTheme.typography.titleMedium,
            color = LumoTheme.colors.textNorm
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your subscription has been activated successfully.",
            style = MaterialTheme.typography.bodyLarge,
            color = LumoTheme.colors.textWeak,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(
                containerColor = LumoTheme.colors.primary,
                contentColor = LumoTheme.colors.textInvert,
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(
                text = "Continue", style = MaterialTheme.typography.labelLarge
            )
        }
    }
}