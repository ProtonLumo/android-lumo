package me.proton.android.lumo.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.android.lumo.MainActivityViewModel.PaymentEvent
import me.proton.android.lumo.data.repository.SubscriptionRepository
import me.proton.android.lumo.data.repository.ThemeRepository
import me.proton.android.lumo.money_machine.BillingAction
import me.proton.android.lumo.money_machine.BillingState
import me.proton.android.lumo.money_machine.BillingStore
import me.proton.android.lumo.navigation.NavRoutes
import me.proton.android.lumo.ui.theme.AppStyle
import me.proton.android.lumo.usecase.HasOfferUseCase
import javax.inject.Inject

private const val TAG = "SubscriptionViewModel"

/**
 * ViewModel that manages subscription data
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
    private val billingStore: BillingStore,
    private val themeRepository: ThemeRepository,
    private val hasOfferUseCase: HasOfferUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val paymentEvent: PaymentEvent =
        savedStateHandle.toRoute<NavRoutes.Subscription>().paymentEvent

    data class UiState(
        val billingState: BillingState = BillingState(),
        val theme: AppStyle? = null,
        val paymentEvent: PaymentEvent
    )

    private val _uiStateFlow = MutableStateFlow(UiState(paymentEvent = paymentEvent))
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        observeTheme()
        observeOffers()
        observeBillingState()

        // Kick off billing
        billingStore.dispatch(BillingAction.Initialize)
    }

    /* ───────────────── Observers ───────────────── */

    private fun observeTheme() {
        viewModelScope.launch {
            _uiStateFlow.update {
                it.copy(theme = themeRepository.getTheme())
            }
        }
    }

    private fun observeOffers() {
        viewModelScope.launch {
            hasOfferUseCase.hasOffer().collectLatest { hasOffer ->
                _uiStateFlow.update {
                    it.copy(
                        paymentEvent = if (hasOffer) paymentEvent else PaymentEvent.Default
                    )
                }
            }
        }
    }

    private fun observeBillingState() {
        viewModelScope.launch {
            billingStore.state.collectLatest { billingState ->

                _uiStateFlow.update { ui ->
                    ui.copy(
                        billingState = billingState
                    )
                }
            }
        }
    }

    fun selectPlan(planIndex: Int) {
        billingStore.dispatch(BillingAction.SelectPlan(planIndex))
    }

    fun launchBillingFlow(
        productId: String,
        offerToken: String?,
        customerId: String
    ) {
        billingStore.dispatch(
            BillingAction.LaunchPurchase(
                productId = productId,
                offerToken = offerToken,
                customerId = customerId
            )
        )
    }

    fun retryPaymentVerification() {
        billingStore.dispatch(BillingAction.RetryVerification)
    }
}
