package me.proton.android.lumo.billing

import me.proton.android.lumo.R
import me.proton.android.lumo.ui.text.UiText

fun String?.fromDebugMessage(): UiText =
    when {
        this?.contains("API version is less than 3", ignoreCase = true) == true ->
            UiText.ResText(R.string.billing_unavailable_old_api)
        this?.contains("not supported", ignoreCase = true) == true ->
            UiText.ResText(R.string.billing_unavailable_not_supported)
        else ->
            UiText.ResText(R.string.billing_unavailable_generic)
    }