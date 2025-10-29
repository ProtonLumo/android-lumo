package me.proton.android.lumo.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.proton.android.lumo.MainActivityViewModel.PaymentEvent
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
    paymentEvent: PaymentEvent,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
) {
    val borderColor = if (isSelected) {
        if (plan.cycle == 12) {
            when (paymentEvent) {
                PaymentEvent.Default -> LumoTheme.colors.focus
                PaymentEvent.BlackFriday -> LumoTheme.colors.interactionSecondary
            }
        } else {
            LumoTheme.colors.focus
        }
    } else {
        LumoTheme.colors.borderNorm
    }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        ),
        label = "scaleAnimation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .height(IntrinsicSize.Min)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(
                color = LumoTheme.colors.planSelectionBackground(isDarkTheme),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .selectable(
                selected = isSelected,
                onClick = onSelected,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (plan.cycle == 12 && paymentEvent == PaymentEvent.BlackFriday) {
            Column {
                PlanSelectorContent(isSelected, onSelected, borderColor, plan, paymentEvent)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(LumoTheme.colors.interactionSecondary.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_time),
                            contentDescription = "",
                            tint = LumoTheme.colors.interactionSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.limited_black_friday_offer),
                            style = MaterialTheme.typography.bodySmall,
                            color = LumoTheme.colors.textNorm
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_sparkles),
                        contentDescription = "",
                        tint = LumoTheme.colors.interactionSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            PlanSelectorContent(isSelected, onSelected, borderColor, plan, paymentEvent)
        }

    }
}

@Composable
private fun PlanSelectorContent(
    isSelected: Boolean,
    onSelected: () -> Unit,
    borderColor: Color,
    plan: JsPlanInfo,
    paymentEvent: PaymentEvent
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(4.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelected,
            colors = RadioButtonDefaults.colors(
                selectedColor = borderColor,
                unselectedColor = LumoTheme.colors.borderNorm
            )
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = plan.duration.asString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = LumoTheme.colors.textNorm,
                )
                if (plan.cycle == 12 && paymentEvent == PaymentEvent.BlackFriday) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = LumoTheme.colors.interactionSecondary,
                                shape = RoundedCornerShape(4.dp)
                            )
                    ) {
                        Text(
                            text = stringResource(R.string.best_value).uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                            color = LumoTheme.colors.textNorm,
                            modifier = Modifier.padding(horizontal = 4.dp)

                        )
                    }
                }
            }
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
                    text = plan.totalPrice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LumoTheme.colors.linkNorm,
                    fontWeight = FontWeight.Medium
                )
            }

            plan.savings?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.discount_template,
                        it
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = when (paymentEvent) {
                        PaymentEvent.Default -> LumoTheme.colors.signalSuccess
                        PaymentEvent.BlackFriday -> LumoTheme.colors.interactionSecondary
                    }
                )
            }
        }

    }
}