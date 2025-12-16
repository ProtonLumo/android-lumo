package me.proton.android.lumo.featureflag.mapper

import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.Json
import me.proton.android.lumo.models.JsEnvelope

inline fun <reified T> CompletableDeferred<Result<T>>.mapResponse(
    json: Json,
    resultJson: String
) {
    runCatching {
        json.decodeFromString<JsEnvelope<T>>(resultJson)
    }.onSuccess { envelope ->
        when (envelope.status) {
            "success" -> {
                val data = envelope.data
                    ?: error("Success response missing data")
                complete(Result.success(data))
            }
            "error" -> {
                complete(
                    Result.failure(
                        IllegalStateException(envelope.message ?: "Unknown JS error")
                    )
                )
            }
            else -> {
                complete(
                    Result.failure(
                        IllegalStateException("Unknown status: ${envelope.status}")
                    )
                )
            }
        }
    }.onFailure { error ->
        complete(Result.failure(error))
    }

}