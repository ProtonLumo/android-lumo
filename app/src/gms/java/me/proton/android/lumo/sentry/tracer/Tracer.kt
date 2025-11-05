package me.proton.android.lumo.sentry.tracer

import io.sentry.ISpan
import io.sentry.ITransaction
import io.sentry.Sentry
import io.sentry.SpanStatus

class Tracer(private val transactionOp: Operation) {

    private var transaction: ITransaction? = null
    private val spans = mutableMapOf<Operation, ISpan>()

    fun startTransaction(name: String) {
        transaction = Sentry.startTransaction(name, transactionOp.name)
    }

    fun measureSpan(operation: Operation, description: String = "") {
        val span = transaction?.startChild(operation.name, description)
        span?.let {
            spans[operation] = it
        }
    }

    fun stopSpan(operation: Operation) {
        spans[operation]?.finish()
    }

    fun finishTransaction() {
        transaction?.status = SpanStatus.OK
        transaction?.finish()
    }

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