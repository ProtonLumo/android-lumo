package me.proton.android.lumo.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.android.lumo.ui.theme.LumoTheme

interface ThemeRepository {

    suspend fun saveTheme(theme: LumoTheme)

    suspend fun getTheme(): LumoTheme
}

class ThemeRepositoryImpl(private val prefs: SharedPreferences) : ThemeRepository {

    override suspend fun saveTheme(theme: LumoTheme) =
        withContext(Dispatchers.IO) {
            prefs.edit { putInt(KEY_THEME, theme.mode) }
        }

    override suspend fun getTheme(): LumoTheme =
        withContext(Dispatchers.IO) {
            LumoTheme.fromInt(
                prefs.getInt(KEY_THEME, LumoTheme.Light.mode)
            )
        }


    companion object {
        private const val KEY_THEME = "key::lumo::theme"
    }
}