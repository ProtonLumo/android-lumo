package me.proton.android.lumo.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

// --- Network Reachability Check ---
suspend fun isHostReachable(host: String, port: Int, timeoutMs: Int): Boolean {
    return withContext(Dispatchers.IO) { // Run on IO dispatcher
        try {
            Timber.tag("NetworkCheck").i(
                "Attempting to connect to $host:$port with ${timeoutMs}ms timeout"
            )
            Socket().use { socket -> // Use try-with-resources
                val socketAddress = InetSocketAddress(host, port)
                socket.connect(socketAddress, timeoutMs)
                // Connection successful
                Timber.tag("NetworkCheck").i("Connection to $host:$port successful.")
                true
            }
        } catch (e: IOException) {
            // Connection failed (timeout, host unknown, network unreachable, etc.)
            Timber.tag("NetworkCheck").i(
                "Host $host:$port not reachable within ${timeoutMs}ms: ${e.message}"
            )
            false
        } catch (e: Exception) {
            // Other potential exceptions
            Timber.tag("NetworkCheck").e(e, "Error checking reachability for $host:$port")
            false
        }
    }
}

fun Context.isNetworkStable(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            caps.linkDownstreamBandwidthKbps >= 6000
}