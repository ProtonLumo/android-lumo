package me.proton.android.lumo.data.repository

import kotlinx.coroutines.flow.Flow
import me.proton.android.lumo.webview.WebAppInterface
import me.proton.android.lumo.MainActivityViewModel.WebEvent as MainWebEvent

interface WebAppRepository {
    suspend fun listenToWebEvent(): Flow<MainWebEvent>

}

class WebAppRepositoryImpl(
    private val webBridge: WebAppInterface,
) : WebAppRepository {
    override suspend fun listenToWebEvent(): Flow<MainWebEvent> = webBridge.mainEventChannel
}