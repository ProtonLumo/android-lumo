package me.proton.android.lumo

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.proton.android.lumo.data.repository.ThemeRepositoryImpl

class MainActivityViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val prefs = application.getSharedPreferences(
            "lumo_prefs",
            Context.MODE_PRIVATE
        )
        val repo = ThemeRepositoryImpl(prefs)
        return MainActivityViewModel(application, repo) as T
    }
}