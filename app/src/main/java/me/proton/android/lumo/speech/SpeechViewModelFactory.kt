package me.proton.android.lumo.speech

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SpeechViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SpeechViewModel::class.java) -> {
                // Create ViewModel with injected dependencies
                SpeechViewModel(application = application) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

    }
}