package me.proton.android.lumo.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BillingStore {
    val state: StateFlow<BillingState>
    val effects: Flow<BillingEffect>

    fun dispatch(action: BillingAction)
}
