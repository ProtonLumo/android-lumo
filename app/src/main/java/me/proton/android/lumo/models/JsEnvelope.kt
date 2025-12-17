package me.proton.android.lumo.models

import kotlinx.serialization.Serializable

@Serializable
data class JsEnvelope<T>(
    val status: String,
    val data: T? = null,
    val message: String? = null
)