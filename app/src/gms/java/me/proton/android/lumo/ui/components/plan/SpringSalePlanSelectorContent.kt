package me.proton.android.lumo.ui.components.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.proton.android.lumo.MainActivityViewModel.PaymentEvent
import me.proton.android.lumo.R
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.ui.theme.LumoTheme

@Composable
fun SpringSalePlanSelectorContent(
    isSelected: Boolean,
    onSelect: () -> Unit,
    borderColor: Color,
    plan: JsPlanInfo,
    paymentEvent: PaymentEvent,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(vertical = 6.dp)) {
        PlanSelectorContent(isSelected, onSelect, borderColor, plan, paymentEvent)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .background(
                    color = LumoTheme.colors.interactionSecondary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(size = 16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_time),
                contentDescription = "",
                tint = LumoTheme.colors.interactionSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.limited_spring_sale_offer),
                style = MaterialTheme.typography.bodySmall,
                color = LumoTheme.colors.textNorm,
            )
        }
    }
}
