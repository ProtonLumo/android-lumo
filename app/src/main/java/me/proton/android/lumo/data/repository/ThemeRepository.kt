package me.proton.android.lumo.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import me.proton.android.lumo.ui.theme.AppStyle
import me.proton.android.lumo.webview.WebAppInterface

interface ThemeRepository {
    suspend fun saveTheme(theme: AppStyle)
    suspend fun getTheme(): AppStyle
    suspend fun observeTheme(isSystemInDarkMode: Boolean): Flow<AppStyle>
}

class ThemeRepositoryImpl(
    private val prefs: SharedPreferences,
    private val webBridge: WebAppInterface,
) : ThemeRepository {

    private val _themeFlow = MutableStateFlow(loadTheme())

    override suspend fun saveTheme(theme: AppStyle) {
        withContext(Dispatchers.IO) {
            prefs.edit { putInt(KEY_THEME, theme.mode) }
        }
        _themeFlow.value = theme
    }

    override suspend fun getTheme(): AppStyle = _themeFlow.value

    override suspend fun observeTheme(isSystemInDarkMode: Boolean): Flow<AppStyle> =
        _themeFlow
            .onEach { theme ->
                val (theme, mode) = when (theme) {
                    is AppStyle.System -> (if (isSystemInDarkMode) 15 else 14) to 0
                    is AppStyle.Dark -> 15 to 1
                    is AppStyle.Light -> 14 to 2
                }

                webBridge.injectTheme(theme, mode)
            }

    private fun loadTheme(): AppStyle {
        return AppStyle.fromInt(prefs.getInt(KEY_THEME, AppStyle.Light.mode))
    }

    companion object {
        private const val KEY_THEME = "key::lumo::theme"
    }
}