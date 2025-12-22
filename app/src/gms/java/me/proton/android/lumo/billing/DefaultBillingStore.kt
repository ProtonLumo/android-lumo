package me.proton.android.lumo.billing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch

class DefaultBillingStore(
    scope: CoroutineScope
) : BillingStore {

    private val actions = MutableSharedFlow<BillingAction>(
        extraBufferCapacity = 64
    )

    private val _state = MutableStateFlow(BillingState())
    override val state: StateFlow<BillingState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<BillingEffect>()
    override val effects: Flow<BillingEffect> = _effects.asSharedFlow()

    override fun dispatch(action: BillingAction) {
        actions.tryEmit(action)
    }

    init {
        scope.launch {
            actions
                .scan(_state.value to emptyList<BillingEffect>()) { (state, _), action ->
                    billingReducer(state, action)
                }
                .collect { (newState, effects) ->
                    _state.value = newState
                    effects.forEach { _effects.emit(it) }
                }
        }
    }
}
