package me.proton.android.lumo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.android.lumo.config.LumoConfig
import me.proton.android.lumo.data.repository.ThemeRepository
import me.proton.android.lumo.data.repository.WebAppRepository
import me.proton.android.lumo.permission.PermissionContract
import me.proton.android.lumo.ui.text.UiText
import me.proton.android.lumo.ui.theme.AppStyle
import me.proton.android.lumo.usecase.HasOfferUseCase
import me.proton.android.lumo.utils.isHostReachable
import me.proton.android.lumo.webview.hideBfButton
import me.proton.android.lumo.webview.keyboardHeightChange

private const val TAG = "MainActivityViewModel"

// Define UI State (can be expanded later)
data class MainUiState(
    val isLoading: Boolean = true,
    val initialLoadError: String? = null,
    val isLumoPage: Boolean = true,
    val hasSeenLumoContainer: Boolean = false,
    val shouldShowBackButton: Boolean = false,
    val theme: AppStyle? = null,
)

class MainActivityViewModel(
    private val themeRepository: ThemeRepository,
    private val webAppRepository: WebAppRepository,
    private val hasOfferUseCase: HasOfferUseCase,
) : ViewModel() {

    sealed class UiEvent {
        data class EvaluateJavascript(val script: String) : UiEvent()
        data class ShowToast(val message: UiText) : UiEvent()
        class ShowPaymentDialog(val paymentEvent: PaymentEvent = PaymentEvent.Default) : UiEvent()
        object ShowSpeechSheet : UiEvent()
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
                "Dark" -> 1
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
        viewModelScope.launch {
            combine(
                hasOfferUseCase.hasOffer(),
                _uiState.map { it.hasSeenLumoContainer }.distinctUntilChanged()
            ) { hasOffer, hasSeenLumoContainer ->
                !hasOffer && hasSeenLumoContainer
            }.distinctUntilChanged()
                .collect { shouldHide ->
                    if (shouldHide) {
                        _eventChannel.send(
                            UiEvent.EvaluateJavascript(
                                hideBfButton()
                            )
                        )
                    }
                }
        }
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
                        resetNetworkCheckFlag()
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
                            it.copy(
                                isLoading = false,
                                hasSeenLumoContainer = true
                            )
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

    // --- Initial Network Check ---
    fun performInitialNetworkCheck() {
        if (checkCompleted) {
            Log.d(TAG, "Initial network check already completed, skipping.")
            return
        }
        _uiState.update { it.copy(isLoading = true, initialLoadError = null) } // Show loading

        // Add safety timeout to ensure loading state is cleared even if network check takes too long
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // 5 second timeout
            if (_uiState.value.isLoading) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        viewModelScope.launch {
            val host = LumoConfig.LUMO_DOMAIN
            val port = 443
            val timeout = 3000 // 3 seconds
            Log.d(TAG, "Performing initial network check for $host:$port...")

            val reachable = isHostReachable(host, port, timeout) // Call the suspend function

            if (reachable) {
                Log.d(TAG, "Initial network check: Host $host is reachable.")
                _initialUrl.value = LumoConfig.LUMO_URL
                _uiState.update { it.copy(initialLoadError = null) }
            } else {
                Log.w(TAG, "Initial network check: Host $host is NOT reachable within $timeout ms.")
                _initialUrl.value = "file:///android_asset/network_error.html"
                _uiState.update { it.copy(initialLoadError = "Host not reachable") } // Set error state
            }
            _uiState.update { it.copy(isLoading = false) } // Hide loading
            checkCompleted = true
            Log.d(TAG, "Initial network check finished. Initial URL set to: ${_initialUrl.value}")
        }
    }

    fun resetNetworkCheckFlag() {
        Log.d(TAG, "Resetting checkCompleted flag for retry.")
        checkCompleted = false
    }

    fun handleJavascriptResult(result: String?) {
        Log.d(TAG, "JavaScript execution result: $result")
        if (result == null || result == "null" || result.contains("Error")) {
            Log.e(TAG, "JavaScript execution failed or function not found. Result: $result")
            viewModelScope.launch {
                _eventChannel.send(
                    UiEvent.ShowToast(
                        UiText.ResText(R.string.submit_prompt_failed)
                    )
                )
            }
        } else {
            Log.d(TAG, "JavaScript insertPromptAndSubmit executed successfully.")
        }
    }

    fun onKeyboardVisibilityChanged(
        isVisible: Boolean,
        keyboardHeightPx: Int,
    ) {
        _eventChannel.trySend(
            UiEvent.EvaluateJavascript(
                keyboardHeightChange(
                    isVisible, keyboardHeightPx
                )
            )
        )
    }

    fun showLoading() {
        _uiState.update {
            it.copy(isLoading = true, hasSeenLumoContainer = false)
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
}
