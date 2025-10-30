package me.proton.android.lumo.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.proton.android.lumo.MainActivityViewModel.PaymentEvent
import me.proton.android.lumo.di.DependencyProvider

/**
 * Modern ViewModel factory that uses dependency injection principles
 * Replaces the old SubscriptionViewModelFactory with a cleaner approach
 */
class SubscriptionViewModelFactory(
    private val paymentEvent: PaymentEvent
) : ViewModelProvider.Factory {


    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
    ): T {
        return when {
            modelClass.isAssignableFrom(SubscriptionViewModel::class.java) -> {
                // Create ViewModel with injected dependencies
                SubscriptionViewModel(
                    repository = DependencyProvider.getSubscriptionRepository(),
                    themeRepository = DependencyProvider.themeRepository(),
                    hasOfferUseCase = DependencyProvider.hasOfferUseCase(),
                    paymentEvent = paymentEvent,
                ) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
