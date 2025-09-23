package me.proton.android.lumo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = LightPurple,
    tertiary = Green,
    background = White,
    surface = White,
    onPrimary = White,
    onSecondary = DarkText,
    onTertiary = White,
    onBackground = DarkText,
    onSurface = DarkText,
    surfaceVariant = BorderGray,
    onSurfaceVariant = GrayText,
    error = ErrorRed,
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = LightPurple,
    tertiary = Green,
    background = Black,
    surface = Black,
    onPrimary = White,
    onSecondary = DarkText,
    onTertiary = White,
    onBackground = LightText,
    onSurface = LightText,
    surfaceVariant = BorderGray,
    onSurfaceVariant = GrayText,
    error = ErrorRed,
    onError = White
)


@Composable
fun LumoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}