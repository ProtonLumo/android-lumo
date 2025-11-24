package me.proton.android.lumo.data.repository

import kotlinx.coroutines.flow.Flow
import me.proton.android.lumo.webview.WebAppInterface
import javax.inject.Inject
import me.proton.android.lumo.MainActivityViewModel.WebEvent as MainWebEvent

interface WebAppRepository {
    suspend fun listenToWebEvent(): Flow<MainWebEvent>

}

class WebAppRepositoryImpl @Inject constructor(
    private val webBridge: WebAppInterface,
) : WebAppRepository {
    override suspend fun listenToWebEvent(): Flow<MainWebEvent> = webBridge.mainEventChannel
}