package me.proton.android.lumo.ui.components.payment

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import me.proton.android.lumo.MainActivityViewModel.PaymentEvent
import me.proton.android.lumo.R

@Composable
fun Header(
    paymentEvent: PaymentEvent,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    HeaderImage(
        paymentEvent = paymentEvent,
        isDarkTheme = isDarkTheme,
        modifier = modifier,
    )
}

@Composable
private fun HeaderImage(
    paymentEvent: PaymentEvent,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val imageRes = when (paymentEvent) {
        PaymentEvent.Default -> R.drawable.lumo_cat_on_laptop
        PaymentEvent.BlackFriday ->
            if (isDarkTheme) {
                R.drawable.lumo_black_friday_dark
            } else {
                R.drawable.lumo_black_friday
            }
    }
    Image(
        painter = painterResource(id = imageRes),
        contentScale =
            when (paymentEvent) {
                PaymentEvent.Default -> ContentScale.Fit
                PaymentEvent.BlackFriday -> ContentScale.Crop
            },
        contentDescription = "Lumo Plus",
        modifier = modifier
            .fillMaxWidth()
            .then(
                when (paymentEvent) {
                    PaymentEvent.Default ->
                        Modifier.windowInsetsPadding(WindowInsets.systemBars)

                    PaymentEvent.BlackFriday -> Modifier
                }
            )
    )
}
