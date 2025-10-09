package me.proton.android.lumo.ui.text

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface UiText {

    fun getText(context: Context): String

    data class StringText(val text: String) : UiText {
        override fun getText(context: Context): String = text
    }

    data class ResText(
        val res: Int,
        val values: List<Any>
    ) : UiText {
        constructor(res: Int, vararg values: Any) : this(res, values.toList())

        override fun getText(context: Context): String = context.getString(res, values)
    }
}

@Composable
fun UiText.asString(): String {
    return when (this) {
        is UiText.StringText -> text
        is UiText.ResText -> stringResource(res, *values.toTypedArray())
    }
}