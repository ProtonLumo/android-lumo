package me.proton.android.lumo.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.proton.android.lumo.R
import me.proton.android.lumo.data.repository.SubscriptionRepository
import me.proton.android.lumo.data.repository.SubscriptionRepositoryImpl
import me.proton.android.lumo.models.JsPlanInfo
import me.proton.android.lumo.models.PlanFeature
import me.proton.android.lumo.models.SubscriptionItemResponse
import me.proton.android.lumo.billing.BillingManagerWrapper

private const val TAG = "SubscriptionViewModel"

/**
 * ViewModel that manages subscription data
 */
class SubscriptionViewModel(
    private val application: Application,
    private val repository: SubscriptionRepository,
    private val billingManagerWrapper: BillingManagerWrapper
) : ViewModel() {

    // Subscriptions state
    private val _isLoadingSubscriptions = MutableStateFlow(false)
    val isLoadingSubscriptions: StateFlow<Boolean> = _isLoadingSubscriptions.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<SubscriptionItemResponse>>(emptyList())
    val subscriptions: StateFlow<List<SubscriptionItemResponse>> = _subscriptions.asStateFlow()

    private val _hasValidSubscription = MutableStateFlow(false)
    val hasValidSubscription: StateFlow<Boolean> = _hasValidSubscription.asStateFlow()

    // Plans state
    private val _isLoadingPlans = MutableStateFlow(false)
    val isLoadingPlans: StateFlow<Boolean> = _isLoadingPlans.asStateFlow()

    private val _planOptions = MutableStateFlow<List<JsPlanInfo>>(emptyList())
    val planOptions: StateFlow<List<JsPlanInfo>> = _planOptions.asStateFlow()

    private val _selectedPlan = MutableStateFlow<JsPlanInfo?>(null)
    val selectedPlan: StateFlow<JsPlanInfo?> = _selectedPlan.asStateFlow()

    private val _planFeatures = MutableStateFlow<List<PlanFeature>>(emptyList())
    val planFeatures: StateFlow<List<PlanFeature>> = _planFeatures.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Google Play product details
    private val _googleProductDetails = MutableStateFlow<List<ProductDetails>>(emptyList())

    init {
        // Collect Google Play product details
        viewModelScope.launch {
            repository.getGooglePlayProducts().collectLatest { products ->
                _googleProductDetails.value = products
                Log.d(TAG, "Received ${products.size} Google Play products")

                // Update plan pricing if we have plans
                if (_planOptions.value.isNotEmpty()) {
                    updatePlanPricing()
                }
            }
        }
    }

    /**
     * Load user subscriptions
     */
    fun loadSubscriptions() {
        _isLoadingSubscriptions.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = repository.getSubscriptions()

                result.onSuccess { response ->
                    // Parse subscriptions from response
                    if (response.data != null && response.data.isJsonObject) {
                        val parsedSubscriptions =
                            (repository as? SubscriptionRepositoryImpl)
                                ?.parseSubscriptions(response) ?: emptyList()

                        _subscriptions.value = parsedSubscriptions
                        _hasValidSubscription.value =
                            repository.hasValidSubscription(parsedSubscriptions)

                        Log.d(
                            TAG,
                            "Loaded ${parsedSubscriptions.size} subscriptions, hasValid=${_hasValidSubscription.value}"
                        )
                    } else {
                        Log.e(TAG, "Invalid subscription data format")
                        _subscriptions.value = emptyList()
                        _hasValidSubscription.value = false
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Failed to load subscriptions: ${error.message}", error)
                    _errorMessage.value = application.getString(
                        R.string.error_failed_to_load_subscriptions,
                        error.message ?: "Unknown error"
                    )
                    _subscriptions.value = emptyList()
                    _hasValidSubscription.value = false
                }

                // If the user doesn't have a valid subscription, load plans
                if (!_hasValidSubscription.value) {
                    loadPlans()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading subscriptions", e)
                _errorMessage.value = application.getString(
                    R.string.error_loading_subscriptions,
                    e.message ?: "Unknown error"
                )
                _subscriptions.value = emptyList()
                _hasValidSubscription.value = false

                // Try to load plans anyway
                loadPlans()
            } finally {
                _isLoadingSubscriptions.value = false
            }
        }
    }

    /**
     * Load available subscription plans
     */
    private fun loadPlans() {
        _isLoadingPlans.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = repository.getPlans()

                result.onSuccess { response ->
                    // Extract features from the response
                    _planFeatures.value = repository.extractPlanFeatures(response)

                    // Extract plans from the response
                    val extractedPlans = repository.extractPlans(response)

                    if (extractedPlans.isNotEmpty()) {
                        // Update plan pricing
                        val updatedPlans = repository.updatePlanPricing(
                            extractedPlans,
                            _googleProductDetails.value
                        )

                        // Only update if we have pricing info
                        if (updatedPlans.any { it.totalPrice.isNotEmpty() }) {
                            _planOptions.value = updatedPlans
                            _selectedPlan.value = updatedPlans.firstOrNull()
                            Log.d(TAG, "Loaded ${updatedPlans.size} plans with pricing")
                        } else {
                            Log.e(TAG, "No plans with pricing information available")
                            _errorMessage.value =
                                application.getString(R.string.error_no_plans_with_pricing)
                        }
                    } else {
                        Log.e(TAG, "No valid plans found")
                        _errorMessage.value =
                            application.getString(R.string.error_problem_loading_subscriptions)
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Failed to load plans: ${error.message}", error)
                    _errorMessage.value = application.getString(
                        R.string.error_failed_to_load_plans,
                        error.message ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading plans", e)
                _errorMessage.value = application.getString(
                    R.string.error_loading_plans,
                    e.message ?: "Unknown error"
                )
            } finally {
                _isLoadingPlans.value = false
            }
        }
    }

    /**
     * Update plan pricing with Google Play product details
     */
    private fun updatePlanPricing() {
        if (_planOptions.value.isEmpty() || _googleProductDetails.value.isEmpty()) {
            return
        }

        Log.d(TAG, "Updating plan pricing from Google Play")

        val updatedPlans = repository.updatePlanPricing(
            _planOptions.value,
            _googleProductDetails.value
        )

        // Only update if we have pricing info
        if (updatedPlans.any { it.totalPrice.isNotEmpty() }) {
            _planOptions.value = updatedPlans.toList() // Force update with new list

            // Re-select the current plan or select first if none selected
            if (_selectedPlan.value == null) {
                _selectedPlan.value = updatedPlans.firstOrNull()
            } else {
                // Find and update the currently selected plan
                val currentPlanId = _selectedPlan.value?.id
                _selectedPlan.value = updatedPlans.find { it.id == currentPlanId }
                    ?: updatedPlans.firstOrNull()
            }
        }
    }

    /**
     * Select a plan
     */
    fun selectPlan(plan: JsPlanInfo) {
        _selectedPlan.value = plan
    }

    /**
     * Refresh subscription status
     */
    fun refreshSubscriptionStatus() {
        repository.refreshGooglePlaySubscriptionStatus()
        loadSubscriptions()
    }

    /**
     * Get Google Play subscription status
     */
    fun getSubscriptionStatus(): Triple<Boolean, Boolean, Long> {
        return repository.getGooglePlaySubscriptionStatus()
    }

    /**
     * Open subscription management screen
     */
    fun openSubscriptionManagement() {
        repository.openSubscriptionManagementScreen()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Check for subscription sync mismatch between API and Google Play
     * Returns true if there's a mismatch that needs recovery
     */
    fun checkSubscriptionSyncMismatch(): Boolean {
        // Get Google Play subscription status
        val (hasGooglePlaySubscription, isAutoRenewing) = repository.getGooglePlaySubscriptionStatus()

        Log.d(
            TAG, "Subscription sync check - API hasValid: ${_hasValidSubscription.value}, " +
                    "GooglePlay hasActive: $hasGooglePlaySubscription, isRenewing: $isAutoRenewing"
        )

        // Check for mismatch: No valid subscription from API but active subscription on Google Play
        val hasMismatch = !_hasValidSubscription.value && hasGooglePlaySubscription

        if (hasMismatch) {
            Log.w(TAG, "SUBSCRIPTION SYNC MISMATCH DETECTED!")
            Log.w(TAG, "API shows no valid subscription, but Google Play shows active subscription")
            Log.w(TAG, "This indicates a sync issue that needs recovery")
        }

        return hasMismatch
    }

    /**
     * Trigger subscription recovery flow
     */
    fun triggerSubscriptionRecovery() {
        billingManagerWrapper.getBillingManager()?.triggerSubscriptionRecovery()
    }
} 