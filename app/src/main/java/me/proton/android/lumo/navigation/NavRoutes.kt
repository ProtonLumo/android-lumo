package me.proton.android.lumo.navigation

import kotlinx.serialization.Serializable

sealed interface NavRoutes {
    @Serializable
    data object Main : NavRoutes

    @Serializable
    data object Subscription : NavRoutes
}
