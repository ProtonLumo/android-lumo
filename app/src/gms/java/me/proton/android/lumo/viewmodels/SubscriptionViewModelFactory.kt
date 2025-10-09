package me.proton.android.lumo.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.proton.android.lumo.data.repository.SubscriptionRepositoryImpl
import me.proton.android.lumo.di.DependencyProvider
import me.proton.android.lumo.webview.WebAppInterface

/**
 * Modern ViewModel factory that uses dependency injection principles
 * Replaces the old SubscriptionViewModelFactory with a cleaner approach
 */
class SubscriptionViewModelFactory() : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SubscriptionViewModel::class.java) -> {
                val billingManagerWrapper = DependencyProvider.getBillingManagerWrapper()
                val repository = SubscriptionRepositoryImpl(
                    billingManager = billingManagerWrapper.getBillingManager(),
                    webBridge = WebAppInterface
                )

                // Create ViewModel with injected dependencies
                SubscriptionViewModel(
                    repository = repository
                ) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
