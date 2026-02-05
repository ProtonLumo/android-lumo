package me.proton.android.lumo.analytics

import me.proton.android.lumo.tracer.LumoTracer

class MainScreenAnalytics(
    private val tracer: LumoTracer
) : LumoAnalytics {

    override fun start() {
        tracer.startTransaction(name = "MainReady")
        tracer.measureSpan(
            operation = LumoTracer.Operation.MainReady,
            description = "Measure the time it took to load the main chat screen"
        )
    }

    override fun finish() {
        tracer.stopSpan(operation = LumoTracer.Operation.MainReady)
        tracer.finishTransaction()
    }

    override fun cancel() {
        tracer.cancel()
    }
}