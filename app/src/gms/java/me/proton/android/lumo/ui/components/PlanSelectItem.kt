package me.proton.android.lumo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.proton.android.lumo.R
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.ui.text.asString
import me.proton.android.lumo.ui.theme.BorderGray
import me.proton.android.lumo.ui.theme.Green
import me.proton.android.lumo.ui.theme.LightPurple

/**
 * A selectable item that displays a subscription plan option
 */
@Composable
fun PlanSelectItem(
    plan: JsPlanInfo,
    isSelected: Boolean,
    onSelected: () -> Unit,
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else BorderGray
    val backgroundColor = if (isSelected) LightPurple else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .selectable(
                selected = isSelected, onClick = onSelected
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelected,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.onSecondary,
                unselectedColor = BorderGray
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plan.duration.asString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondary,
            )
            if (plan.pricePerMonth.isNotEmpty() && plan.cycle > 1) {
                Text(
                    "${plan.pricePerMonth}/" + stringResource(id = R.string.month),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            if (plan.totalPrice.isNotEmpty()) {
                Text(
                    stringResource(id = R.string.for_price) + " ${plan.totalPrice}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            plan.savings?.let {
                Text(
                    it, style = MaterialTheme.typography.bodySmall, color = Green
                )
            }
        }
    }
} 