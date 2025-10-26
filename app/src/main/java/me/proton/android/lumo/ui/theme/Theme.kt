package me.proton.android.lumo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf


val LocalAppColors = staticCompositionLocalOf { LightColors }
val LocalAppTypography = staticCompositionLocalOf { Typography }

object LumoTheme {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current

    val typography: androidx.compose.material3.Typography
        @Composable get() = LocalAppTypography.current
}


@Composable
fun LumoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val typography = Typography

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme(),
            typography = typography,
            content = content
        )
    }
}