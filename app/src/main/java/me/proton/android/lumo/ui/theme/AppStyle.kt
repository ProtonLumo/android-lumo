package me.proton.android.lumo.ui.theme

sealed class AppStyle(val mode: Int) {
    data object System : AppStyle(mode = 0)
    data object Dark : AppStyle(mode = 1)
    data object Light : AppStyle(mode = 2)

    companion object {
        fun fromInt(mode: Int): AppStyle = when (mode) {
            1 -> Dark
            2 -> Light
            else -> System
        }
    }
}