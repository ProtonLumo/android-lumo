package me.proton.android.lumo.utils

import me.proton.android.lumo.R
import me.proton.android.lumo.ui.text.UiText
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.PortUnreachableException
import java.net.ProtocolException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.UnknownServiceException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLProtocolException
import java.security.cert.CertificateException

/**
 * Professional error classification system for network and other errors.
 * This replaces unreliable string matching with proper type-based classification.
 */
object ErrorClassifier {

    /**
     * Sealed class representing different types of errors
     */
    sealed class ErrorType {
        object Network : ErrorType()
        object Timeout : ErrorType()
        object Authentication : ErrorType()
        object SSL : ErrorType()
        object Server : ErrorType()
        object Client : ErrorType()
        object Unknown : ErrorType()
    }

    /**
     * Data class containing detailed error information
     */
    data class ErrorInfo(
        val type: ErrorType,
        val isRetryable: Boolean,
        val userMessageResId: Int,
        val technicalDetails: String? = null
    ) {
        /**
         * Get the localized user message
         */
        fun getUserMessage(): UiText {
            return UiText.ResText(userMessageResId)
        }
    }

    /**
     * Classifies throwables into proper error categories
     */
    fun classify(throwable: Throwable): ErrorInfo {
        return when (throwable) {
            // Network connectivity errors
            is UnknownHostException -> ErrorInfo(
                type = ErrorType.Network,
                isRetryable = true,
                userMessageResId = R.string.error_network_dns,
                technicalDetails = "DNS resolution failed: ${throwable.message}"
            )

            is ConnectException -> ErrorInfo(
                type = ErrorType.Network,
                isRetryable = true,
                userMessageResId = R.string.error_network_connection,
                technicalDetails = "Connection refused: ${throwable.message}"
            )

            is NoRouteToHostException -> ErrorInfo(
                type = ErrorType.Network,
                isRetryable = true,
                userMessageResId = R.string.error_network_no_route,
                technicalDetails = "No route to host: ${throwable.message}"
            )

            is PortUnreachableException -> ErrorInfo(
                type = ErrorType.Network,
                isRetryable = false,
                userMessageResId = R.string.error_network_port_unreachable,
                technicalDetails = "Port unreachable: ${throwable.message}"
            )

            // Timeout errors
            is SocketTimeoutException -> ErrorInfo(
                type = ErrorType.Timeout,
                isRetryable = true,
                userMessageResId = R.string.error_timeout_socket,
                technicalDetails = "Socket timeout: ${throwable.message}"
            )

            // SSL/TLS errors
            is SSLHandshakeException -> ErrorInfo(
                type = ErrorType.SSL,
                isRetryable = false,
                userMessageResId = R.string.error_ssl_handshake,
                technicalDetails = "SSL handshake failed: ${throwable.message}"
            )

            is SSLPeerUnverifiedException -> ErrorInfo(
                type = ErrorType.SSL,
                isRetryable = false,
                userMessageResId = R.string.error_ssl_peer_unverified,
                technicalDetails = "SSL peer unverified: ${throwable.message}"
            )

            is SSLProtocolException -> ErrorInfo(
                type = ErrorType.SSL,
                isRetryable = false,
                userMessageResId = R.string.error_ssl_protocol,
                technicalDetails = "SSL protocol error: ${throwable.message}"
            )

            is SSLException -> ErrorInfo(
                type = ErrorType.SSL,
                isRetryable = false,
                userMessageResId = R.string.error_ssl_general,
                technicalDetails = "SSL error: ${throwable.message}"
            )

            is CertificateException -> ErrorInfo(
                type = ErrorType.SSL,
                isRetryable = false,
                userMessageResId = R.string.error_certificate,
                technicalDetails = "Certificate error: ${throwable.message}"
            )

            // Protocol errors
            is ProtocolException -> ErrorInfo(
                type = ErrorType.Client,
                isRetryable = false,
                userMessageResId = R.string.error_client_protocol,
                technicalDetails = "Protocol error: ${throwable.message}"
            )

            is UnknownServiceException -> ErrorInfo(
                type = ErrorType.Client,
                isRetryable = false,
                userMessageResId = R.string.error_client_service_unknown,
                technicalDetails = "Unknown service: ${throwable.message}"
            )

            // Socket errors (general network issues)
            is SocketException -> ErrorInfo(
                type = ErrorType.Network,
                isRetryable = true,
                userMessageResId = R.string.error_network_socket,
                technicalDetails = "Socket error: ${throwable.message}"
            )

            // General IO errors
            is IOException -> ErrorInfo(
                type = ErrorType.Network,
                isRetryable = true,
                userMessageResId = R.string.error_network_io,
                technicalDetails = "IO error: ${throwable.message}"
            )

            // HTTP-specific errors (if using OkHttp or similar)
            else -> classifyHttpError(throwable)
        }
    }

    /**
     * Classifies HTTP-specific errors (extend this based on your HTTP client)
     */
    private fun classifyHttpError(throwable: Throwable): ErrorInfo {
        val message = throwable.message?.lowercase() ?: ""

        return when {
            // Check for common HTTP error patterns as fallback
            message.contains("401") || message.contains("unauthorized") -> ErrorInfo(
                type = ErrorType.Authentication,
                isRetryable = false,
                userMessageResId = R.string.error_auth_unauthorized,
                technicalDetails = throwable.message
            )

            message.contains("403") || message.contains("forbidden") -> ErrorInfo(
                type = ErrorType.Authentication,
                isRetryable = false,
                userMessageResId = R.string.error_auth_forbidden,
                technicalDetails = throwable.message
            )

            message.contains("404") || message.contains("not found") -> ErrorInfo(
                type = ErrorType.Client,
                isRetryable = false,
                userMessageResId = R.string.error_client_not_found,
                technicalDetails = throwable.message
            )

            message.contains("500") || message.contains("internal server") -> ErrorInfo(
                type = ErrorType.Server,
                isRetryable = true,
                userMessageResId = R.string.error_server_internal,
                technicalDetails = throwable.message
            )

            message.contains("502") || message.contains("bad gateway") -> ErrorInfo(
                type = ErrorType.Server,
                isRetryable = true,
                userMessageResId = R.string.error_server_bad_gateway,
                technicalDetails = throwable.message
            )

            message.contains("503") || message.contains("service unavailable") -> ErrorInfo(
                type = ErrorType.Server,
                isRetryable = true,
                userMessageResId = R.string.error_server_unavailable,
                technicalDetails = throwable.message
            )

            else -> ErrorInfo(
                type = ErrorType.Unknown,
                isRetryable = false,
                userMessageResId = R.string.error_unknown,
                technicalDetails = throwable.message
            )
        }
    }

    /**
     * Convenience method to check if error is network-related
     */
    fun isNetworkError(throwable: Throwable): Boolean {
        val errorInfo = classify(throwable)
        return errorInfo.type in setOf(
            ErrorType.Network,
            ErrorType.Timeout,
            ErrorType.SSL
        )
    }

    /**
     * Convenience method to check if error is retryable
     */
    fun isRetryable(throwable: Throwable): Boolean {
        return classify(throwable).isRetryable
    }
}
