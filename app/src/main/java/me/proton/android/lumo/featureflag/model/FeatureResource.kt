/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.lumo.featureflag.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeatureResource(
    @SerialName("Code")
    val featureId: String,
    @SerialName("Global")
    val isGlobal: Boolean,
    @SerialName("DefaultValue")
    val defaultValue: Boolean,
    @SerialName("Value")
    val value: Boolean
) {
    internal fun toFeatureFlag() = FeatureFlag(
        featureId = FeatureId(featureId),
        scope = if (isGlobal) Scope.Global else Scope.User,
        defaultValue = defaultValue,
        value = value,
        variantName = null,
        payloadType = null,
        payloadValue = null,
    )
}

data class FeatureId(val id: String)
data class FeatureFlag(
    val featureId: FeatureId,
    val scope: Scope,
    val defaultValue: Boolean = false,
    val value: Boolean = false,
    val variantName: String? = null,
    val payloadType: String? = null,
    val payloadValue: String? = null,
) {
    companion object {
        val DEFAULT = default(featureId = "disabled", defaultValue = false)
        fun default(featureId: String, defaultValue: Boolean): FeatureFlag = FeatureFlag(
            featureId = FeatureId(featureId),
            scope = Scope.Unknown,
            defaultValue = defaultValue,
            value = defaultValue,
            variantName = null,
            payloadType = null,
            payloadValue = null,
        )
    }
}

enum class Scope(val value: Int) {
    /* Requested but unknown. */
    Unknown(0),

    /* For this device. */
    Local(1),

    /* For this User. */
    User(2),

    /* For all Users. */
    Global(3),

    /* Source: Unleash */
    Unleash(4),
}

