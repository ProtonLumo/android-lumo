package me.proton.android.lumo.di

import android.app.Application

abstract class BaseDependencyProvider {

    lateinit var application: Application

    open fun initialise(application: Application) {
        this.application = application
    }

    abstract fun isPaymentAvailable(): Boolean
}