package me.proton.android.lumo.permission

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat

@Composable
fun rememberSinglePermission(
    permission: String,
    onGranted: () -> Unit = {},
    onDenied: (String) -> Unit = {},
): SinglePermission {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onGranted() else onDenied(permission)
    }
    return remember(permission) {
        SinglePermission(permission, context, launcher, onGranted, onDenied)
    }
}

@Composable
fun rememberMultiPermission(
    permissions: Array<String>,
    onGranted: () -> Unit,
    onDenied: (List<String>) -> Unit
): MultiPermission {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultMap: Map<String, Boolean> ->
        val denied = resultMap.filterValues { granted -> !granted }.keys.toList()
        if (denied.isEmpty()) onGranted() else onDenied(denied)
    }
    return remember(permissions) {
        MultiPermission(permissions, context, launcher, onGranted, onDenied)
    }
}

fun Activity.shouldShowPermissionRationale(permission: String, onShowRationale: (String) -> Unit) {
    ActivityCompat.shouldShowRequestPermissionRationale(this, permission).let {
        if (it) {
            onShowRationale(permission)
        }
    }
}
