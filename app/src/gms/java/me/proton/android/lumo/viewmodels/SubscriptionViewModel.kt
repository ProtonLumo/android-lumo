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
import me.proton.android.lumo.billing.BillingAction
import me.proton.android.lumo.billing.BillingEffectHandler
import me.proton.android.lumo.billing.BillingState
import me.proton.android.lumo.billing.BillingStore
import me.proton.android.lumo.data.repository.ThemeRepository
import me.proton.android.lumo.navigation.NavRoutes
import me.proton.android.lumo.ui.theme.AppStyle
import javax.inject.Inject

/**
 * ViewModel that manages subscription data
 */
@Suppress("UnusedPrivateProperty")
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val billingStore: BillingStore,
    private val themeRepository: ThemeRepository,
    private val billingEffectHandler: BillingEffectHandler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route: NavRoutes.Subscription = savedStateHandle.toRoute()
    private val paymentEvent: PaymentEvent = route.paymentEvent

    data class UiState(
        val billingState: BillingState = BillingState(),
        val theme: AppStyle? = null,
        val paymentEvent: PaymentEvent = PaymentEvent.Default,
    )

    private val _uiStateFlow = MutableStateFlow(UiState(paymentEvent = paymentEvent))
    val uiState = _uiStateFlow.asStateFlow()

    init {
        observeBillingState()

        billingStore.dispatch(BillingAction.Initialize(paymentEvent = paymentEvent))
    }

    private fun observeBillingState() {
        viewModelScope.launch {
            billingStore.state.collectLatest { billingState ->
                _uiStateFlow.update { uiState ->
                    val theme = themeRepository.getTheme()
                    uiState.copy(
                        billingState = billingState,
                        theme = theme,
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
        customerId: String?
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
