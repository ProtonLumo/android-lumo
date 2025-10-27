package me.proton.android.lumo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.proton.android.lumo.R
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.ui.text.asString
import me.proton.android.lumo.ui.theme.LumoTheme

/**
 * Represents the different states of payment processing
 */
sealed class PaymentProcessingState {
    data object Loading : PaymentProcessingState()
    data object Verifying : PaymentProcessingState()
    data class Error(val message: UiText) : PaymentProcessingState()
    data class NetworkError(val message: UiText) : PaymentProcessingState()
    data object Success : PaymentProcessingState()
    data class SubscriptionRecovery(val message: String) : PaymentProcessingState()
}

/**
 * Screen that shows payment processing status and handles retries
 */
@Composable
fun PaymentProcessingScreen(
    state: PaymentProcessingState,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(), color = LumoTheme.colors.backgroundNorm
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Logo/Image at top
            Image(
                painter = painterResource(id = R.drawable.lumo_cat_on_laptop),
                contentDescription = "Lumo",
                modifier = Modifier.height(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Different content based on state
            when (state) {
                is PaymentProcessingState.Loading -> {
                    PaymentLoadingContent()
                }

                is PaymentProcessingState.Verifying -> {
                    PaymentVerifyingContent()
                }

                is PaymentProcessingState.Error -> {
                    PaymentErrorContent(
                        message = state.message.asString(),
                        onRetry = onRetry,
                        onClose = onClose
                    )
                }

                is PaymentProcessingState.NetworkError -> {
                    PaymentNetworkErrorContent(
                        message = state.message.asString(),
                        onRetry = onRetry,
                        onClose = onClose
                    )
                }

                is PaymentProcessingState.Success -> {
                    PaymentSuccessContent(onClose = onClose)
                }

                is PaymentProcessingState.SubscriptionRecovery -> {
                    SubscriptionRecoveryContent(
                        message = state.message, onRetry = onRetry, onClose = onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentLoadingContent() {
    val loadingTexts = listOf(
        "Processing your payment...",
        "This will just take a moment...",
        "Setting up your subscription..."
    )

    var currentTextIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(key1 = Unit) {
        while (true) {
            delay(2000)
            currentTextIndex = (currentTextIndex + 1) % loadingTexts.size
        }
    }

    Text(
        text = "Payment Processing",
        style = MaterialTheme.typography.titleMedium,
        color = LumoTheme.colors.textNorm
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = loadingTexts[currentTextIndex],
        style = MaterialTheme.typography.bodyLarge,
        color = LumoTheme.colors.textNorm,
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
        text = "Please wait while we process your payment with Google Play.",
        style = MaterialTheme.typography.bodyMedium,
        color = LumoTheme.colors.textWeak,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PaymentVerifyingContent() {
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

@Composable
private fun PaymentErrorContent(
    message: String, onRetry: () -> Unit, onClose: () -> Unit
) {
    var isRetrying by remember { mutableStateOf(false) }

    // Reset retry loading state when we get back to error state
    LaunchedEffect(message) {
        isRetrying = false
    }

    Icon(
        imageVector = Icons.Default.Error,
        contentDescription = "Error",
        tint = LumoTheme.colors.signalDanger,
        modifier = Modifier.size(48.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Payment Error",
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

    Button(
        onClick = {
            isRetrying = true
            onRetry()
        }, modifier = Modifier
            .fillMaxWidth()
            .height(48.dp), colors = ButtonDefaults.buttonColors(
            containerColor = LumoTheme.colors.primary
        ), shape = RoundedCornerShape(24.dp), enabled = !isRetrying
    ) {
        if (isRetrying) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "Retry", style = MaterialTheme.typography.labelLarge
            )
        }
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

@Composable
private fun PaymentNetworkErrorContent(
    message: String, onRetry: () -> Unit, onClose: () -> Unit
) {
    var isRetrying by remember { mutableStateOf(false) }

    // Reset retry loading state when we get back to error state
    LaunchedEffect(message) {
        isRetrying = false
    }

    Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = "Network Error",
        tint = LumoTheme.colors.signalWarning,
        modifier = Modifier.size(48.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Connection Error",
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

    Button(
        onClick = {
            isRetrying = true
            onRetry()
        }, modifier = Modifier
            .fillMaxWidth()
            .height(48.dp), colors = ButtonDefaults.buttonColors(
            containerColor = LumoTheme.colors.primary
        ), shape = RoundedCornerShape(24.dp), enabled = !isRetrying
    ) {
        if (isRetrying) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp
            )
        } else {
            Text(
                text = stringResource(id = R.string.retry_connection),
                style = MaterialTheme.typography.labelLarge
            )
        }
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
        text = stringResource(id = R.string.please_check_your_internet_connection_and_try_again),
        style = MaterialTheme.typography.bodyMedium,
        color = LumoTheme.colors.textWeak,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SubscriptionRecoveryContent(
    message: String, onRetry: () -> Unit, onClose: () -> Unit
) {
    var isRetrying by remember { mutableStateOf(false) }

    // Reset retry loading state when we get back to error state
    LaunchedEffect(message) {
        isRetrying = false
    }

    Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = "Subscription Recovery",
        tint = LumoTheme.colors.signalDanger,
        modifier = Modifier.size(48.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Subscription Recovery",
        style = MaterialTheme.typography.titleMedium,
        color = LumoTheme.colors.textNorm
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "We found an active subscription on your Google Play account that isn't synced with our servers.",
        style = MaterialTheme.typography.bodyLarge,
        color = LumoTheme.colors.textWeak,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = {
            isRetrying = true
            onRetry()
        }, modifier = Modifier
            .fillMaxWidth()
            .height(48.dp), colors = ButtonDefaults.buttonColors(
            containerColor = LumoTheme.colors.primary
        ), shape = RoundedCornerShape(24.dp), enabled = !isRetrying
    ) {
        if (isRetrying) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "Recover Subscription", style = MaterialTheme.typography.labelLarge
            )
        }
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
        text = "Click 'Recover Subscription' to sync your Google Play subscription with our servers. " + "This will restore your subscription access.",
        style = MaterialTheme.typography.bodyMedium,
        color = LumoTheme.colors.textWeak,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PaymentSuccessContent(
    onClose: () -> Unit
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
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LumoTheme.colors.primary
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Text(
            text = "Continue", style = MaterialTheme.typography.labelLarge
        )
    }
} 