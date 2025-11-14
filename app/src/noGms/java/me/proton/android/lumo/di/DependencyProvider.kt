package me.proton.android.lumo.di

import android.annotation.SuppressLint
import android.app.Application
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import me.proton.android.lumo.tracer.LumoTracer
import me.proton.android.lumo.usecase.HasOfferUseCase
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

    override fun isPaymentAvailable(): Boolean = false

    override fun hasOfferUseCase(): HasOfferUseCase =
        object : HasOfferUseCase {
            override fun hasOffer(): Flow<Boolean> = flowOf(true)
        }

    override fun getMainScreenTracer(): LumoTracer =
        object: LumoTracer {
            override fun startTransaction(name: String) {
            }

            override fun measureSpan(
                operation: LumoTracer.Operation,
                description: String
            ) {
            }

            override fun stopSpan(operation: LumoTracer.Operation) {
            }

            override fun finishTransaction() {
            }
        }
}