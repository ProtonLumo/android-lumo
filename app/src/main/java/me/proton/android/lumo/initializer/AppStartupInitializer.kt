package me.proton.android.lumo.initializer

import android.content.Context

interface AppStartupInitializer {
    fun initialize(context: Context)
}
