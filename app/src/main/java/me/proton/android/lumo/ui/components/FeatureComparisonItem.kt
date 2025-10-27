package me.proton.android.lumo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.ui.theme.LumoTheme

/**
 * Displays a feature comparison row between free and paid plans
 */
@Composable
fun FeatureComparisonItem(
    feature: PlanFeature,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Load icon from URL
        val iconUrl = "https://lumo-api.proton.me/payments/v5/resources/icons/${feature.iconName}"

        // Icon - using AsyncImage with fallback
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(0.8f),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .background(
                            color = LumoTheme.colors.backgroundWeak,
                            shape = RoundedCornerShape(6.dp)
                        )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(iconUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(20.dp),
                    colorFilter = ColorFilter.tint(LumoTheme.colors.primary)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = feature.name,
                style = MaterialTheme.typography.bodyMedium,
                color = LumoTheme.colors.textNorm,
                textAlign = TextAlign.Left,
            )
        }

        // Plus tier text
        Text(
            text = feature.paidText,
            style = MaterialTheme.typography.bodyMedium,
            color = LumoTheme.colors.focus,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.2f)
        )
    }
} 