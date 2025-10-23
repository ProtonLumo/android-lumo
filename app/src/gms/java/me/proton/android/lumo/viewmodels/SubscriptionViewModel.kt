package me.proton.android.lumo.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.android.lumo.R
import me.proton.android.lumo.data.repository.SubscriptionRepository
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.models.SubscriptionItemResponse
import me.proton.android.lumo.ui.components.PaymentProcessingState
import me.proton.android.lumo.ui.text.UiText

private const val TAG = "SubscriptionViewModel"

/**
 * ViewModel that manages subscription data
 */
class SubscriptionViewModel(
    private val repository: SubscriptionRepository
) : ViewModel() {

    data class UiState(
        val isLoadingSubscriptions: Boolean = false,
        val subscriptions: List<SubscriptionItemResponse> = emptyList(),
        val hasValidSubscription: Boolean = false,
        val isLoadingPlans: Boolean = false,
        val planOptions: List<JsPlanInfo> = emptyList(),
        val selectedPlan: JsPlanInfo? = null,
        val planFeatures: List<PlanFeature> = emptyList(),
        val errorMessage: UiText? = null,
        val paymentProcessingState: PaymentProcessingState? = null,
        val isRefreshingPurchases: Boolean = false,
        val googleProductDetails: List<ProductDetails> = emptyList(),
    )

    private val _uiStateFlow = MutableStateFlow(UiState())
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        // Collect Google Play product details
        viewModelScope.launch {
            repository.getGooglePlayProducts().collectLatest { products ->
                _uiStateFlow.update {
                    it.copy(googleProductDetails = products)
                }
                Log.d(TAG, "Received ${products.size} Google Play products")

                // Update plan pricing if we have plans
                if (_uiStateFlow.value.planOptions.isNotEmpty()) {
                    updatePlanPricing(_uiStateFlow.value.planOptions)
                }
            }
        }
        viewModelScope.launch {
            repository.getPaymentProcessingState().collectLatest { state ->
                _uiStateFlow.update {
                    it.copy(paymentProcessingState = state)
                }
            }
        }
        viewModelScope.launch {
            repository.isRefreshingPurchases().collectLatest { isRefreshing ->
                _uiStateFlow.update {
                    it.copy(isRefreshing)
                }
            }
        }
    }

    /**
     * Load user subscriptions
     */
    private fun loadSubscriptions() {
        _uiStateFlow.update {
            it.copy(
                isLoadingSubscriptions = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val subscriptionResult = repository.fetchSubscriptions()
            if (!subscriptionResult.hasValidSubscription) {
                loadPlans()
            }
            _uiStateFlow.update {
                it.copy(
                    isLoadingSubscriptions = false,
                    subscriptions = subscriptionResult.subscriptions,
                    hasValidSubscription = subscriptionResult.hasValidSubscription,
                    errorMessage = it.errorMessage
                )
            }
        }
    }

    /**
     * Load available subscription plans
     */
    private fun loadPlans() {
        _uiStateFlow.update {
            it.copy(
                isLoadingPlans = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            val planResult = repository.fetchPlans()

            if (planResult.error == null) {
                updatePlanPricing(planResult.planOptions)
            }

            _uiStateFlow.update {
                it.copy(
                    isLoadingPlans = false,
                    planFeatures = planResult.planFeatures,
                    errorMessage = planResult.error
                )
            }
        }
    }

    /**
     * Update plan pricing with Google Play product details
     */
    private fun updatePlanPricing(planOptions: List<JsPlanInfo>) {
        if (planOptions.isEmpty() || _uiStateFlow.value.googleProductDetails.isEmpty()) {
            _uiStateFlow.update {
                it.copy(
                    errorMessage = UiText.ResText(R.string.error_problem_loading_subscriptions)
                )
            }
            return
        }

        Log.d(TAG, "Updating plan pricing from Google Play")

        val updatedPlans = repository.updatePlanPricing(
            planOptions, _uiStateFlow.value.googleProductDetails
        )

        // Only update if we have pricing info
        if (updatedPlans.any { it.totalPrice.isNotEmpty() }) {
            _uiStateFlow.update {
                it.copy(
                    planOptions = updatedPlans.toList() // Force update with new list
                )
            }

            val selectedPlan = _uiStateFlow.value.selectedPlan

            // Re-select the current plan or select first if none selected
            if (selectedPlan == null) {
                _uiStateFlow.update {
                    it.copy(
                        selectedPlan = updatedPlans.firstOrNull()
                    )
                }
            } else {
                // Find and update the currently selected plan
                val currentPlanId = selectedPlan.id
                _uiStateFlow.update {
                    it.copy(selectedPlan = updatedPlans.find { plan -> plan.id == currentPlanId }
                        ?: updatedPlans.firstOrNull())
                }
            }
        } else {
            Log.e(TAG, "No valid plans found")
            _uiStateFlow.update {
                it.copy(
                    errorMessage = UiText.ResText(R.string.error_no_plans_with_pricing)
                )
            }
        }
    }

    /**
     * Select a plan
     */
    fun selectPlan(plan: JsPlanInfo) {
        _uiStateFlow.update {
            it.copy(selectedPlan = plan)
        }
    }

    /**
     * Refresh subscription status
     */
    fun refreshSubscriptionStatus() {
        repository.refreshGooglePlaySubscriptionStatus()
        loadSubscriptions()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiStateFlow.update {
            it.copy(errorMessage = null)
        }
    }

    /**
     * Check for subscription sync mismatch between API and Google Play
     * Returns true if there's a mismatch that needs recovery
     */
    fun checkSubscriptionSyncMismatch(): Boolean {
        val hasValidSubscriptions = _uiStateFlow.value.hasValidSubscription
        // Get Google Play subscription status
        val (hasGooglePlaySubscription, isAutoRenewing) = getGooglePlaySubscriptionStatus()

        Log.d(
            TAG,
            "Subscription sync check - API hasValid: $hasValidSubscriptions, " + "GooglePlay hasActive: $hasGooglePlaySubscription, isRenewing: $isAutoRenewing"
        )

        // Check for mismatch: No valid subscription from API but active subscription on Google Play
        val hasMismatch = !hasValidSubscriptions && hasGooglePlaySubscription

        if (hasMismatch) {
            Log.w(TAG, "SUBSCRIPTION SYNC MISMATCH DETECTED!")
            Log.w(TAG, "API shows no valid subscription, but Google Play shows active subscription")
            Log.w(TAG, "This indicates a sync issue that needs recovery")
        }

        return hasMismatch
    }

    fun getGooglePlaySubscriptionStatus(): Triple<Boolean, Boolean, Long> =
        repository.getGooglePlaySubscriptionStatus()

    fun launchBillingFlowForProduct(
        productId: String,
        offerToken: String?,
        customerID: String? = null,
        getBillingResult: (BillingClient?, BillingFlowParams) -> BillingResult?
    ) {
        repository.launchBillingFlowForProduct(
            productId = productId,
            offerToken = offerToken,
            customerID = customerID,
            getBillingResult = getBillingResult,
        )
    }

    fun triggerSubscriptionRecovery() {
        repository.triggerSubscriptionRecovery()
    }

    fun retryPaymentVerification() {
        repository.retryPaymentVerification()
    }

    fun resetPaymentState() {
        repository.resetPaymentState()
    }
}