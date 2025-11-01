package me.proton.android.lumo.permission

interface PermissionContract {
    val isGranted: Boolean
    fun request()
}