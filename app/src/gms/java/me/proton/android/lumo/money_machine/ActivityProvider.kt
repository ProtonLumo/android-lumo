package me.proton.android.lumo.money_machine

import android.app.Activity

interface ActivityProvider {
    fun currentActivity(): Activity?
    fun onActivityResumed(activity: Activity)
    fun onActivityPaused(activity: Activity)
}

class DefaultActivityProvider : ActivityProvider {

    @Volatile
    private var activity: Activity? = null

    override fun onActivityResumed(activity: Activity) {
        this.activity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        if (this.activity === activity) {
            this.activity = null
        }
    }

    override fun currentActivity(): Activity? = activity
}
