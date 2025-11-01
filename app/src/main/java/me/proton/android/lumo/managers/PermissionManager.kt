package me.proton.android.lumo.managers

import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

private const val TAG = "PermissionManager"

/**
 * Manager class that handles all permission-related operations including audio permissions
 * and file chooser functionality. Separates permission concerns from MainActivity.
 */
class PermissionManager(
    private val activity: ComponentActivity,
    private val webViewManager: WebViewManager? = null
) {

    val fileChooserLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "File chooser result received with code: ${result.resultCode}")
        webViewManager?.handleFileChooserResult(result.resultCode, result.data)
    }

    /**
     * Launch file chooser for WebView file input
     */
    fun launchFileChooser(intent: Intent) {
        Log.d(TAG, "Launching file chooser")
        fileChooserLauncher.launch(intent)
    }

    /**
     * Create file chooser intent for various file types
     */
    fun createFileChooserIntent(
        acceptTypes: Array<String>? = null,
        multiple: Boolean = false
    ): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)

            // Handle accept types
            acceptTypes?.let { types ->
                if (types.isNotEmpty()) {
                    if (types.size == 1) {
                        type = types[0]
                    } else {
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, types)
                    }
                }
            }

            // Handle multiple selection
            if (multiple) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }

        Log.d(
            TAG,
            "Created file chooser intent with types: ${acceptTypes?.contentToString()}, multiple: $multiple"
        )
        return intent
    }
}
