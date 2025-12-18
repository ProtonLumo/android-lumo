package me.proton.android.lumo.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.proton.android.lumo.BuildConfig
import me.proton.android.lumo.data.repository.SubscriptionRepository
import me.proton.android.lumo.money_machine.BillingStore
import javax.inject.Inject

class HasOffer @Inject constructor(
    private val billingStore: BillingStore
) : HasOfferUseCase {

    override fun hasOffer(): Flow<Boolean> = flow { emit(false) }
//        billingStore.state.map { state ->
//                state.products.any { product ->
//                    product.subscriptionOfferDetails?.any {
//                        it.offerId == BuildConfig.OFFER_ID
//                    } == true
//                }
//            }
//            .distinctUntilChanged()
}
