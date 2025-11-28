package me.proton.android.lumo.webview

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import me.proton.android.lumo.R
import me.proton.android.lumo.ui.text.UiText

class LumoChromeClient(
    private val activity: ComponentActivity,
    private val errorHandler: (UiText) -> Unit,
) : WebChromeClient() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val fileChooserLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(
            TAG,
            "File chooser result received with code: ${result.resultCode}"
        )
        handleFileChooserResult(result.resultCode, result.data)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        showFileChooser(filePathCallback)
        return true
    }

    private fun showFileChooser(filePathCallback: ValueCallback<Array<Uri>>?) {
        this.filePathCallback = filePathCallback
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        val pm = activity.packageManager
        val resolved = pm.queryIntentActivities(intent, 0).isNotEmpty()

        if (!resolved) {
            Log.e(TAG, "No activity available to handle file chooser")
            errorHandler(UiText.ResText(R.string.error_open_file_chooser))
            return
        }

        fileChooserLauncher.launch(intent)
    }

    private fun handleFileChooserResult(
        resultCode: Int,
        data: Intent?
    ) {
        val results = if (data == null || resultCode != Activity.RESULT_OK) {
            null
        } else {
            val dataString = data.dataString
            if (dataString != null) {
                arrayOf(dataString.toUri())
            } else {
                data.clipData?.let { clipData ->
                    Array(clipData.itemCount) { i ->
                        clipData.getItemAt(i).uri
                    }
                }
            }
        }

        filePathCallback?.onReceiveValue(results ?: arrayOf())
        filePathCallback = null

        Log.d(TAG, "File chooser result handled. Results: ${results?.size ?: 0} files")
    }

    companion object {
        private const val TAG = "LumoChromeClient"
    }

}