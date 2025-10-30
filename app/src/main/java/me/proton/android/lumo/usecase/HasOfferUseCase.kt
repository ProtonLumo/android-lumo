package me.proton.android.lumo.usecase

import kotlinx.coroutines.flow.Flow

interface HasOfferUseCase {

    fun hasOffer(): Flow<Boolean>
}