package me.proton.android.lumo

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.proton.android.lumo.data.repository.WebAppRepositoryImpl
import me.proton.android.lumo.di.DependencyProvider

class MainActivityViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        val webAppRepository = WebAppRepositoryImpl(webBridge = DependencyProvider.getWebBridge())
        return MainActivityViewModel(
            application = application,
            themeRepository = DependencyProvider.themeRepository(),
            webAppRepository = webAppRepository
        ) as T
    }
}