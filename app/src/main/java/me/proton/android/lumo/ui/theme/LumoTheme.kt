package me.proton.android.lumo.ui.theme

sealed class LumoTheme(val mode: Int) {
    data object System : LumoTheme(mode = 0)
    data object Dark : LumoTheme(mode = 1)
    data object Light : LumoTheme(mode = 2)

    companion object {
        fun fromInt(mode: Int): LumoTheme = when (mode) {
            1 -> Dark
            2 -> Light
            else -> System
        }
    }
}