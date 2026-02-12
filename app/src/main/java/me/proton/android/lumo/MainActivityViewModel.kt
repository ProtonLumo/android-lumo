package me.proton.android.lumo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.android.lumo.analytics.LumoAnalytics
import me.proton.android.lumo.config.LumoConfig
import me.proton.android.lumo.data.repository.ThemeRepository
import me.proton.android.lumo.data.repository.WebAppRepository
import me.proton.android.lumo.featureflag.FeatureGatekeeper
import me.proton.android.lumo.permission.PermissionContract
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.ui.theme.AppStyle
import me.proton.android.lumo.utils.isHostReachable
import timber.log.Timber
import javax.inject.Inject

private const val TAG = "MainActivityViewModel"
private const val HTTPS_PORT = 443
private const val NETWORK_CHECK_TIMEOUT_MS = 3000 // 3 seconds
private const val LOADING_TIMEOUT_MS = 5000 // 5 seconds

// Define UI State (can be expanded later)
data class MainUiState(
    val isLoading: Boolean = true,
    val initialLoadError: String? = null,
    val isLumoPage: Boolean = true,
    val hasSeenLumoContainer: Boolean = false,
    val shouldShowBackButton: Boolean = false,
    val theme: AppStyle? = null,
)

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
    private val webAppRepository: WebAppRepository,
    private val analytics: LumoAnalytics,
    private val featureGatekeeper: FeatureGatekeeper,
) : ViewModel() {

    sealed class UiEvent {
        data class EvaluateJavascript(val script: String) : UiEvent()
        data class ShowToast(val message: UiText) : UiEvent()
        class ShowPaymentDialog(val paymentEvent: PaymentEvent = PaymentEvent.Default) : UiEvent()
        object ShowSpeechSheet : UiEvent()
        data class MissingPermission(val missingPermission: String) : UiEvent()
    }

    enum class PaymentEvent {
        Default, BlackFriday
    }

    sealed interface WebEvent {
        data object ShowPaymentRequested : WebEvent
        data object ShowBlackFridaySale : WebEvent
        data object StartVoiceEntryRequested : WebEvent
        data object RetryLoadRequested : WebEvent
        data class PageTypeChanged(val isLumo: Boolean, val url: String) : WebEvent
        data class Navigated(val url: String, val type: String) : WebEvent
        data object LumoContainerVisible : WebEvent
        data class ThemeResult(val mode: String) : WebEvent {
            val theme = when (mode) {
                "lumo-dark-theme",
                "Dark" -> 1

                "lumo-light-theme",
                "Light" -> 2

                else -> 0
            }
        }
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _eventChannel = Channel<UiEvent>()
    val events = _eventChannel.receiveAsFlow()

    // State for initial URL after network check
    private val _initialUrl =
        MutableStateFlow(LumoConfig.LUMO_URL) // Start with default URL
    val initialUrl: StateFlow<String> = _initialUrl.asStateFlow()
    private var checkCompleted = false // Prevent re-checking on config change
    private var audioPermissionContract: PermissionContract? = null

    init {
        // Don't call performInitialNetworkCheck here, call from Activity onCreate
        viewModelScope.launch {
            webAppRepository.listenToWebEvent().collect { event ->
                when (event) {
                    // UI state toggle; Activity will render from state in a later step
                    is WebEvent.ShowPaymentRequested -> {
                        _eventChannel.trySend(UiEvent.ShowPaymentDialog())
                    }

                    is WebEvent.StartVoiceEntryRequested -> {
                        startVoiceEntry()
                    }

                    is WebEvent.RetryLoadRequested -> {
                        Timber.tag(TAG).i("Resetting checkCompleted flag for retry.")
                        checkCompleted = false
                        performInitialNetworkCheck()
                    }

                    is WebEvent.PageTypeChanged -> {
                        _uiState.update { state ->
                            val newIsLumo = event.isLumo
                            val showBack = LumoConfig.isAccountDomain(event.url)
                            state.copy(
                                isLumoPage = newIsLumo,
                                shouldShowBackButton = showBack,
                            )
                        }
                    }

                    is WebEvent.Navigated -> {
                        _uiState.update { state ->
                            val showBack = LumoConfig.isAccountDomain(event.url)
                            state.copy(
                                shouldShowBackButton = showBack
                            )
                        }
                    }

                    is WebEvent.LumoContainerVisible -> {
                        _uiState.update {
                            if (it.hasSeenLumoContainer && !it.isLoading) {
                                it
                            } else {
                                analytics.finish()
                                featureGatekeeper.start()
                                it.copy(
                                    isLoading = false,
                                    hasSeenLumoContainer = true
                                )
                            }
                        }
                    }

                    is WebEvent.ThemeResult -> {
                        if (event.theme != _uiState.value.theme?.mode) {
                            val appStyle = AppStyle.fromInt(event.theme)
                            viewModelScope.launch {
                                themeRepository.saveTheme(appStyle)
                            }
                        }
                    }

                    is WebEvent.ShowBlackFridaySale -> {
                        _eventChannel.trySend(
                            UiEvent.ShowPaymentDialog(
                                paymentEvent = PaymentEvent.BlackFriday
                            )
                        )
                    }
                }
            }
        }
    }

    fun setupThemeUpdates(isSystemInDarkMode: Boolean) {
        viewModelScope.launch {
            themeRepository.observeTheme(isSystemInDarkMode).collect { theme ->
                _uiState.update { state ->
                    state.copy(theme = theme)
                }
            }
        }
    }

    fun startVoiceEntry() {
        if (audioPermissionContract?.isGranted == true) {
            _eventChannel.trySend(UiEvent.ShowSpeechSheet)
        } else {
            audioPermissionContract?.request()
        }
    }

    fun performInitialNetworkCheck() {
        if (checkCompleted) {
            Timber.tag(TAG).i("Initial network check already completed, skipping.")
            return
        }
        _uiState.update { it.copy(isLoading = true, initialLoadError = null) } // Show loading

        viewModelScope.launch {
            val host = LumoConfig.LUMO_DOMAIN
            val port = HTTPS_PORT
            val timeout = NETWORK_CHECK_TIMEOUT_MS
            Timber.tag(TAG).i("Performing initial network check for $host:$port...")

            val reachable = isHostReachable(host, port, timeout) // Call the suspend function

            if (reachable) {
                Timber.tag(TAG).i("Initial network check: Host $host is reachable.")
                _initialUrl.value = LumoConfig.LUMO_URL
                _uiState.update {
                    it.copy(
                        initialLoadError = null,
                        isLoading = false
                    )
                }
            } else {
                Timber.tag(TAG)
                    .i("Initial network check: Host $host is NOT reachable within $timeout ms.")
                _initialUrl.value = "file:///android_asset/network_error.html"
                _uiState.update {
                    it.copy(
                        initialLoadError = "Host not reachable",
                        isLoading = false
                    )
                } // Set error state
            }
            checkCompleted = true
            Timber.tag(TAG)
                .i("Initial network check finished.Initial URL set to : $ { _initialUrl.value }")
        }
        forceHideLoadingAfterDelay()
    }

    private fun forceHideLoadingAfterDelay() {
        viewModelScope.launch {
            delay(LOADING_TIMEOUT_MS.toLong())
            val currentState = _uiState.value
            if (currentState.isLoading) {
                Timber.tag(TAG).i("Forcing loading screen to hide from global timer")
                hideLoading()
            }
        }
    }

    fun handleJavascriptResult(result: String?) {
        Timber.tag(TAG).i("JavaScript execution result: $result")
        if (result == null || result == "null" || result.contains("Error")) {
            Timber.tag(TAG).e("JavaScript execution failed or function not found.Result: $result")
            viewModelScope.launch {
                _eventChannel.send(
                    UiEvent.ShowToast(
                        UiText.ResText(R.string.submit_prompt_failed)
                    )
                )
            }
        } else {
            Timber.tag(TAG).i("JavaScript insertPromptAndSubmit executed successfully.")
        }
    }

    fun showLoading() {
        if (!_uiState.value.isLoading) {
            _uiState.update {
                it.copy(isLoading = true, hasSeenLumoContainer = false)
            }
        }
    }

    fun hideLoading(hasSeenLumoContainer: Boolean = true) {
        if (hasSeenLumoContainer) {
            _uiState.update {
                it.copy(isLoading = false, hasSeenLumoContainer = true)
            }
        } else {
            _uiState.update {
                it.copy(isLoading = false)
            }
        }
    }

    fun attachPermissionContract(permissionContract: PermissionContract) {
        audioPermissionContract = permissionContract
    }

    fun detachPermissionContract() {
        audioPermissionContract = null
    }

    fun showMissingPermission(missingPermission: String) {
        _eventChannel.trySend(UiEvent.MissingPermission(missingPermission))
    }

    fun startAnalytics() {
        analytics.start()
    }

    fun cancelAnalytics() {
        analytics.cancel()
    }
}
