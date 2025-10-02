package me.proton.android.lumo.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.android.lumo.R
import me.proton.android.lumo.data.repository.SubscriptionRepository
import me.proton.android.lumo.data.repository.SubscriptionRepositoryImpl
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PaymentJsResponse
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.models.SubscriptionItemResponse

private const val TAG = "SubscriptionViewModel"

/**
 * ViewModel that manages subscription data
 */
class SubscriptionViewModel(
    private val application: Application, private val repository: SubscriptionRepository
) : ViewModel() {

    sealed interface UiEvent {
        data object LoadSubscriptions : UiEvent
        data object LoadPlans : UiEvent
    }

    data class UiState(
        val isLoadingSubscriptions: Boolean = false,
        val subscriptions: List<SubscriptionItemResponse> = emptyList(),
        val hasValidSubscription: Boolean = false,
        val isLoadingPlans: Boolean = false,
        val planOptions: List<JsPlanInfo> = emptyList(),
        val selectedPlan: JsPlanInfo? = null,
        val planFeatures: List<PlanFeature> = emptyList(),
        val errorMessage: String? = null,
    )

    private val _uiStateFlow = MutableStateFlow(UiState())
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: Flow<UiEvent> = _events

    // Google Play product details
    private val _googleProductDetails = MutableStateFlow<List<ProductDetails>>(emptyList())

    init {
        // Collect Google Play product details
        viewModelScope.launch {
            repository.getGooglePlayProducts().collectLatest { products ->
                _googleProductDetails.value = products
                Log.d(TAG, "Received ${products.size} Google Play products")

                // Update plan pricing if we have plans
                if (_uiStateFlow.value.planOptions.isNotEmpty()) {
                    updatePlanPricing()
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
                isLoadingSubscriptions = true, errorMessage = null
            )
        }

        viewModelScope.launch {
            _events.emit(UiEvent.LoadSubscriptions)
        }
    }

    fun subscriptionsLoaded(result: Result<PaymentJsResponse>) {
        try {
            result.onSuccess { response ->
                // Parse subscriptions from response
                if (response.data != null && response.data.isJsonObject) {
                    val parsedSubscriptions =
                        (repository as? SubscriptionRepositoryImpl)?.parseSubscriptions(response)
                            ?: emptyList()

                    _uiStateFlow.update {
                        it.copy(
                            subscriptions = parsedSubscriptions,
                            hasValidSubscription = repository.hasValidSubscription(
                                parsedSubscriptions
                            )
                        )
                    }

                    Log.d(
                        TAG,
                        "Loaded ${parsedSubscriptions.size} subscriptions, " + "hasValid=${_uiStateFlow.value.hasValidSubscription}"
                    )
                } else {
                    Log.e(TAG, "Invalid subscription data format")
                    _uiStateFlow.update {
                        it.copy(
                            subscriptions = emptyList(),
                            hasValidSubscription = false,
                        )
                    }
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load subscriptions: ${error.message}", error)
                _uiStateFlow.update {
                    it.copy(
                        errorMessage = application.getString(
                            R.string.error_failed_to_load_subscriptions,
                            error.message ?: "Unknown error"
                        ), subscriptions = emptyList(), hasValidSubscription = false
                    )
                }
            }

            // If the user doesn't have a valid subscription, load plans
            if (!_uiStateFlow.value.hasValidSubscription) {
                loadPlans()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading subscriptions", e)
            _uiStateFlow.update {
                it.copy(
                    errorMessage = application.getString(
                        R.string.error_loading_subscriptions, e.message ?: "Unknown error"
                    ), subscriptions = emptyList(), hasValidSubscription = false
                )
            }

            // Try to load plans anyway
            loadPlans()
        } finally {
            _uiStateFlow.update {
                it.copy(isLoadingSubscriptions = false)
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
            _events.emit(UiEvent.LoadPlans)
        }
    }

    fun plansLoaded(result: Result<PaymentJsResponse>) {
        try {
            result.onSuccess { response ->
                // Extract features from the response
                _uiStateFlow.update {
                    it.copy(
                        planFeatures = repository.extractPlanFeatures(response)
                    )
                }

                // Extract plans from the response
                val extractedPlans = repository.extractPlans(response)

                if (extractedPlans.isNotEmpty()) {
                    // Update plan pricing
                    val updatedPlans = repository.updatePlanPricing(
                        extractedPlans, _googleProductDetails.value
                    )

                    // Only update if we have pricing info
                    if (updatedPlans.any { it.totalPrice.isNotEmpty() }) {
                        _uiStateFlow.update {
                            it.copy(
                                planOptions = updatedPlans,
                                selectedPlan = updatedPlans.firstOrNull()
                            )
                        }
                        Log.d(TAG, "Loaded ${updatedPlans.size} plans with pricing")
                    } else {
                        _uiStateFlow.update {
                            it.copy(
                                errorMessage = application.getString(R.string.error_no_plans_with_pricing)
                            )
                        }
                        Log.e(TAG, "No plans with pricing information available")
                    }
                } else {
                    Log.e(TAG, "No valid plans found")
                    _uiStateFlow.update {
                        it.copy(
                            errorMessage = application.getString(R.string.error_problem_loading_subscriptions)
                        )
                    }
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load plans: ${error.message}", error)
                _uiStateFlow.update {
                    it.copy(
                        errorMessage = application.getString(
                            R.string.error_failed_to_load_plans, error.message ?: "Unknown error"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading plans", e)
            _uiStateFlow.update {
                it.copy(
                    errorMessage = application.getString(
                        R.string.error_loading_plans, e.message ?: "Unknown error"
                    )
                )
            }
        } finally {
            _uiStateFlow.update {
                it.copy(
                    isLoadingPlans = false
                )
            }
        }
    }

    /**
     * Update plan pricing with Google Play product details
     */
    private fun updatePlanPricing() {
        val planOptions = _uiStateFlow.value.planOptions
        if (planOptions.isEmpty() || _googleProductDetails.value.isEmpty()) {
            return
        }

        Log.d(TAG, "Updating plan pricing from Google Play")

        val updatedPlans = repository.updatePlanPricing(
            planOptions, _googleProductDetails.value
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
        val (hasGooglePlaySubscription, isAutoRenewing) = repository.getGooglePlaySubscriptionStatus()

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
}