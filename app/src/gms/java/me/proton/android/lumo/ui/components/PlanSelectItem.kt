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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.proton.android.lumo.R
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.ui.text.asString
import me.proton.android.lumo.ui.theme.LumoTheme
import me.proton.android.lumo.ui.theme.planSelectionBackground

/**
 * A selectable item that displays a subscription plan option
 */
@Composable
fun PlanSelectItem(
    plan: JsPlanInfo,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
) {
    val borderColor = if (isSelected) LumoTheme.colors.focus else LumoTheme.colors.borderNorm

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(
                color = LumoTheme.colors.planSelectionBackground(isDarkTheme),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .selectable(
                selected = isSelected, onClick = onSelected
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelected,
            colors = RadioButtonDefaults.colors(
                selectedColor = LumoTheme.colors.focus,
                unselectedColor = LumoTheme.colors.borderNorm
            )
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plan.duration.asString(),
                style = MaterialTheme.typography.labelLarge,
                color = LumoTheme.colors.textNorm,
            )
            if (plan.pricePerMonth.isNotEmpty() && plan.cycle > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${plan.pricePerMonth}/" + stringResource(id = R.string.month),
                    style = MaterialTheme.typography.bodySmall,
                    color = LumoTheme.colors.textWeak
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = 16.dp)
        ) {
            if (plan.totalPrice.isNotEmpty()) {
                Text(
                    stringResource(id = R.string.for_price) + " ${plan.totalPrice}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LumoTheme.colors.linkNorm,
                    fontWeight = FontWeight.Medium
                )
            }

            plan.savings?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = LumoTheme.colors.signalSuccess
                )
            }
        }
    }
} 