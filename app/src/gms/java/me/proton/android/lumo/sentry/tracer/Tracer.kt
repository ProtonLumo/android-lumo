package me.proton.android.lumo.sentry.tracer

import io.sentry.ISpan
import io.sentry.ITransaction
import io.sentry.Sentry
import io.sentry.SpanStatus
import me.proton.android.lumo.tracer.LumoTracer
import me.proton.android.lumo.tracer.LumoTracer.Operation

class Tracer(private val transactionOp: Operation) : LumoTracer {

    private var transaction: ITransaction? = null
    private val spans = mutableMapOf<Operation, ISpan>()

    override fun startTransaction(name: String) {
        transaction = Sentry.startTransaction(name, transactionOp.name)
    }

    override fun measureSpan(operation: Operation, description: String) {
        val span = transaction?.startChild(operation.name, description)
        span?.let {
            spans[operation] = it
        }
    }

    override fun stopSpan(operation: Operation) {
        spans[operation]?.finish()
    }

    override fun finishTransaction() {
        transaction?.status = SpanStatus.OK
        transaction?.finish()
    }
}