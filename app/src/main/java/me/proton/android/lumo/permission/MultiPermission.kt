package me.proton.android.lumo.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

class MultiPermission(
    private val permissions: Array<String>,
    private val context: Context,
    private val launcher: ActivityResultLauncher<Array<String>>,
    private val onSuccess: () -> Unit = {},
    private val onDeny: (List<String>) -> Unit = {}
) : PermissionContract {
    override val isGranted: Boolean
        get() = permissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }

    override fun request() {
        launcher.launch(permissions)
    }

    fun copy(
        onSuccess: () -> Unit = this.onSuccess,
        onDeny: (List<String>) -> Unit = this.onDeny
    ) = MultiPermission(permissions, context, launcher, onSuccess, onDeny)
}