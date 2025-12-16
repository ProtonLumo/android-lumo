package me.proton.android.lumo.review

import android.app.Activity

interface InAppReviewManager {
    fun start(activity: Activity)
    fun stop()
}