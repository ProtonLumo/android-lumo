package me.proton.android.lumo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.proton.android.lumo.di.DependencyProvider

class MainActivityViewModelFactory : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MainActivityViewModel(
            themeRepository = DependencyProvider.themeRepository(),
            webAppRepository = DependencyProvider.webAppRepository(),
            hasOfferUseCase = DependencyProvider.hasOfferUseCase(),
            measureMainScreenReady = DependencyProvider.getMainScreenTracer()
        ) as T
}