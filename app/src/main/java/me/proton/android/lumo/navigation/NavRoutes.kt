package me.proton.android.lumo.navigation

import kotlinx.serialization.Serializable

sealed interface NavRoutes {
    @Serializable
    data object Chat : NavRoutes

    @Serializable
    data object Subscription : NavRoutes
}
