package me.proton.android.lumo.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException

/**
 * Extension functions for better error handling throughout the app
 */

/**
 * Safely execute a suspend function with proper error classification and logging
 */
suspend inline fun <T> safeApiCall(
    tag: String,
    operation: String,
    crossinline apiCall: suspend () -> T
): Result<T> {
    return try {
        Result.success(apiCall())
    } catch (cancellation: CancellationException) {
        // Re-throw cancellation to preserve coroutine cancellation
        throw cancellation
    } catch (throwable: Throwable) {
        val errorInfo = ErrorClassifier.classify(throwable)
        Log.e(
            tag,
            "$operation failed: ${errorInfo.technicalDetails ?: throwable.message}",
            throwable
        )
        Result.failure(throwable)
    }
}

/**
 * Handle API call results with proper error classification
 */
inline fun <T> Result<T>.handleError(
    tag: String,
    operation: String,
    context: Context,
    onSuccess: (T) -> Unit,
    onError: (ErrorClassifier.ErrorInfo) -> Unit
) {
    fold(
        onSuccess = onSuccess,
        onFailure = { throwable ->
            val errorInfo = ErrorClassifier.classify(throwable)
            Log.w(
                tag,
                "$operation error: ${errorInfo.getUserMessage()} (${errorInfo.technicalDetails})"
            )
            onError(errorInfo)
        }
    )
}

/**
 * Extension to check if Result failure is retryable
 */
fun <T> Result<T>.isRetryable(): Boolean {
    return fold(
        onSuccess = { false },
        onFailure = { ErrorClassifier.isRetryable(it) }
    )
}

/**
 * Extension to check if Result failure is a network error
 */
fun <T> Result<T>.isNetworkError(): Boolean {
    return fold(
        onSuccess = { false },
        onFailure = { ErrorClassifier.isNetworkError(it) }
    )
}
