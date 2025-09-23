package me.proton.android.lumo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import me.proton.android.lumo.models.PlanFeature

/**
 * Displays a feature comparison row between free and paid plans
 */
@Composable
fun FeatureComparisonItem(feature: PlanFeature) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Load icon from URL
        val iconUrl = "https://lumo-api.proton.me/payments/v5/resources/icons/${feature.iconName}"

        // Icon - using AsyncImage with fallback
        Box(modifier = Modifier.size(24.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(iconUrl).crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Feature name
        Text(
            text = feature.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Left,
            modifier = Modifier.weight(1f)
        )

        // Plus tier text
        Text(
            text = feature.paidText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Right,
            modifier = Modifier.weight(0.8f)
        )
    }
} 