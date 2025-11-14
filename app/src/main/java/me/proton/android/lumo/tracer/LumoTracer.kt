package me.proton.android.lumo.tracer

interface LumoTracer {

    fun startTransaction(name: String)

    fun measureSpan(operation: Operation, description: String = "")

    fun stopSpan(operation: Operation)

    fun finishTransaction()

    sealed interface Operation {
        val name: String

        data object LoadUi : Operation {
            override val name: String
                get() = "load.ui"
        }

        data object MainReady : Operation {
            override val name: String
                get() = "main.ready"
        }
    }

}