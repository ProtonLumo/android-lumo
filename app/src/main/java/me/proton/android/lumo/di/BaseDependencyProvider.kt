package me.proton.android.lumo.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import me.proton.android.lumo.data.repository.ThemeRepository
import me.proton.android.lumo.data.repository.ThemeRepositoryImpl

abstract class BaseDependencyProvider {

    lateinit var application: Application

    private var prefs: SharedPreferences? = null
    private var themeRepository: ThemeRepository? = null

    open fun initialise(application: Application) {
        this.application = application
    }

    abstract fun isPaymentAvailable(): Boolean

    fun themeRepository(): ThemeRepository =
        themeRepository ?: ThemeRepositoryImpl(getPrefs())

    private fun getPrefs(): SharedPreferences =
        prefs ?: application.getSharedPreferences(
            "lumo_prefs",
            Context.MODE_PRIVATE
        )
}