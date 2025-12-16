package me.proton.android.lumo.review

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface ReviewRepository {
    suspend fun markSeen()
    suspend fun hasSeen(): Boolean
    suspend fun observeSeen(): Flow<Boolean>

}

class ReviewRepositoryImpl @Inject constructor(
    private val prefs: SharedPreferences,
) : ReviewRepository {

    private val _seenFlow = MutableStateFlow(prefs.getBoolean(KEY_SEEN, false))

    override suspend fun markSeen() {
        withContext(Dispatchers.IO) {
            prefs.edit { putBoolean(KEY_SEEN, true) }
        }
        _seenFlow.value = true
    }

    override suspend fun hasSeen(): Boolean = _seenFlow.value

    override suspend fun observeSeen(): Flow<Boolean> = _seenFlow

    companion object {
        private const val KEY_SEEN = "key::lumo::review::has_seen"
    }
}