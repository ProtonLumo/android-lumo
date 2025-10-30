package me.proton.android.lumo.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import me.proton.android.lumo.data.repository.ThemeRepository
import me.proton.android.lumo.data.repository.ThemeRepositoryImpl
import me.proton.android.lumo.data.repository.WebAppRepository
import me.proton.android.lumo.data.repository.WebAppRepositoryImpl
import me.proton.android.lumo.usecase.HasOfferUseCase

abstract class BaseDependencyProvider {

    lateinit var application: Application

    private var prefs: SharedPreferences? = null
    private var themeRepository: ThemeRepository? = null
    private var webAppRepository: WebAppRepository? = null

    open fun initialise(application: Application) {
        this.application = application
    }

    abstract fun isPaymentAvailable(): Boolean

    abstract fun hasOfferUseCase(): HasOfferUseCase

    fun webAppRepository(): WebAppRepository =
        webAppRepository ?: WebAppRepositoryImpl(
            webBridge = DependencyProvider.getWebBridge()
        ).also { webAppRepository = it }

    fun themeRepository(): ThemeRepository =
        themeRepository ?: ThemeRepositoryImpl(getPrefs()).also { themeRepository = it }

    private fun getPrefs(): SharedPreferences =
        prefs ?: application.getSharedPreferences(
            "lumo_prefs",
            Context.MODE_PRIVATE
        )
}