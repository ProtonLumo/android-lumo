package me.proton.android.lumo.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.android.lumo.data.repository.ThemeRepository
import me.proton.android.lumo.billing.BillingAction
import me.proton.android.lumo.billing.BillingEffectHandler
import me.proton.android.lumo.billing.BillingState
import me.proton.android.lumo.billing.BillingStore
import me.proton.android.lumo.ui.theme.AppStyle
import javax.inject.Inject

/**
 * ViewModel that manages subscription data
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val billingStore: BillingStore,
    private val themeRepository: ThemeRepository,
    private val billingEffectHandler: BillingEffectHandler,
) : ViewModel() {

    data class UiState(
        val billingState: BillingState = BillingState(),
        val theme: AppStyle? = null,
    )

    private val _uiStateFlow = MutableStateFlow(UiState())
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        observeTheme()
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
