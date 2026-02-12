package me.proton.android.lumo.webview

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.annotation.WorkerThread
import timber.log.Timber
import java.io.IOException

private val fontExtensions = setOf(
    "woff",
    "woff2",
    "ttf",
    "otf",
    "eot",
)

fun String.fontUrl(): String? {
    val extension = this.split(".").lastOrNull()
    return if (extension != null && fontExtensions.contains(extension)) {
        this
    } else {
        null
    }
}

@WorkerThread
fun WebResourceRequest?.loadFontAndType(
    context: Context,
    defaultResponse: () -> WebResourceResponse?,
): WebResourceResponse? =
    this?.url
        .toString()
        .fontUrl()
        ?.split("/")
        ?.lastOrNull()
        ?.split(".")
        ?.let { fontParts ->
            try {
                context.assets.open(
                    "fonts/${fontParts.first()}.${fontParts.last()}"
                ) to fontParts.last()
            } catch (e: IOException) {
                Timber.tag("FontLoading").e(e, "Unable to read file:")
                null to fontParts.last()
            } catch (e: NoSuchElementException) {
                Timber.tag("FontLoading").e(e, "Element not found, parsing error:")
                null to fontParts.last()
            }
        }
        ?.let { inputToExtension ->
            val input = inputToExtension.first
            if (input == null) {
                defaultResponse()
            } else {
                WebResourceResponse(
                    "font/${inputToExtension.second.last()}",
                    null,
                    inputToExtension.first
                )
            }
        }
