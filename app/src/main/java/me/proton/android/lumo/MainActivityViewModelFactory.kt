package me.proton.android.lumo

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.proton.android.lumo.di.DependencyProvider

class MainActivityViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MainActivityViewModel(
            application = application,
            themeRepository = DependencyProvider.themeRepository(),
            webAppRepository = DependencyProvider.webAppRepository(),
            hasOfferUseCase = DependencyProvider.hasOfferUseCase()
        ) as T
}