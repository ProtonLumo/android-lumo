package me.proton.android.lumo.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.android.lumo.BuildConfig
import me.proton.android.lumo.data.repository.SubscriptionRepository
import javax.inject.Inject

class HasOffer @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : HasOfferUseCase {

    override fun hasOffer(): Flow<Boolean> =
        subscriptionRepository.getGooglePlayProducts().map { productDetails ->
            productDetails.any { product ->
                product.subscriptionOfferDetails?.find {
                    it.offerId == BuildConfig.OFFER_ID
                } != null
            }
        }
}