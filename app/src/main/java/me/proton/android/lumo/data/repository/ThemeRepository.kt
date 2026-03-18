package me.proton.android.lumo.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import me.proton.android.lumo.ui.theme.AppStyle
import me.proton.android.lumo.webview.WebAppInterface
import javax.inject.Inject

interface ThemeRepository {
    suspend fun saveTheme(theme: AppStyle)
    suspend fun getTheme(): AppStyle
    suspend fun observeTheme(isSystemInDarkMode: Boolean): Flow<AppStyle>
}

class ThemeRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val webBridge: WebAppInterface,
) : ThemeRepository {

    private val themeStream: Flow<AppStyle> = dataStore.data
        .map { prefs -> AppStyle.fromInt(prefs[KEY_THEME] ?: AppStyle.Light.mode) }
        .catch { emit(AppStyle.Light) }

    override suspend fun saveTheme(theme: AppStyle) {
        dataStore.edit { prefs -> prefs[KEY_THEME] = theme.mode }
    }

    override suspend fun getTheme(): AppStyle = themeStream.firstOrNull() ?: AppStyle.System

    override suspend fun observeTheme(isSystemInDarkMode: Boolean): Flow<AppStyle> =
        themeStream
            .onEach { theme ->
                val (themeValue, mode) = when (theme) {
                    is AppStyle.System -> (if (isSystemInDarkMode) THEME_DARK else THEME_LIGHT) to MODE_SYSTEM
                    is AppStyle.Dark -> THEME_DARK to MODE_DARK
                    is AppStyle.Light -> THEME_LIGHT to MODE_LIGHT
                }

                webBridge.injectTheme(themeValue, mode)
            }

    companion object {
        private val KEY_THEME = intPreferencesKey("key::lumo::theme")
        private const val THEME_DARK = 15
        private const val THEME_LIGHT = 14
        private const val MODE_SYSTEM = 0
        private const val MODE_DARK = 1
        private const val MODE_LIGHT = 2
    }
}
