package me.proton.android.lumo.ui.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseLinkDialog(
    onDismissRequest: () -> Unit,
    onOpenUrl: (String) -> Unit,
    title: String = "Purchase Subscription",
    messagePrefix: String = "We can't offer in‑app purchases right now, but don't worry — " +
            "tap here to head over to the ",
) {
    val annotatedString = buildAnnotatedString {
        append(messagePrefix)
        withLink(
            LinkAnnotation.Clickable(
                tag = "purchase_link",
                linkInteractionListener = { onOpenUrl("https://lumo.proton.me/") },
                styles = TextLinkStyles(style = SpanStyle(color = Color.Blue))
            )
        ) {
            append("purr‑chase ")
        }
        append("page!")
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Got it")
            }
        }
    )
}