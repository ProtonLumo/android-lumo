package me.proton.android.lumo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf


val LocalLumoAppColors = staticCompositionLocalOf { LightColors }
val LocalLumoAppTypography = staticCompositionLocalOf { Typography }

object LumoTheme {
    val colors: AppColors
        @Composable
        get() = LocalLumoAppColors.current

    val typography: androidx.compose.material3.Typography
        @Composable get() = LocalLumoAppTypography.current
}


@Composable
fun LumoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val typography = Typography

    CompositionLocalProvider(
        LocalLumoAppColors provides colors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColorScheme(
                primary = colors.primary,
                onPrimary = colors.textNorm,
                background = colors.backgroundNorm,
                onBackground = colors.textNorm,
                surface = colors.backgroundNorm,
                onSurface = colors.textNorm
            ) else lightColorScheme(
                primary = colors.primary,
                onPrimary = colors.textNorm,
                background = colors.backgroundNorm,
                onBackground = colors.textNorm,
                surface = colors.backgroundNorm,
                onSurface = colors.textNorm
            ),
            typography = typography,
            content = content
        )
    }
}