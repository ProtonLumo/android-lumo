package me.proton.android.lumo.managers

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

private const val TAG = "PermissionManager"

/**
 * Manager class that handles all permission-related operations including audio permissions
 * and file chooser functionality. Separates permission concerns from MainActivity.
 */
class PermissionManager(
    private val activity: ComponentActivity,
    private val onPermissionResult: (String, Boolean) -> Unit,
    private val webViewManager: WebViewManager? = null
) {

    // Permission launchers
    private val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.d(TAG, "Permission result received: isGranted = $isGranted")
        onPermissionResult(Manifest.permission.RECORD_AUDIO, isGranted)
    }

    // File chooser launcher
    val fileChooserLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "File chooser result received with code: ${result.resultCode}")
        webViewManager?.handleFileChooserResult(result.resultCode, result.data)
    }

    /**
     * Check if a specific permission is granted
     */
    fun isPermissionGranted(permission: String): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Permission $permission granted: $granted")
        return granted
    }

    /**
     * Check if RECORD_AUDIO permission is granted
     */
    fun isRecordAudioPermissionGranted(): Boolean {
        return isPermissionGranted(Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Request RECORD_AUDIO permission
     */
    fun requestRecordAudioPermission() {
        Log.d(TAG, "Requesting RECORD_AUDIO permission")
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Request any permission
     */
    fun requestPermission(permission: String) {
        Log.d(TAG, "Requesting permission: $permission")
        when (permission) {
            Manifest.permission.RECORD_AUDIO -> requestRecordAudioPermission()
            else -> {
                Log.w(TAG, "Unsupported permission request: $permission")
            }
        }
    }

    /**
     * Check if we should show permission rationale
     */
    fun shouldShowPermissionRationale(permission: String): Boolean {
        return activity.shouldShowRequestPermissionRationale(permission)
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

    /**
     * Get user-friendly permission rationale message
     */
    fun getPermissionRationaleMessage(permission: String): String {
        return when (permission) {
            Manifest.permission.RECORD_AUDIO -> {
                "Microphone access is needed for voice input functionality. This allows you to speak your messages instead of typing them."
            }

            else -> {
                "This permission is required for the app to function properly."
            }
        }
    }

    /**
     * Check if all required permissions are granted
     */
    fun areAllRequiredPermissionsGranted(): Boolean {
        // For now, we only require RECORD_AUDIO for speech functionality, but it's optional
        // So we return true here. Add more permissions as needed.
        return true
    }

    /**
     * Get list of permissions that are not granted
     */
    fun getMissingPermissions(requiredPermissions: Array<String>): List<String> {
        return requiredPermissions.filter { !isPermissionGranted(it) }
    }
}
