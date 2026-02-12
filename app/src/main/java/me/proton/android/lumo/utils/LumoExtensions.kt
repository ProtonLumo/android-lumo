package me.proton.android.lumo.utils

fun <T> T.takeIf(predicate: () -> Boolean): T? =
    if (predicate()) {
        this
    } else {
        null
    }
