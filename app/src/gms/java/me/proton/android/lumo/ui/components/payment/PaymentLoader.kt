package me.proton.android.lumo.ui.components.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.proton.android.lumo.ui.theme.LumoTheme

@Composable
fun Loader(
    messageRes: Int,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(color = LumoTheme.colors.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = messageRes),
            style = MaterialTheme.typography.bodyMedium,
            color = LumoTheme.colors.textWeak
        )
    }
}
