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
    onGrant: () -> Unit = {},
    onDeny: (String) -> Unit = {},
): SinglePermission {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onGrant() else onDeny(permission)
    }
    return remember(permission) {
        SinglePermission(permission, context, launcher, onGrant, onDeny)
    }
}

@Composable
fun rememberMultiPermission(
    permissions: Array<String>,
    onGrant: () -> Unit,
    onDeny: (List<String>) -> Unit
): MultiPermission {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultMap: Map<String, Boolean> ->
        val denied = resultMap.filterValues { granted -> !granted }.keys.toList()
        if (denied.isEmpty()) onGrant() else onDeny(denied)
    }
    return remember(permissions) {
        MultiPermission(permissions, context, launcher, onGrant, onDeny)
    }
}

fun Activity.shouldShowPermissionRationale(permission: String, onShowRationale: (String) -> Unit) {
    ActivityCompat.shouldShowRequestPermissionRationale(this, permission).let {
        if (it) {
            onShowRationale(permission)
        }
    }
}
