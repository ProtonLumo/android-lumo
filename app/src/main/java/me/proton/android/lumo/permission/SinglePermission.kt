package me.proton.android.lumo.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

class SinglePermission(
    private val permission: String,
    private val context: Context,
    private val launcher: ActivityResultLauncher<String>,
    private val onSuccess: () -> Unit = {},
    private val onDeny: (String) -> Unit = {}
) : PermissionContract {
    override val isGranted: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

    override fun request() {
        launcher.launch(permission)
    }

    fun copy(
        onSuccess: () -> Unit = this.onSuccess,
        onDeny: (String) -> Unit = this.onDeny
    ) = SinglePermission(permission, context, launcher, onSuccess, onDeny)
}