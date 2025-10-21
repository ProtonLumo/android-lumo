package me.proton.android.lumo.di

import android.annotation.SuppressLint
import android.app.Application
import me.proton.android.lumo.webview.WebAppInterface

object DependencyProvider : BaseDependencyProvider() {

    @SuppressLint("StaticFieldLeak")
    private var webBridge: WebAppInterface? = null

    override fun initialise(application: Application) {
        super.initialise(application)
        webBridge = WebAppInterface()
    }

    fun getWebBridge(): WebAppInterface =
        webBridge ?: WebAppInterface().also { webBridge = it }

    override fun isPaymentAvailable(): Boolean  = false
}