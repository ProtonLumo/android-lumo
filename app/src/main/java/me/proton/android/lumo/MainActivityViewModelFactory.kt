package me.proton.android.lumo

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.proton.android.lumo.data.repository.ThemeRepositoryImpl
import me.proton.android.lumo.data.repository.WebAppRepositoryImpl
import me.proton.android.lumo.di.DependencyProvider
import me.proton.android.lumo.webview.WebAppInterface

class MainActivityViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val prefs = application.getSharedPreferences(
            "lumo_prefs",
            Context.MODE_PRIVATE
        )
        val themeRepository = ThemeRepositoryImpl(prefs)
        val webAppRepository = WebAppRepositoryImpl(webBridge = DependencyProvider.getWebBridge())
        return MainActivityViewModel(
            application = application,
            themeRepository = themeRepository,
            webAppRepository = webAppRepository
        ) as T
    }
}